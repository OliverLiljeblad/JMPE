package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mulu_Test {
    @Test
    void executeMultipliesUnsignedLowWordsAndUpdatesFlags() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Mulu.execute(Size.WORD, () -> 0xFFFF_0003, () -> 0xAAAA_0004, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Mulu.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_000C, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsNegativeWhenProductUsesTopBit() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Mulu.execute(Size.WORD, () -> 0x0000_FFFF, () -> 0x1234_FFFF, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0xFFFE_0001, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsInvalidInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "MULU on the Macintosh Plus 68000 supports only WORD size",
                assertThrows(IllegalArgumentException.class,
                    () -> Mulu.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, conditionCodes)
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Mulu.execute(null, () -> 0, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Mulu.execute(Size.WORD, null, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Mulu.execute(Size.WORD, () -> 0, null, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Mulu.execute(Size.WORD, () -> 0, () -> 0, null, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Mulu.execute(Size.WORD, () -> 0, () -> 0, value -> { }, null))
        );
    }

    private static final class TrackingConditionCodes implements com.JMPE.cpu.m68k.instructions.ConditionCodes {
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
