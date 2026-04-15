package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Cmpa_Test {
    @Test
    void executeComparesSignExtendedWordSourceAgainstAddressRegister() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Cmpa.execute(Size.WORD, () -> 0xFFFF, () -> 0xFFFF_FFFF, conditionCodes);

        assertAll(
            () -> assertEquals(Cmpa.EXECUTION_CYCLES, cycles),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeComparesLongSourceAgainstAddressRegister() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Cmpa.execute(Size.LONG, () -> 0x0000_0002, () -> 0x0000_0001, conditionCodes);

        assertAll(
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsUnsizedAndByteOperations() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "CMPA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Cmpa.execute(Size.BYTE, () -> 0, () -> 0, conditionCodes)
                ).getMessage()
            ),
            () -> assertEquals(
                "CMPA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Cmpa.execute(Size.UNSIZED, () -> 0, () -> 0, conditionCodes)
                ).getMessage()
            )
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
