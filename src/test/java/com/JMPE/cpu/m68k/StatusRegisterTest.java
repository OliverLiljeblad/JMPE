package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.instructions.arithmetic.Add;
import com.JMPE.cpu.m68k.instructions.data.Move;
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

    @Test
    void writesInterruptMaskAndTraceBit() {
        StatusRegister sr = new StatusRegister();

        sr.setInterruptMask(7);
        sr.setTrace(true);

        assertEquals(7, sr.interruptMask());
        assertTrue(sr.isTraceSet());
        assertThrows(IllegalArgumentException.class, () -> sr.setInterruptMask(8));
    }

    @Test
    void ccrWritePreservesUpperStatusBits() {
        StatusRegister sr = new StatusRegister();
        sr.setInterruptMask(5);
        sr.setSupervisor(true);
        sr.setConditionCodeRegister(0x001F);

        assertEquals(5, sr.interruptMask());
        assertTrue(sr.isSupervisorSet());
        assertEquals(0x1F, sr.conditionCodeRegister());
    }

    @Test
    void moveConditionCodesAdapterUpdatesNzvcWithoutTouchingX() {
        StatusRegister sr = new StatusRegister();
        sr.setExtend(true);

        Move.updateConditionCodes(0x00, Size.BYTE, sr.moveConditionCodes());

        assertTrue(sr.isZeroSet());
        assertFalse(sr.isNegativeSet());
        assertFalse(sr.isOverflowSet());
        assertFalse(sr.isCarrySet());
        assertTrue(sr.isExtendSet());
    }

    @Test
    void arithmeticConditionCodesAdapterSetsExtendWithCarry() {
        StatusRegister sr = new StatusRegister();
        Add.execute(
            Size.BYTE,
            () -> 0x01,
            () -> 0xFF,
            ignored -> {
            },
            sr.addConditionCodes()
        );

        assertTrue(sr.isCarrySet());
        assertTrue(sr.isExtendSet());
        assertTrue(sr.isZeroSet());
    }
}
