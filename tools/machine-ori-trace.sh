#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

gradle --no-daemon test --rerun-tasks \
  --tests "com.JMPE.integration.SmallProgramTest.traceOriImmediateToDataRegisterThroughMachineLayerToConsole" \
  --info --console=plain 2>&1 | grep -E "\[machine-ori-trace\]|\[m68k-step\]"
