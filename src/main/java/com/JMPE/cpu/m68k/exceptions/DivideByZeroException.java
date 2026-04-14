package com.JMPE.cpu.m68k.exceptions;

/**
 * Runtime placeholder for the 68000 integer divide-by-zero trap until the CPU
 * vectors execution-time exceptions through the full exception model.
 */
public final class DivideByZeroException extends RuntimeException {
    public static final int VECTOR = 5;

    private final int vector;

    public DivideByZeroException() {
        super("Integer divide by zero triggered exception vector " + VECTOR);
        this.vector = VECTOR;
    }

    public int vector() {
        return vector;
    }
}
