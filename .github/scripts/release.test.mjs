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
const outputMessage = (content, status = 'completed') => ({
    type: 'message', role: 'assistant', status,
    content: [{ type: 'output_text', text: content, annotations: [] }],
});
const outputResponse = (output, overrides = {}) => ({
    ok: true,
    json: async () => ({ status: 'completed', error: null, incomplete_details: null, output, ...overrides }),
});
const response = (content, status = 'completed') => outputResponse([outputMessage(content)], { status });

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

test('collects changelog for an explicit revision such as an existing tag', t => {
    const { cwd, git, commit } = repository(t);
    commit('Tagged change');
    git('tag', metadata.tag);
    commit('Later change');
    const changelog = collectChangelog(metadata, cwd, git('rev-parse', `refs/tags/${metadata.tag}^{commit}`));
    assert.match(changelog.source, /Tagged change/);
    assert.doesNotMatch(changelog.source, /Later change/);
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
            assert.equal(url, 'https://api.deepseek.com/responses');
            assert.equal(request.method, 'POST');
            assert.equal(request.headers.Authorization, 'Bearer test-only');
            assert.equal(request.redirect, 'error');
            assert.ok(request.signal instanceof AbortSignal);
            const payload = JSON.parse(request.body);
            assert.equal(payload.model, 'test-model');
            assert.match(payload.instructions, /entirely in English/);
            assert.match(payload.instructions, /untrusted data/);
            assert.ok(payload.input.endsWith(source));
            assert.deepEqual(payload.reasoning, { effort: 'none' });
            assert.equal(payload.max_output_tokens, 4096);
            assert.equal(payload.stream, false);
            assert.equal(Object.hasOwn(payload, 'messages'), false);
            assert.equal(Object.hasOwn(payload, 'max_tokens'), false);
            return response(english);
        },
    });
    assert.equal(result, english);
});

test('uses the supported default model and assembles only assistant output text', async () => {
    const result = await summarize('commits', fallback, {
        ...options,
        fetchImpl: async (_url, request) => {
            assert.equal(JSON.parse(request.body).model, 'deepseek-flash');
            return outputResponse([
                { type: 'reasoning', content: [{ type: 'reasoning_text', text: '这不是发布正文' }] },
                { ...outputMessage(''), content: [
                    { type: 'output_text', text: '### Fixes\n\n' },
                    { type: 'output_text', text: '- Keep counts visible.\n' },
                ] },
                outputMessage('### Other\n\n- Update documentation.\n'),
            ]);
        },
    });
    assert.equal(result, '### Fixes\n\n- Keep counts visible.\n\n### Other\n\n- Update documentation.\n');
});

test('normalizes harmless English heading depth and capitalization differences', async () => {
    for (const heading of ['## Fixes', '### FIXES', '#### fixes', '### Fixes ###']) {
        assert.equal(await summarize('commits', fallback, {
            ...options, fetchImpl: async () => response(`${heading}\n\n- Fix display order.`),
        }), '### Fixes\n\n- Fix display order.\n');
    }
});

for (const [name, fetchImpl, code] of [
    ['HTTP failure', async () => ({ ok: false, status: 401 }), 'HTTP_ERROR: status=401'],
    ['timeout', async () => { throw new DOMException('credential-containing timeout', 'TimeoutError'); }, 'TIMEOUT'],
    ['transport error', async () => { throw new Error('credential-containing transport error'); }, 'TRANSPORT_ERROR'],
    ['invalid JSON', async () => ({ ok: true, json: async () => { throw new SyntaxError(); } }), 'INVALID_JSON'],
    ['missing response fields', async () => ({ ok: true, json: async () => ({}) }), 'RESPONSE_NOT_COMPLETED'],
    ['legacy Chat Completions response', async () => ({ ok: true, json: async () => ({ choices: [{ message: { content: english }, finish_reason: 'stop' }] }) }), 'RESPONSE_NOT_COMPLETED'],
    ['empty content', async () => response(''), 'EMPTY_SUMMARY'],
    ['non-string content', async () => response({ text: english }), 'INVALID_OUTPUT_TEXT'],
    ...['incomplete', 'failed', 'cancelled', 'in_progress', 'queued'].map(status => [
        `${status} response`, async () => response(english, status), `RESPONSE_NOT_COMPLETED: status=${status}`,
    ]),
    ['response error', async () => outputResponse([outputMessage(english)], { error: { message: 'failed' } }), 'API_ERROR'],
    ['incomplete details', async () => outputResponse([outputMessage(english)], { incomplete_details: { reason: 'max_output_tokens' } }), 'OUTPUT_TOKEN_LIMIT'],
    ['missing output', async () => outputResponse(undefined), 'INVALID_OUTPUT'],
    ['non-array output', async () => outputResponse({ text: english }), 'INVALID_OUTPUT'],
    ['reasoning without a message', async () => outputResponse([{ type: 'reasoning', content: [{ type: 'reasoning_text', text: english }] }]), 'MISSING_OUTPUT_TEXT'],
    ['incomplete message', async () => outputResponse([outputMessage(english, 'incomplete')]), 'INVALID_MESSAGE'],
    ['non-assistant message', async () => outputResponse([{ ...outputMessage(english), role: 'user' }]), 'INVALID_MESSAGE'],
    ['empty message content', async () => outputResponse([{ ...outputMessage(english), content: [] }]), 'MISSING_OUTPUT_TEXT'],
    ['refusal mixed with text', async () => outputResponse([{ ...outputMessage(english), content: [
        { type: 'output_text', text: english }, { type: 'refusal', refusal: 'Cannot comply.' },
    ] }]), 'REFUSAL'],
    ['Chinese content', async () => response('### Fixes\n- 修复显示层级'), 'NON_ENGLISH_SUMMARY'],
    ['unexpected headings', async () => response('### Installation\n- Install the mod.'), 'INVALID_HEADINGS'],
    ['unstructured response', async () => response('Sorry, I cannot help.'), 'INVALID_HEADINGS'],
    ['code fences', async () => response(`\`\`\`markdown\n${english}\`\`\``), 'UNEXPECTED_CODE_FENCE'],
]) {
    test(`${name} produces a safe English fallback with a specific diagnostic`, async () => {
        const warnings = [];
        assert.equal(await summarize('commits', fallback, { ...options, fetchImpl, warn: value => warnings.push(value) }), fallback);
        assert.equal(warnings.length, 1);
        assert.ok(warnings[0].startsWith(`::warning title=Release summary::${code}`));
        assert.doesNotMatch(warnings[0], /credential-containing|test-only/);
    });
}

test('reports token exhaustion without leaking reasoning text', async () => {
    const warnings = [];
    const result = await summarize('commits', fallback, {
        ...options,
        warn: value => warnings.push(value),
        fetchImpl: async () => outputResponse([
            { type: 'reasoning', content: [{ type: 'reasoning_text', text: 'private reasoning' }] },
        ], {
            status: 'incomplete', incomplete_details: { reason: 'max_output_tokens' },
            usage: { output_tokens: 2048, output_tokens_details: { reasoning_tokens: 2048 } },
        }),
    });
    assert.equal(result, fallback);
    assert.match(warnings[0], /OUTPUT_TOKEN_LIMIT: output_tokens=2048 reasoning_tokens=2048/);
    assert.doesNotMatch(warnings[0], /private reasoning/);
});

test('diagnostics never echo arbitrary server or transport fields', async () => {
    const secret = 'test-only\n::error::untrusted message';
    for (const fetchImpl of [
        async () => ({ ok: false, status: secret }),
        async () => outputResponse([], { status: secret }),
        async () => outputResponse([], { error: { code: secret, message: secret } }),
        async () => outputResponse([], {
            incomplete_details: { reason: secret },
            usage: { output_tokens: secret, output_tokens_details: { reasoning_tokens: secret } },
        }),
        async () => { throw new Error(secret, { cause: { code: secret } }); },
    ]) {
        const warnings = [];
        assert.equal(await summarize('commits', fallback, { ...options, fetchImpl, warn: value => warnings.push(value) }), fallback);
        assert.equal(warnings.length, 1);
        assert.doesNotMatch(warnings[0], /test-only|untrusted|\n|::error::/);
    }
});

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
