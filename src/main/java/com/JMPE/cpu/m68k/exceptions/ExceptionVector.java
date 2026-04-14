package com.JMPE.cpu.m68k.exceptions;

public enum ExceptionVector {
    BUS_ERROR(2),
    ADDRESS_ERROR(3),
    ILLEGAL_INSTRUCTION(4),
    DIVIDE_BY_ZERO(5),
    CHK(6),
    TRAPV(7),
    PRIVILEGE_VIOLATION(8),
    TRACE(9),
    LINE_A_TRAP(10),
    LINE_F_TRAP(11);

    private final int vectorNumber;

    ExceptionVector(int vectorNumber) {
        this.vectorNumber = vectorNumber;
    }

    public int vectorNumber() {
        return vectorNumber;
    }

    public int vectorAddress() {
        return vectorNumber * 4;
    }

    public static int trapVectorNumber(int trapNumber) {
        if (trapNumber < 0 || trapNumber > 0xF) {
            throw new IllegalArgumentException("TRAP immediate must be in range 0..15");
        }
        return 32 + trapNumber;
    }
}
