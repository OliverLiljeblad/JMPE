package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.instructions.data.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Addq_Test {
    @Test
    void executeSetsZeroCarryAndExtendOnUnsignedWrap() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Addq.execute(Move.Size.BYTE, 1, () -> 0x00FF, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Addq.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry),
                () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeSetsOverflowForSignedPositiveOverflow() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Addq.execute(Move.Size.BYTE, 1, () -> 0x007F, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x0080, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
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
                                () -> Addq.execute(Move.Size.WORD, 0, () -> 0, value -> {
                                }, conditionCodes)
                        ).getMessage()
                ),
                () -> assertEquals(
                        "quickValue must be in range 1..8",
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> Addq.execute(Move.Size.WORD, 9, () -> 0, value -> {
                                }, conditionCodes)
                        ).getMessage()
                )
        );
    }

    private static final class TrackingConditionCodes implements Addq.ConditionCodes {
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
