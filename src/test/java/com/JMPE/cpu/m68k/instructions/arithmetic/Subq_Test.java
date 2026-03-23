package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.instructions.data.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Subq_Test {
    @Test
    void executeSetsNegativeCarryAndExtendOnBorrow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Subq.execute(Move.Size.BYTE, 1, () -> 0x0000, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Subq.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0x00FF, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry),
                () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeSetsOverflowForSignedNegativeOverflow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Subq.execute(Move.Size.BYTE, 1, () -> 0x0080, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x007F, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertTrue(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry),
                () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsQuickValuesOutsideOneToEight() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
                () -> assertEquals(
                        "quickValue must be in range 1..8",
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> Subq.execute(Move.Size.WORD, 0, () -> 0, value -> {
                                }, conditionCodes)
                        ).getMessage()
                ),
                () -> assertEquals(
                        "quickValue must be in range 1..8",
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> Subq.execute(Move.Size.WORD, 9, () -> 0, value -> {
                                }, conditionCodes)
                        ).getMessage()
                )
        );
    }

    private static final class TrackingConditionCodes implements Subq.ConditionCodes {
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
