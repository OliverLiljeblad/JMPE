package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Muls_Test {
    @Test
    void executeSignExtendsLowWordsAndWritesSignedProduct() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Muls.execute(Size.WORD, () -> 0x0000_FFFE, () -> 0xAAAA_0003, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Muls.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFFFF_FFFA, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsZeroForZeroProduct() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Muls.execute(Size.WORD, () -> 0xFFFF_0000, () -> 0x1234_0005, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0x0000_0000, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsInvalidInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "MULS on the Macintosh Plus 68000 supports only WORD size",
                assertThrows(IllegalArgumentException.class,
                    () -> Muls.execute(Size.LONG, () -> 0, () -> 0, value -> { }, conditionCodes)
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Muls.execute(null, () -> 0, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Muls.execute(Size.WORD, null, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Muls.execute(Size.WORD, () -> 0, null, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Muls.execute(Size.WORD, () -> 0, () -> 0, null, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Muls.execute(Size.WORD, () -> 0, () -> 0, value -> { }, null))
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
