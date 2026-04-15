package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Subi_Test {
    @Test
    void executeSubtractsImmediateAndSetsBorrowFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Subi.execute(Size.BYTE, () -> 1, () -> 0x00, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Subi.EXECUTION_CYCLES_DN, cycles),
            () -> assertEquals(0xFF, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeSubtractsImmediateAndSetsZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Subi.execute(Size.WORD, () -> 1, () -> 1, writtenValue::set, conditionCodes);

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

        assertThrows(NullPointerException.class, () -> Subi.execute(null, () -> 0, () -> 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Subi.execute(Size.BYTE, null, () -> 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Subi.execute(Size.BYTE, () -> 0, null, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Subi.execute(Size.BYTE, () -> 0, () -> 0, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Subi.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, null));
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
