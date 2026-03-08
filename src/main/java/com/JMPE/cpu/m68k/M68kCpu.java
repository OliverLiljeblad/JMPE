package com.JMPE.cpu.m68k;

/**
 * Minimal CPU shell used to wire core components during early emulator milestones.
 */
public final class M68kCpu {
    private final StatusRegister statusRegister;

    public M68kCpu() {
        this(new StatusRegister());
    }

    public M68kCpu(StatusRegister statusRegister) {
        if (statusRegister == null) {
            throw new IllegalArgumentException("statusRegister must not be null");
        }
        this.statusRegister = statusRegister;
    }

    public StatusRegister statusRegister() {
        return statusRegister;
    }
}
