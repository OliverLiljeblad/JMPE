package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.data.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Asr_Test {
    @Test
    void executePreservesSignBitForNegativeByteValue() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // 0x80 sign-extended is -128; right-shifted by 1 gives -64 (0xC0). Sign is preserved.
        int cycles = Asr.execute(Size.BYTE, 1, () -> 0x80, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(Asr.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0xC0, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry),
                () -> assertFalse(conditionCodes.extend)
        );
    }

    @Test
    void executeSetsCarryAndExtendFromLeastSignificantBit() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // Shifting 0x01 right by 1: LSB becomes carry; result is 0.
        Asr.execute(Size.BYTE, 1, () -> 0x01, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertTrue(conditionCodes.carry),
                () -> assertTrue(conditionCodes.extend)
        );
    }

    @Test
    void executeWithZeroCountLeavesResultAndDoesNotSetExtend() {
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        // count=0: no shift; extend is never written; flags reflect the original value.
        Asr.execute(Size.BYTE, 0, () -> 0x42, writtenValue::set, conditionCodes);

        assertAll(
                () -> assertEquals(0x42, writtenValue.get()),
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
                () -> Asr.execute(Size.BYTE, -1, () -> 0, value -> {}, conditionCodes)
        );

        assertEquals("count must be >= 0", exception.getMessage());
    }

    private static final class TrackingConditionCodes implements Asr.ConditionCodes {
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
