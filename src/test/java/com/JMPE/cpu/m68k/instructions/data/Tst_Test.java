package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tst_Test {
    @Test
    void executeMasksByteOperandAndSetsNegative() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Tst.execute(Size.BYTE, () -> 0x0000_0180, conditionCodes);

        assertAll(
                () -> assertEquals(Tst.EXECUTION_CYCLES, cycles),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsZeroForMaskedWordZero() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Tst.execute(Size.WORD, () -> 0x0001_0000, conditionCodes);

        assertAll(
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsNullInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertThrows(NullPointerException.class, () -> Tst.execute(null, () -> 0, conditionCodes));
        assertThrows(NullPointerException.class, () -> Tst.execute(Size.BYTE, null, conditionCodes));
        assertThrows(NullPointerException.class, () -> Tst.execute(Size.BYTE, () -> 0, null));
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
