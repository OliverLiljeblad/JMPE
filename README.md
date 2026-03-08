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

## License
MIT (see `LICENSE`)
