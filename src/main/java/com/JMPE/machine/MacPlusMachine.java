package com.JMPE.machine;

import com.JMPE.bus.Rom;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.util.RomLoader;

/**
 * Coarse machine composition entry point for integration tests.
 */
public final class MacPlusMachine {
    private final Rom rom;
    private final M68kCpu cpu;

    public MacPlusMachine(Rom rom) {
        this(rom, new M68kCpu());
    }

    public MacPlusMachine(Rom rom, M68kCpu cpu) {
        if (rom == null) {
            throw new IllegalArgumentException("rom must not be null");
        }
        if (cpu == null) {
            throw new IllegalArgumentException("cpu must not be null");
        }
        this.rom = rom;
        this.cpu = cpu;
    }

    public static MacPlusMachine fromRomBytes(byte[] romBytes, int baseAddress) {
        return new MacPlusMachine(RomLoader.fromBytes(romBytes, baseAddress));
    }

    public Rom rom() {
        return rom;
    }

    public M68kCpu cpu() {
        return cpu;
    }
}
