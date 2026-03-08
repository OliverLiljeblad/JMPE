package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusRegisterTest {
    @Test
    void setsAndReadsIndividualFlags() {
        StatusRegister sr = new StatusRegister();

        sr.setCarry(true);
        sr.setZero(true);
        sr.setSupervisor(true);

        assertTrue(sr.isCarrySet());
        assertTrue(sr.isZeroSet());
        assertTrue(sr.isSupervisorSet());

        sr.setCarry(false);
        sr.setZero(false);
        sr.setSupervisor(false);

        assertFalse(sr.isCarrySet());
        assertFalse(sr.isZeroSet());
        assertFalse(sr.isSupervisorSet());
    }

    @Test
    void updatesAddFlagsForSignedOverflowAndCarry() {
        StatusRegister sr = new StatusRegister();

        // 0x7F + 0x01 => 0x80: signed overflow and negative set, no carry.
        sr.updateAddFlags(0x01, 0x7F, 0x80, 8);
        assertTrue(sr.isOverflowSet());
        assertTrue(sr.isNegativeSet());
        assertFalse(sr.isCarrySet());

        // 0xFF + 0x01 => 0x00: carry/extend and zero set.
        sr.updateAddFlags(0x01, 0xFF, 0x100, 8);
        assertTrue(sr.isCarrySet());
        assertTrue(sr.isExtendSet());
        assertTrue(sr.isZeroSet());
    }

    @Test
    void updatesSubFlagsForBorrowAndSignedOverflow() {
        StatusRegister sr = new StatusRegister();

        // 0x00 - 0x01 => 0xFF: borrow/carry set and negative set.
        sr.updateSubFlags(0x01, 0x00, 0xFF, 8);
        assertTrue(sr.isCarrySet());
        assertTrue(sr.isNegativeSet());
        assertFalse(sr.isZeroSet());

        // 0x80 - 0x01 => 0x7F: signed overflow for 8-bit subtraction.
        sr.updateSubFlags(0x01, 0x80, 0x7F, 8);
        assertTrue(sr.isOverflowSet());
    }

    @Test
    void rejectsUnsupportedOperandWidths() {
        StatusRegister sr = new StatusRegister();
        assertThrows(IllegalArgumentException.class, () -> sr.updateAddFlags(1, 1, 2, 24));
        assertThrows(IllegalArgumentException.class, () -> sr.updateSubFlags(1, 1, 0, 64));
    }
}
