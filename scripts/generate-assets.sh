#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir/src/main/generation"
npm ci --no-audit --no-fund --cache "$project_dir/.gradle_home/npm"
node --no-warnings --loader ts-node/esm run.ts
