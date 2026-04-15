#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

rom_path="${1:-${JMPE_ROM:-}}"
step_limit="${2:-${JMPE_BOOT_STEPS:-32}}"

if [[ -z "${rom_path}" ]]; then
  echo "usage: $0 /path/to/mac-plus.rom [step-count]" >&2
  echo "or set JMPE_ROM=/path/to/mac-plus.rom" >&2
  exit 1
fi

JMPE_ROM="${rom_path}" JMPE_BOOT_STEPS="${step_limit}" \
gradle --no-daemon test --rerun-tasks \
  -Djmpe.rom="${rom_path}" \
  -Djmpe.boot.steps="${step_limit}" \
  --tests "com.JMPE.integration.BootsToQuestionMarkTest.runsLocalMacPlusRomForConfiguredBootSmokeSteps"
