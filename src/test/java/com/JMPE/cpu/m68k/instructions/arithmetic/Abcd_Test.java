package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Abcd_Test {
    @Test
    void executeAddsPackedBcdDigitsAndSetsDecimalCarry() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Abcd.execute(Size.BYTE, () -> 0x55, () -> 0x45, writtenValue::set, false, conditionCodes);

        assertAll(
            () -> assertEquals(Abcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x00, writtenValue.get()),
            () -> assertFalse(conditionCodes.zeroCleared),
            () -> assertTrue(conditionCodes.carry),
            () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeClearsStickyZeroWhenResultIsNonZero() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        Abcd.execute(Size.BYTE, () -> 0x01, () -> 0x02, writtenValue::set, false, conditionCodes);

        assertAll(
            () -> assertEquals(0x03, writtenValue.get()),
            () -> assertTrue(conditionCodes.zeroCleared),
            () -> assertFalse(conditionCodes.carry),
            () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeRejectsInvalidInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "ABCD on the Macintosh Plus 68000 supports only BYTE size",
                assertThrows(IllegalArgumentException.class,
                    () -> Abcd.execute(Size.WORD, () -> 0, () -> 0, value -> { }, false, conditionCodes)
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Abcd.execute(null, () -> 0, () -> 0, value -> { }, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Abcd.execute(Size.BYTE, null, () -> 0, value -> { }, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Abcd.execute(Size.BYTE, () -> 0, null, value -> { }, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Abcd.execute(Size.BYTE, () -> 0, () -> 0, null, false, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Abcd.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, false, null))
        );
    }

    private static final class TrackingConditionCodes implements Abcd.ConditionCodes {
        private boolean zeroCleared;
        private boolean carry;
        private boolean extend;

        @Override
        public void clearZero() {
            zeroCleared = true;
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
