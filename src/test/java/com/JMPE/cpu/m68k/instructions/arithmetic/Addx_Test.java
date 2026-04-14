package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Addx_Test {
    @Test
    void executeAddsExtendInputAndSetsOverflow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Addx.execute(Size.BYTE, () -> 0x00, () -> 0x7F, writtenValue::set, true, true, conditionCodes);

        assertAll(
            () -> assertEquals(Addx.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x80, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertTrue(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry),
            () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executePreservesClearedZeroWhenWrappedResultIsZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Addx.execute(Size.BYTE, () -> 0x00, () -> 0xFF, writtenValue::set, true, false, conditionCodes);

        assertAll(
            () -> assertEquals(0x00, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsNullInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Addx.execute(null, () -> 0, () -> 0, value -> { }, false, true, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addx.execute(Size.BYTE, null, () -> 0, value -> { }, false, true, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addx.execute(Size.BYTE, () -> 0, null, value -> { }, false, true, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addx.execute(Size.BYTE, () -> 0, () -> 0, null, false, true, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addx.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, false, true, null));
    }

    private static final class TrackingConditionCodes implements Add.ConditionCodes {
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
