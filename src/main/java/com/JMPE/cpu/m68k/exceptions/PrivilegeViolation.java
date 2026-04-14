package com.JMPE.cpu.m68k.exceptions;

/**
 * Thrown when privileged CPU state is accessed from user mode.
 *
 * <p>
 * The current runtime treats execution-time CPU faults such as bus/address
 * errors as unchecked exceptions that bubble through {@code M68kCpu.step()},
 * where they are logged and later can be routed through a fuller exception
 * dispatcher. Privilege violations follow the same model for now.
 * </p>
 */
public class PrivilegeViolation extends RuntimeException {
    private static final int VECTOR = 8;

    public PrivilegeViolation() {
        this("Privileged instruction");
    }

    public PrivilegeViolation(String operation) {
        super(operation + " requires supervisor mode");
    }

    public int vector() {
        return VECTOR;
    }
}
