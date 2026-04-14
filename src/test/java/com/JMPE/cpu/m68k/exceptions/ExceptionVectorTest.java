package com.JMPE.cpu.m68k.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExceptionVectorTest {
    @Test
    void marksGroupZeroFaultsSeparatelyFromSimpleFrameVectors() {
        assertEquals(ExceptionFrameKind.GROUP_0, ExceptionVector.BUS_ERROR.frameKind());
        assertEquals(ExceptionFrameKind.GROUP_0, ExceptionVector.ADDRESS_ERROR.frameKind());
        assertFalse(ExceptionVector.BUS_ERROR.usesSimpleFrame());
        assertFalse(ExceptionVector.ADDRESS_ERROR.usesSimpleFrame());
    }

    @Test
    void marksTrapStyleCpuFaultsAsSimpleFrameVectors() {
        assertEquals(ExceptionFrameKind.SIX_BYTE_SIMPLE, ExceptionVector.ILLEGAL_INSTRUCTION.frameKind());
        assertEquals(ExceptionFrameKind.SIX_BYTE_SIMPLE, ExceptionVector.DIVIDE_BY_ZERO.frameKind());
        assertEquals(ExceptionFrameKind.SIX_BYTE_SIMPLE, ExceptionVector.CHK.frameKind());
        assertEquals(ExceptionFrameKind.SIX_BYTE_SIMPLE, ExceptionVector.PRIVILEGE_VIOLATION.frameKind());
        assertTrue(ExceptionVector.ILLEGAL_INSTRUCTION.usesSimpleFrame());
        assertTrue(ExceptionVector.LINE_F_TRAP.usesSimpleFrame());
    }

    @Test
    void encodesTrapImmediateVectorNumbers() {
        assertEquals(32, ExceptionVector.trapVectorNumber(0));
        assertEquals(47, ExceptionVector.trapVectorNumber(15));
        assertThrows(IllegalArgumentException.class, () -> ExceptionVector.trapVectorNumber(-1));
        assertThrows(IllegalArgumentException.class, () -> ExceptionVector.trapVectorNumber(16));
    }
}
