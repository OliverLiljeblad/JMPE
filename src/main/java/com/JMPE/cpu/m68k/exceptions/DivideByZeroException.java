package com.JMPE.cpu.m68k.exceptions;

/**
 * Runtime signal used by DIVU/DIVS execution to request the 68000 divide-by-zero vector.
 */
public final class DivideByZeroException extends RuntimeException implements SimpleVectoredException {
    public static final int VECTOR = ExceptionVector.DIVIDE_BY_ZERO.vectorNumber();

    public DivideByZeroException() {
        super("Integer divide by zero triggered exception vector " + VECTOR);
    }

    public int vector() {
        return VECTOR;
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.DIVIDE_BY_ZERO;
    }
}
