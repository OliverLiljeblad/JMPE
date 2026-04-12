package com.JMPE.cpu.m68k.exceptions;

/**
 * Runtime placeholder for a CHK trap until the CPU vectors execution-time
 * exceptions through the full 68000 exception model.
 */
public final class ChkException extends RuntimeException {
    private final int vector;

    public ChkException(int vector) {
        super("CHK triggered exception vector " + vector);
        this.vector = vector;
    }

    public int vector() {
        return vector;
    }
}
