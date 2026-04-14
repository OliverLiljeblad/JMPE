package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.DivideByZeroException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Divu_Test {
    @Test
    void executeStoresRemainderHighWordAndQuotientLowWord() {
        AtomicInteger writtenValue = new AtomicInteger(0xDEAD_BEEF);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        int cycles = Divu.execute(Size.WORD, () -> 3, () -> 20, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(Divu.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0002_0006, writtenValue.get()),
            () -> assertFalse(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertFalse(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeSetsOverflowAndPreservesDestinationWhenQuotientDoesNotFitWord() {
        AtomicInteger writtenValue = new AtomicInteger(0xDEAD_BEEF);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();
        conditionCodes.negative = true;
        conditionCodes.zero = false;
        conditionCodes.carry = true;

        Divu.execute(Size.WORD, () -> 1, () -> 0x0001_0000, writtenValue::set, conditionCodes);

        assertAll(
            () -> assertEquals(0xDEAD_BEEF, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertFalse(conditionCodes.zero),
            () -> assertTrue(conditionCodes.overflow),
            () -> assertFalse(conditionCodes.carry)
        );
    }

    @Test
    void executeThrowsDivideByZeroWithoutWritingDestinationOrFlags() {
        AtomicInteger writtenValue = new AtomicInteger(0xDEAD_BEEF);
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();
        conditionCodes.negative = true;
        conditionCodes.zero = true;
        conditionCodes.overflow = true;
        conditionCodes.carry = true;

        DivideByZeroException thrown = assertThrows(
            DivideByZeroException.class,
            () -> Divu.execute(Size.WORD, () -> 0, () -> 20, writtenValue::set, conditionCodes)
        );

        assertAll(
            () -> assertEquals(DivideByZeroException.VECTOR, thrown.vector()),
            () -> assertEquals("Integer divide by zero triggered exception vector 5", thrown.getMessage()),
            () -> assertEquals(0xDEAD_BEEF, writtenValue.get()),
            () -> assertTrue(conditionCodes.negative),
            () -> assertTrue(conditionCodes.zero),
            () -> assertTrue(conditionCodes.overflow),
            () -> assertTrue(conditionCodes.carry)
        );
    }

    @Test
    void executeRejectsInvalidInputs() {
        TrackingConditionCodes conditionCodes = new TrackingConditionCodes();

        assertAll(
            () -> assertEquals(
                "DIVU on the Macintosh Plus 68000 supports only WORD size",
                assertThrows(IllegalArgumentException.class,
                    () -> Divu.execute(Size.LONG, () -> 0, () -> 0, value -> { }, conditionCodes)
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Divu.execute(null, () -> 0, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Divu.execute(Size.WORD, null, () -> 0, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Divu.execute(Size.WORD, () -> 0, null, value -> { }, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Divu.execute(Size.WORD, () -> 0, () -> 0, null, conditionCodes)),
            () -> assertThrows(NullPointerException.class, () -> Divu.execute(Size.WORD, () -> 0, () -> 0, value -> { }, null))
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
