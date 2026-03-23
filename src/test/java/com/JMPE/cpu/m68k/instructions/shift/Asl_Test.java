package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.instructions.data.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Asl_Test {
    @Test
    void executeSetsCarryExtendAndOverflowOnByteShiftOfMSB() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Shifting 0x80 left by 1: MSB becomes carry, result wraps to 0, sign flips 1→0 so overflow is set.
        int cycles = Asl.execute(Move.Size.BYTE, 1, () -> 0x80, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Asl.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertTrue(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry),
                () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeSetsOverflowAndNegativeWhenSignFlipsFromZeroToOne() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Shifting 0x40 left by 1: sign changes from 0 to 1, so overflow is set.
        Asl.execute(Move.Size.BYTE, 1, () -> 0x40, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x80, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertTrue(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry),
                () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeWithZeroCountLeavesResultAndDoesNotSetExtend() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // count=0: no shift; extend is never written; flags reflect the original value.
        Asl.execute(Move.Size.BYTE, 0, () -> 0x01, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x01, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry),
                () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsNegativeCount() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Asl.execute(Move.Size.BYTE, -1, () -> 0, value -> {}, conditionCodes)
        );

        assertEquals("count must be >= 0", exception.getMessage());
    }

    private static final class TrackingConditionCodes implements Asl.ConditionCodes {
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
