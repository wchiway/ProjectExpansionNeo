import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
    collectChangelog, fallbackSummary, parseProperties, releaseMetadata,
    renderReleaseBody, summarize, validateCheckout, validateModMetadata,
} from './release.mjs';

const properties = {
    minecraft_version: '1.21.1', mod_version: '1.1.0',
    projectef_version: '1.3.1', loader_version: '0.16.14',
};
const metadata = releaseMetadata(properties, '1.21.1-1.1.0');
const fallback = fallbackSummary('example/ProjectExpansionNeo', metadata, '1.21.1-1.0.6');
const english = '### Fixes\n\n- Keep exchange counts above item icons and below tooltips.\n';
const noWarnings = () => {};
const options = { apiKey: 'test-only', warn: noWarnings };
const response = (content, finish_reason = 'stop') => ({
    ok: true,
    json: async () => ({ choices: [{ message: { content }, finish_reason }] }),
});

function repository(t) {
    const cwd = mkdtempSync(join(tmpdir(), 'projectexpansion-release-test-'));
    t.after(() => rmSync(cwd, { recursive: true, force: true }));
    const git = (...args) => execFileSync('git', args, {
        cwd, encoding: 'utf8',
        env: { ...process.env, GIT_CONFIG_NOSYSTEM: '1', GIT_CONFIG_GLOBAL: '/dev/null' },
        stdio: ['ignore', 'pipe', 'pipe'],
    }).trim();
    git('init', '--initial-branch=main');
    git('config', 'user.name', 'Release tests');
    git('config', 'user.email', 'release-tests@example.invalid');
    const commit = subject => git('commit', '--allow-empty', '-m', subject);
    return { cwd, git, commit };
}

test('reads CRLF properties and rejects duplicate version keys', () => {
    assert.deepEqual(parseProperties('# comment\r\nmod_version = 1.1.0\r\nminecraft_version=1.21.1\r\n'), {
        mod_version: '1.1.0', minecraft_version: '1.21.1',
    });
    assert.throws(() => parseProperties('mod_version=1.1.0\nmod_version=1.2.0'), /Duplicate/);
});

test('accepts the canonical tag and legacy semver tags with identical artifact names', () => {
    for (const tag of ['1.21.1-1.1.0', '1.1.0', 'v1.1.0']) {
        const result = releaseMetadata(properties, tag);
        assert.equal(result.mainJar, 'ProjectExpansionNeo-1.21.1-1.1.0.jar');
        assert.equal(result.sourcesJar, 'ProjectExpansionNeo-1.21.1-1.1.0-sources.jar');
    }
});

test('rejects mismatched, prerelease, malicious, and missing tags', () => {
    for (const tag of ['1.21.1-1.0.6', '1.20.1-1.1.0', 'v1.1.0-rc1', '../main', '1.1.0\nkey=value', '$(id)', undefined]) {
        assert.throws(() => releaseMetadata(properties, tag), /Tag must match/);
    }
    assert.throws(() => releaseMetadata({ ...properties, mod_version: '01.1.0' }, '01.1.0'), /mod_version/);
    assert.throws(() => releaseMetadata({ ...properties, projectef_version: '' }, '1.1.0'), /projectef_version/);
});

test('validates annotated tags and rejects a different checked-out commit', t => {
    const { cwd, git, commit } = repository(t);
    commit('Initial release');
    git('tag', '-a', metadata.tag, '-m', 'Release');
    assert.equal(validateCheckout(metadata, cwd), git('rev-parse', 'HEAD'));
    commit('Not part of the release');
    assert.throws(() => validateCheckout(metadata, cwd), /Checkout/);
});

test('first release on the root commit includes its complete history', t => {
    const { cwd, git, commit } = repository(t);
    commit('Initial Fabric port');
    git('tag', metadata.tag);
    const changelog = collectChangelog(metadata, cwd);
    assert.equal(changelog.previousTag, '');
    assert.match(changelog.source, /Initial Fabric port/);
});

test('finds the nearest compatible release and excludes unrelated tags and older commits', t => {
    const { cwd, git, commit } = repository(t);
    commit('Old change');
    git('tag', '1.21.1-1.0.6');
    commit('Fix exchange counts');
    git('tag', '1.20.1-9.0.0');
    git('tag', 'build-checkpoint');
    commit('Add English README');
    git('tag', metadata.tag);
    const changelog = collectChangelog(metadata, cwd);
    assert.equal(changelog.previousTag, '1.21.1-1.0.6');
    assert.doesNotMatch(changelog.source, /Old change/);
    assert.match(changelog.source, /Fix exchange counts/);
    assert.match(changelog.source, /Add English README/);
});

test('fallback is English and links the relevant history instead of copying commits', () => {
    assert.match(fallback, /\/compare\/1\.21\.1-1\.0\.6\.\.\.1\.21\.1-1\.1\.0/);
    assert.match(fallbackSummary('example/mod', metadata, ''), /\/commits\/1\.21\.1-1\.1\.0/);
    assert.doesNotMatch(fallback, /\p{Script=Han}/u);
});

test('missing credentials and oversized input never send a request', async () => {
    const fetchImpl = () => assert.fail('No request should be made');
    assert.equal(await summarize('修复显示层级', fallback, { fetchImpl, warn: noWarnings }), fallback);
    assert.equal(await summarize('x'.repeat(60_001), fallback, { ...options, fetchImpl }), fallback);
});

test('requests English output, preserves full input, and accepts valid notes', async () => {
    const source = '修复显示层级\nAdd docs';
    const result = await summarize(source, fallback, {
        ...options,
        model: 'test-model',
        fetchImpl: async (url, request) => {
            assert.equal(url, 'https://api.deepseek.com/chat/completions');
            assert.equal(request.headers.Authorization, 'Bearer test-only');
            assert.equal(request.redirect, 'error');
            assert.ok(request.signal instanceof AbortSignal);
            const payload = JSON.parse(request.body);
            assert.equal(payload.model, 'test-model');
            assert.match(payload.messages[0].content, /entirely in English/);
            assert.match(payload.messages[0].content, /untrusted data/);
            assert.ok(payload.messages[1].content.endsWith(source));
            return response(english);
        },
    });
    assert.equal(result, english);
});

for (const [name, fetchImpl] of [
    ['HTTP failure', async () => ({ ok: false })],
    ['timeout', async () => { throw new Error('credential-containing transport error'); }],
    ['invalid JSON', async () => ({ ok: true, json: async () => { throw new SyntaxError(); } })],
    ['missing choices', async () => ({ ok: true, json: async () => ({}) })],
    ['empty content', async () => response('')],
    ['non-string content', async () => response({ text: english })],
    ['truncated content', async () => response(english, 'length')],
    ['Chinese content', async () => response('### Fixes\n- 修复显示层级')],
    ['unexpected headings', async () => response('### 安装\n- Install the mod.')],
    ['unstructured response', async () => response('Sorry, I cannot help.')],
    ['code fences', async () => response(`\`\`\`markdown\n${english}\`\`\``)],
]) {
    test(`${name} produces a safe English fallback without logging the response`, async () => {
        const warnings = [];
        assert.equal(await summarize('commits', fallback, { ...options, fetchImpl, warn: value => warnings.push(value) }), fallback);
        assert.equal(warnings.length, 1);
        assert.doesNotMatch(warnings[0], /credential-containing|test-only/);
    });
}

test('verifies the packaged mod ID, expanded version, and Minecraft version', () => {
    const mod = { id: 'projectexpansion', version: metadata.artifactVersion, depends: { minecraft: '1.21.1' } };
    validateModMetadata(mod, metadata);
    for (const wrong of [{ ...mod, id: 'projecte' }, { ...mod, version: '${version}' }, { ...mod, depends: {} }]) {
        assert.throws(() => validateModMetadata(wrong, metadata), /Built JAR/);
    }
});

test('release body identifies the correct prerequisite and separates player and source downloads', () => {
    const body = renderReleaseBody(metadata, english, [
        { name: metadata.mainJar, size: 1024, description: '**Main mod — install this file**' },
        { name: metadata.sourcesJar, size: 2048, description: 'Source code' },
    ]);
    assert.match(body, /ProjectEF\/releases\/tag\/1\.3\.1/);
    assert.match(body, /ProjectExpansionNeo-1\.21\.1-1\.1\.0\.jar/);
    assert.match(body, /bundled by ProjectEF Neo, not by this expansion/);
    assert.match(body, /sources JAR/);
    assert.match(body, /1\.0 KiB/);
    assert.doesNotMatch(body, /\p{Script=Han}/u);
});
