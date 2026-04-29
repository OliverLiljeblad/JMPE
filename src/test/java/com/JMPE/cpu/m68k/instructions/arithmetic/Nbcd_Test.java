package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Nbcd_Test {
    @Test
    void executeNegatesPackedBcdDigitsAndSetsDecimalBorrow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Nbcd.execute(Size.BYTE, () -> 0x01, writtenValue::set, false, conditionCodes);

        assertAll(
            () -> assertEquals(Nbcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x99, writtenValue.get()),
            () -> assertTrue(conditionCodes.zeroCleared),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeLeavesStickyZeroSetForZeroResult() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Nbcd.execute(Size.BYTE, () -> 0x00, writtenValue::set, false, conditionCodes);

        assertAll(
            () -> assertEquals(0x00, writtenValue.get()),
            () -> assertFalse(conditionCodes.zeroCleared),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry),
            () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeUses68000DecimalCorrectionForInvalidDigits() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Nbcd.execute(Size.BYTE, () -> 0x27, writtenValue::set, true, conditionCodes);

        assertAll(
            () -> assertEquals(0x72, writtenValue.get()),
            () -> assertTrue(conditionCodes.zeroCleared),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsInvalidInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "NBCD on the Macintosh Plus 68000 supports only BYTE size",
                assertThrows(IllegalArgumentException.class,
                    () -> Nbcd.execute(Size.WORD, () -> 0, value -> { }, false, conditionCodes)
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Nbcd.execute(null, () -> 0, value -> { }, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Nbcd.execute(Size.BYTE, null, value -> { }, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Nbcd.execute(Size.BYTE, () -> 0, null, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Nbcd.execute(Size.BYTE, () -> 0, value -> { }, false, null))
        );
    }

    private static final class TrackingConditionCodes implements Nbcd.ConditionCodes {
        private boolean zeroCleared;
        private boolean negative;
        private boolean overflow;
        private boolean carry;
        private boolean extend;

        @Override
        public void clearZero() {
            zeroCleared = true;
        }

        @Override
        public void setNegative(boolean value) {
            negative = value;
        }

        @Override
        public void setOverflow(boolean value) {
            overflow = value;
        }

        @Override
        public void setCarry(boolean value) {
            carry = value;
        }

        @Override
        public void setExtend(boolean value) {
            extend = value;
        }
    }
}
