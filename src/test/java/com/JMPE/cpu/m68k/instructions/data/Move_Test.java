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

public class Move_Test {
    @Test
    void executeMasksByteResultUpdatesCcr() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Move.execute(Size.BYTE, 0x01FF, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Move.DEFAULT_CYCLES, cycles),
                () -> assertEquals(0x00FF, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsZeroAndClearsNegativeForMaskedWordZero() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Move.execute(Size.WORD, 0x0001_0000, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeReturnsExplicitCycleCount() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Move.execute(Size.LONG, 0x1234_5678, writtenValue::set, conditionCodes, 12);

        assertAll(
                () -> assertEquals(12, cycles),
                () -> assertEquals(0x1234_5678, writtenValue.get()),
                () -> assertFalse(conditionCodes.zero)
        );
    }

    @Test
    void executeRejectsNegativeCycleCounts() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Move.execute(Size.WORD, 0x1234, value -> {
                }, conditionCodes, -1)
        );

        assertEquals("cycles must be >= 0", exception.getMessage());
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
