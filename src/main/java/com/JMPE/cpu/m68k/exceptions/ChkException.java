package com.JMPE.cpu.m68k.exceptions;

/**
 * Runtime signal used by CHK execution to request the 68000 CHK exception vector.
 */
public final class ChkException extends RuntimeException implements SimpleVectoredException {
    private final int vector;

    public ChkException(int vector) {
        super("CHK triggered exception vector " + validateVector(vector));
        this.vector = vector;
    }

    public int vector() {
        return vector;
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.CHK;
    }

    private static int validateVector(int vector) {
        if (vector != ExceptionVector.CHK.vectorNumber()) {
            throw new IllegalArgumentException("Mac Plus CHK currently supports only exception vector 6");
        }
        return vector;
    }
}
