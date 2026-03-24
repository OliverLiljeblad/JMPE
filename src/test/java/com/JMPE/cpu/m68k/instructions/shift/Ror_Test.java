package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.data.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ror_Test {
    @Test
    void executeRotatesLSBIntoMSBAndSetsCarry() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Rotating 0x01 right by 1: LSB wraps into MSB (0x80), carry reflects the shifted-out LSB.
        int cycles = Ror.execute(Size.BYTE, 1, () -> 0x01, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Ror.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0x80, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry)
        );
    }

    @Test
    void executeRotatesWordValueRightAndClearsNegative() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Rotating 0x8000 right by 1: MSB shifts to bit 14 (0x4000), no carry since LSB was 0.
        Ror.execute(Size.WORD, 1, () -> 0x8000, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x4000, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeWithZeroCountLeavesResultAndClearsCarry() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // count=0: no rotation; carry is cleared.
        Ror.execute(Size.BYTE, 0, () -> 0x55, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x55, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsNegativeCount() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Ror.execute(Size.BYTE, -1, () -> 0, value -> {}, conditionCodes)
        );

        assertEquals("count must be >= 0", exception.getMessage());
    }

    private static final class TrackingConditionCodes implements Move.ConditionCodes {
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
