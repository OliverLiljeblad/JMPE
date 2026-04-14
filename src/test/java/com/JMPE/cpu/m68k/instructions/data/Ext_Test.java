package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ext_Test {
    @Test
    void executeExtendsLowByteToWordAndUpdatesMoveFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Ext.execute(Size.WORD, 0x1234_5680, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Ext.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_FF80, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeExtendsLowWordToLongAndSetsZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Ext.execute(Size.LONG, 0x1234_0000, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x0000_0000, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsNullInputsAndUnsupportedSize() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Ext.execute(null, 0, value -> { }, conditionCodes));
        assertThrows(NullPointerException.class, () -> Ext.execute(Size.WORD, 0, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Ext.execute(Size.WORD, 0, value -> { }, null));
        assertThrows(IllegalArgumentException.class, () -> Ext.execute(Size.BYTE, 0, value -> { }, conditionCodes));
    }

    private static final class TrackingConditionCodes implements ConditionCodes {
        private boolean negative;
        private boolean zero;
        private boolean overflow;
        private boolean carry;

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
    }
}
