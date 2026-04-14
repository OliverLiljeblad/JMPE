package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Swap_Test {
    @Test
    void executeSwapsUpperAndLowerWordsAndUpdatesMoveFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Swap.execute(0x1234_5678, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Swap.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x5678_1234, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsZeroWhenSwappedResultIsZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Swap.execute(0x0000_0000, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x0000_0000, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero)
        );
    }

    @Test
    void executeRejectsNullInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Swap.execute(0, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Swap.execute(0, value -> { }, null));
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
