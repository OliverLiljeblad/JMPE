package com.JMPE.cpu.m68k.instructions.data;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoveQ_Test {
    @Test
    void executeWritesSignExtendedNegativeLongAndUpdatesCcr() {
        AtomicInteger registerIndex = new AtomicInteger(-1);
        AtomicInteger writtenValue = new AtomicInteger();
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = MoveQ.execute(3, 0x80, writeLong(registerIndex, writtenValue), conditionCodes);

        assertAll(
                () -> assertEquals(MoveQ.EXECUTION_CYCLES, cycles),
                () -> assertEquals(3, registerIndex.get()),
                () -> assertEquals(-128, writtenValue.get()),
                () -> assertTrue(conditionCodes.negative),
                () -> assertFalse(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry),
                () -> assertTrue(conditionCodes.externalXState)
        );
    }

    @Test
    void executeSetsZeroForZeroImmediate() {
        AtomicInteger writtenValue = new AtomicInteger(Integer.MIN_VALUE);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        MoveQ.execute(0, 0x00, (register, value) -> writtenValue.set(value), conditionCodes);

        assertAll(
                () -> assertEquals(0, writtenValue.get()),
                () -> assertFalse(conditionCodes.negative),
                () -> assertTrue(conditionCodes.zero),
                () -> assertFalse(conditionCodes.overflow),
                () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void signExtend8UsesOnlyLowByte() {
        assertAll(
                () -> assertEquals(-1, MoveQ.signExtend8(0x01FF)),
                () -> assertEquals(127, MoveQ.signExtend8(0x007F))
        );
    }

    @Test
    void executeRejectsRegistersOutsideDataRegisterRange() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
                () -> assertEquals(
                        "destinationRegister must be in range 0..7",
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> MoveQ.execute(-1, 0x12, (register, value) -> {
                                }, conditionCodes)
                        ).getMessage()
                ),
                () -> assertEquals(
                        "destinationRegister must be in range 0..7",
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> MoveQ.execute(8, 0x12, (register, value) -> {
                                }, conditionCodes)
                        ).getMessage()
                )
        );
    }

    private MoveQ.DataRegisterWriter writeLong(AtomicInteger registerIndex, AtomicInteger writtenValue) {
        return (register, value) -> {
            registerIndex.set(register);
            writtenValue.set(value);
        };
    }

    private static final class TrackingConditionCodes implements Move.ConditionCodes {
        private boolean negative;
        private boolean zero;
        private boolean overflow;
        private boolean carry;
        private boolean externalXState = true;

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
