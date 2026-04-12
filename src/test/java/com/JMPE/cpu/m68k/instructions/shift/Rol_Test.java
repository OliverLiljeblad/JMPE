package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Rol_Test {
    @Test
    void executeRotatesMSBIntoLSBAndSetsCarry() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Rotating 0x80 left by 1: MSB wraps into bit 0, carry reflects the shifted-out MSB.
        int cycles = Rol.execute(Size.BYTE, 1, () -> 0x80, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Rol.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0x01, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry)
        );
    }

    @Test
    void executeRotatesWordValueAndSetsNegative() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Rotating 0x4000 left by 1: result is 0x8000 (MSB set = negative word), no carry.
        Rol.execute(Size.WORD, 1, () -> 0x4000, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x8000, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
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
        Rol.execute(Size.BYTE, 0, () -> 0x42, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x42, writtenValue.get()),
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
                () -> Rol.execute(Size.BYTE, -1, () -> 0, value -> {}, conditionCodes)
        );

        assertEquals("count must be >= 0", exception.getMessage());
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
