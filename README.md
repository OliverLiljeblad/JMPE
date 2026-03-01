# JMPE

**JMPE** is a Java-based Mac Plus Emulator built by a team of college students.

## Creators
- Oliver
- Jonatan
- Clinton
- Noel

## Goal
Build a clean, modular emulator project that is easy to understand, test, and extend.

## Planned Scope
- CPU emulation core
- Memory and ROM handling
- Display and input emulation
- Incremental testing and documentation

## Project Structure (initial)
```
JMPE/
  README.md
  build.gradle
  settings.gradle
  .gitignore
  .editorconfig

  docs/
    architecture.md
    memory-map.md
    testing.md
    roadmap.md

  roms/            # gitignored (hashes documented in docs)
  disks/           # gitignored
  tools/
    singlestep/
      decode.py

  src/main/java/com.JMPE
    Main.java

    machine/
      MacPlusMachine.java
      Clock.java
      Interrupts.java
      Resettable.java

    bus/
      Bus.java
      MemoryRegion.java
      AddressSpace.java
      Ram.java
      Rom.java
      Mmio.java

    cpu/m68k/
      M68kCpu.java
      Registers.java
      StatusRegister.java      # merged with Flags — owns SR bits + calc methods
      AddressingModes.java
      EffectiveAddress.java    # sealed interface + records
      Decoder.java
      Instruction.java
      dispatch/
        DispatchTable.java     # thin wiring only — no logic, just delegates
        Op.java
      instructions/
        data/
          Move.java
          MoveQ.java
          Lea.java
          Pea.java
          Movem.java
        arithmetic/
          Add.java
          Sub.java
          Cmp.java
          Addq.java
          Subq.java
        logic/
          And.java
          Or.java
          Eor.java
          Not.java
        shift/
          Lsl.java
          Lsr.java
          Asl.java
          Asr.java
          Rol.java
          Ror.java
        control/
          Bra.java
          Bcc.java
          Bsr.java
          Jmp.java
          Jsr.java
          Rts.java
          Rte.java
          Trap.java
          Stop.java
          Nop.java
      exceptions/              # new — M68k exception/trap handling
        ExceptionVector.java   # enum of all 68k vector table entries
        AddressError.java
        BusError.java
        PrivilegeViolation.java
        ExceptionDispatcher.java  # decides which exception fires + stacks frame

    devices/
      via/
        Via6522.java
        ViaPorts.java
        ViaRtcBridge.java      # new — explicit interface for VIA <-> RTC serial protocol
      scc/
        Scc8530.java
      iwm/
        Iwm.java
        FloppyDrive.java
        DiskImage.java
        formats/
          Dsk.java
          Img.java
      video/
        VideoController.java
        Framebuffer1bpp.java
      sound/
        SoundGenerator.java
      rtc/
        RtcPram.java
      input/
        KeyboardController.java
        MouseController.java

    ui/
      DesktopWindow.java
      Renderer.java
      InputBindings.java
      DebugOverlay.java
      debugger/                # new — dev tooling
        Disassembler.java      # M68k disassembler for stepping through code
        MemoryInspector.java   # watch/inspect arbitrary address ranges
        DebuggerWindow.java    # UI shell for the above

    util/
      Bits.java
      Hex.java
      Logger.java
      RomLoader.java
      Checksums.java
      Preconditions.java

  src/test/java/com.JMPE
    harness/
      SingleStepLoader.java
      CpuStateAdapter.java
      DiffPrinter.java
      MemoryAsserts.java

    cpu/
      Singlestep_Move_Test.java
      Singlestep_Add_Test.java
      Singlestep_Branch_Test.java

    integration/
      BootsToQuestionMarkTest.java
      SmallProgramTest.java
```

## Team Workflow
- Feature branches for all work
- Pull requests for review
- Small, testable commits

## Resources
- General
  - https://en.wikipedia.org/wiki/Macintosh_Plus
- CPU (Motorola 68000)
  - https://www.nxp.com/docs/en/reference-manual/MC68000UM.pdf
  - https://datasheets.chipdb.org/Motorola/68000/mc68000.pdf
  - https://en.wikipedia.org/wiki/Motorola_68000#Instruction_set_details
  - https://www.bitsavers.org/pdf/codata/05-0004-01_Codata_CPU_Board_Manual_Jul83.pdf
## License
MIT (see `LICENSE`)
