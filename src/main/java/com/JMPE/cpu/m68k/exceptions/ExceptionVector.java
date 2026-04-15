package com.JMPE.cpu.m68k.exceptions;

public enum ExceptionVector {
    BUS_ERROR(2, ExceptionFrameKind.GROUP_0),
    ADDRESS_ERROR(3, ExceptionFrameKind.GROUP_0),
    ILLEGAL_INSTRUCTION(4, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    DIVIDE_BY_ZERO(5, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    CHK(6, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    TRAPV(7, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    PRIVILEGE_VIOLATION(8, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    TRACE(9, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    LINE_A_TRAP(10, ExceptionFrameKind.SIX_BYTE_SIMPLE),
    LINE_F_TRAP(11, ExceptionFrameKind.SIX_BYTE_SIMPLE);

    private final int vectorNumber;
    private final ExceptionFrameKind frameKind;

    ExceptionVector(int vectorNumber, ExceptionFrameKind frameKind) {
        this.vectorNumber = vectorNumber;
        this.frameKind = frameKind;
    }

    public int vectorNumber() {
        return vectorNumber;
    }

    public ExceptionFrameKind frameKind() {
        return frameKind;
    }

    public int vectorAddress() {
        return vectorNumber * 4;
    }

    public boolean usesSimpleFrame() {
        return frameKind == ExceptionFrameKind.SIX_BYTE_SIMPLE;
    }

    public static int interruptAutovectorNumber(int interruptLevel) {
        if (interruptLevel < 1 || interruptLevel > 7) {
            throw new IllegalArgumentException("interrupt level must be in range 1..7");
        }
        return 24 + interruptLevel;
    }

    public static int trapVectorNumber(int trapNumber) {
        if (trapNumber < 0 || trapNumber > 0xF) {
            throw new IllegalArgumentException("TRAP immediate must be in range 0..15");
        }
        return 32 + trapNumber;
    }
}
