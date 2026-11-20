#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
./gradlew -q --console=plain conformancePayload >/dev/null
node scripts/conformance/sort-payload.mjs build/conformance/payload.json
