package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Cmp_Test {
    @Test
    void executeSetsZeroForEqualOperands() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Cmp.execute(Size.WORD, () -> 0x1234, () -> 0x1234, conditionCodes);

        assertAll(
                () -> assertEquals(Cmp.EXECUTION_CYCLES, cycles),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsNegativeAndCarryWhenUnsignedBorrowOccurs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Cmp.execute(Size.WORD, () -> 0x0001, () -> 0x0000, conditionCodes);

        assertAll(
                () -> assertFalse(conditionCodes.zero),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsOverflowForSignedWraparoundCase() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Cmp.execute(Size.BYTE, () -> 0x01, () -> 0x80, conditionCodes);

        assertAll(
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
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
