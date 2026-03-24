package com.JMPE.machine;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Bus;
import com.JMPE.bus.Rom;
import com.JMPE.bus.RomRegion;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.util.RomLoader;

import java.util.function.Consumer;

/**
 * Coarse machine composition entry point for integration tests.
 */
public final class MacPlusMachine {
    private final Rom rom;
    private final M68kCpu cpu;
    private final AddressSpace bus;
    private final DispatchTable dispatchTable;

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
        this.bus = new AddressSpace();
        this.dispatchTable = new DispatchTable();
        this.bus.addRegion(new RomRegion(rom));
        this.cpu.resetFromRom(rom);
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

    public Bus bus() {
        return bus;
    }

    public M68kCpu.StepReport step() throws IllegalInstructionException {
        return cpu.step(bus, dispatchTable);
    }

    public M68kCpu.StepReport step(Consumer<String> reporter) throws IllegalInstructionException {
        return cpu.step(bus, dispatchTable, reporter);
    }

    public M68kCpu.StepReport stepWithConsoleReport() throws IllegalInstructionException {
        return cpu.stepWithConsoleReport(bus, dispatchTable);
    }
}
