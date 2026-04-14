package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neg_Test {
    @Test
    void executeNegatesDestinationAndSetsBorrowFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Neg.execute(Size.BYTE, () -> 1, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Neg.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFF, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeNegatesZeroAndClearsBorrowFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Neg.execute(Size.WORD, () -> 0, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x0000, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry),
            () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsNullInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Neg.execute(null, () -> 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Neg.execute(Size.BYTE, null, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Neg.execute(Size.BYTE, () -> 0, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Neg.execute(Size.BYTE, () -> 0, value -> { }, null));
    }

    private static final class TrackingConditionCodes implements Sub.ConditionCodes {
        private boolean negative;
        private boolean zero;
        private boolean overflow;
        private boolean carry;
        private boolean extend;

        @Override
        public void setNegative(boolean value) {
            negative = value;
        }

        @Override
        public void setZero(boolean value) {
            zero = value;
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
