package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Roxl_Test {
    @Test
    void executeRotatesLeftThroughExtendAndUpdatesCarryAndExtend() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Roxl.execute(Size.BYTE, 1, true, () -> 0x80, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Roxl.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x01, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeLeavesExtendUnchangedWhenCountIsZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();
        conditionCodes.extend = true;

        Roxl.execute(Size.WORD, 0, false, () -> 0x1234, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x1234, writtenValue.get()),
            () -> assertFalse(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    private static final class TrackingConditionCodes implements Roxl.ConditionCodes {
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
