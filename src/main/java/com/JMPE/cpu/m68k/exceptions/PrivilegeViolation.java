package com.JMPE.cpu.m68k.exceptions;

/**
 * Thrown when privileged CPU state is accessed from user mode.
 *
 * <p>
 * Privilege violations currently enter through the 68000 simple six-byte exception
 * frame path. Group-0 bus/address faults remain a separate later concern.
 * </p>
 */
public class PrivilegeViolation extends RuntimeException implements SimpleVectoredException {
    private static final int VECTOR = ExceptionVector.PRIVILEGE_VIOLATION.vectorNumber();

    public PrivilegeViolation() {
        this("Privileged instruction");
    }

    public PrivilegeViolation(String operation) {
        super(operation + " requires supervisor mode");
    }

    public int vector() {
        return VECTOR;
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.PRIVILEGE_VIOLATION;
    }
}
