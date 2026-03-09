package com.JMPE.cpu.m68k;

import com.JMPE.bus.Rom;

/**
 * Minimal CPU shell used to wire core components during early emulator milestones.
 */
public final class M68kCpu {
    private static final int RESET_INTERRUPT_MASK = 7;

    private final Registers registers;
    private final StatusRegister statusRegister;

    public M68kCpu() {
        this(new Registers(), new StatusRegister());
    }

    public M68kCpu(StatusRegister statusRegister) {
        this(new Registers(), statusRegister);
    }

    public M68kCpu(Registers registers, StatusRegister statusRegister) {
        if (registers == null) {
            throw new IllegalArgumentException("registers must not be null");
        }
        if (statusRegister == null) {
            throw new IllegalArgumentException("statusRegister must not be null");
        }
        this.registers = registers;
        this.statusRegister = statusRegister;
    }

    public Registers registers() {
        return registers;
    }

    public StatusRegister statusRegister() {
        return statusRegister;
    }

    /**
     * Applies 68000 reset bootstrap state from ROM vectors.
     * <p>
     * On reset, the CPU loads SSP and PC from vector table offsets 0 and 4, enters supervisor mode,
     * clears trace, and masks interrupts to level 7.
     * </p>
     */
    public void resetFromRom(Rom rom) {
        if (rom == null) {
            throw new IllegalArgumentException("rom must not be null");
        }

        registers.setStackPointer(rom.initialSupervisorStackPointer());
        registers.setProgramCounter(rom.initialProgramCounter());

        statusRegister.setRawValue(0);
        statusRegister.setSupervisor(true);
        statusRegister.setTrace(false);
        statusRegister.setInterruptMask(RESET_INTERRUPT_MASK);
    }
}
