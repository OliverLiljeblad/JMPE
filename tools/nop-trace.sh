#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

gradle --no-daemon test --rerun-tasks \
  --tests "com.JMPE.cpu.m68k.M68kCpuStepTest.traceNopPipelineToConsole" \
  --info --console=plain 2>&1 | grep -E "\[nop-trace\]|\[m68k-step\]"
