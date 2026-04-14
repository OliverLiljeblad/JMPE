package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Addi_Test {
    @Test
    void executeAddsImmediateAndSetsOverflow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Addi.execute(Size.BYTE, () -> 1, () -> 0x7F, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Addi.EXECUTION_CYCLES_DN, cycles),
            () -> assertEquals(0x80, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertTrue(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry),
            () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeAddsImmediateAndSetsCarryAndExtendOnWrap() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Addi.execute(Size.BYTE, () -> 1, () -> 0xFF, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x00, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsNullInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Addi.execute(null, () -> 0, () -> 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addi.execute(Size.BYTE, null, () -> 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addi.execute(Size.BYTE, () -> 0, null, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addi.execute(Size.BYTE, () -> 0, () -> 0, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Addi.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, null));
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
