# JMPE

**JMPE** is a Java-based Macintosh Plus emulator project built by a college team that cares about clean architecture, readable code, and incremental, test-driven progress.

## Why this project is worth following
- **Classic hardware challenge:** emulate a real Motorola 68000-era machine.
- **Modern engineering style:** feature branches, pull requests, CI, and focused tests.
- **Readable foundations:** intentionally modular packages so contributors can ramp up quickly.

## Current focus
We are building the emulator in practical slices:
1. Core machine composition
2. ROM and memory behavior
3. CPU status register and instruction execution
4. Device wiring (video, input, storage, etc.)

## Quick start
> Note: the Gradle wrapper JAR is currently missing in this repository, so use system Gradle.

```bash
gradle --no-daemon test
gradle --no-daemon build
```

## Helpful trace commands
To see the current end-to-end trace flows with only the important lines:

```bash
# CPU-layer NOP trace
./tools/nop-trace.sh

# Machine-layer NOP trace
./tools/machine-nop-trace.sh

# CPU-layer CLR.B D0 trace
./tools/clr-trace.sh

# Machine-layer CLR.B D0 trace
./tools/machine-clr-trace.sh

# CPU-layer NOT.B D0 trace
./tools/not-trace.sh

# Machine-layer NOT.B D0 trace
./tools/machine-not-trace.sh

# CPU-layer ANDI.B #$80,D0 trace
./tools/andi-trace.sh

# Machine-layer ANDI.B #$80,D0 trace
./tools/machine-andi-trace.sh

# CPU-layer ORI.B #$80,D0 trace
./tools/ori-trace.sh

# Machine-layer ORI.B #$80,D0 trace
./tools/machine-ori-trace.sh

# CPU-layer EORI.B #$80,D0 trace
./tools/eori-trace.sh

# Machine-layer EORI.B #$80,D0 trace
./tools/machine-eori-trace.sh

# CPU-layer CMPI.B #$01,D0 trace
./tools/cmpi-trace.sh

# Machine-layer CMPI.B #$01,D0 trace
./tools/machine-cmpi-trace.sh

# CPU-layer TST.B D0 trace
./tools/tst-trace.sh

# Machine-layer TST.B D0 trace
./tools/machine-tst-trace.sh

# Machine-layer 10-instruction checkpoint trace
./tools/machine-ten-step-trace.sh
```

## Optional local ROM boot smoke test
If you have a local Mac Plus ROM outside the repository, you can run a bounded boot smoke test:

```bash
./tools/boot-rom-smoke.sh /path/to/mac-plus.rom

# or increase the step budget
./tools/boot-rom-smoke.sh /path/to/mac-plus.rom 64
```

If you prefer running Gradle directly, the test also accepts `-Djmpe.rom=/path/to/mac-plus.rom`
and `-Djmpe.boot.steps=32`. ROM binaries are not committed to this repository.

## Optional 68000 single-step conformance smoke tests
If you clone `SingleStepTests/680x0` under `~/cpu-testdata/680x0`, the single-step smoke tests
auto-detect `~/cpu-testdata/680x0/68000/v1` and run a bounded subset of the external corpus.
You can also point at another checkout explicitly:

```bash
gradle --no-daemon test \
  -Djmpe.680x0.enable=true \
  -Djmpe.680x0.dir=/path/to/680x0/68000/v1 \
  -Djmpe.680x0.cases=25 \
  --tests 'com.JMPE.cpu.Singlestep_Add_Test' \
  --tests 'com.JMPE.cpu.Singlestep_Move_Test' \
  --tests 'com.JMPE.cpu.Singlestep_Branch_Test'
```

Use `-Djmpe.680x0.cases=all` to remove the per-file case cap. By default the harness compares
architected final CPU state and RAM writes for each one-instruction case; add
`-Djmpe.680x0.cycles=true` to also assert cycle counts.

## Project layout
```text
src/main/java/com/JMPE/
  bus/        # ROM, RAM, address-space primitives
  cpu/m68k/   # CPU core, status register, decoder, instructions
  devices/    # VIA, SCC, IWM, video, input, sound, RTC
  machine/    # machine composition (MacPlusMachine, clock, interrupts)
  ui/         # renderer, overlays, debugger-facing UI shells
  util/       # shared helpers (bits, hex, ROM loading, logging)

src/test/java/com/JMPE/
  bus/
  cpu/
  harness/
  integration/
  machine/
  util/
```

## Team workflow
- Branch from `main` for each change.
- Keep commits small, purposeful, and reviewable.
- Open a pull request early and iterate with feedback.
- Ensure CI and local tests pass before merge.

## Creators
- Oliver
- Jonatan
- Clinton
- Noel

## Technical references
- Macintosh Plus overview: https://en.wikipedia.org/wiki/Macintosh_Plus
- Motorola 68000 User Manual: https://www.nxp.com/docs/en/reference-manual/MC68000UM.pdf
- Motorola 68000 datasheet mirror: https://datasheets.chipdb.org/Motorola/68000/mc68000.pdf
- 68000 instruction set overview: https://en.wikipedia.org/wiki/Motorola_68000#Instruction_set_details
- Historical board manual: https://www.bitsavers.org/pdf/codata/05-0004-01_Codata_CPU_Board_Manual_Jul83.pdf
- https://m680x0.github.io/doc/official-docs.html
- https://web.njit.edu/~rosensta/classes/architecture/252software/code.pdf

## License
MIT (see `LICENSE`)
