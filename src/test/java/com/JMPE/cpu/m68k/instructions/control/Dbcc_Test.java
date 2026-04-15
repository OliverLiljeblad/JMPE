package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dbcc_Test {
    @Test
    void executeLeavesCounterAndPcUntouchedWhenConditionIsTrue() {
        AtomicInteger counter = new AtomicInteger(0x1234_0002);
        AtomicBoolean wrotePc = new AtomicBoolean(false);

        int cycles = Dbcc.execute(
            0x0,
            0x1002,
            -4,
            counter::get,
            counter::set,
            new TrackingConditionCodesReader(false, false, false, false),
            value -> wrotePc.set(true)
        );

        assertAll(
            () -> assertEquals(Dbcc.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_0002, counter.get()),
            () -> assertFalse(wrotePc.get())
        );
    }

    @Test
    void executeDecrementsLowWordAndBranchesWhenConditionIsFalseAndCounterDoesNotUnderflow() {
        AtomicInteger counter = new AtomicInteger(0x1234_0001);
        AtomicInteger newPc = new AtomicInteger(Integer.MIN_VALUE);

        int cycles = Dbcc.execute(
            0x1,
            0x1002,
            -4,
            counter::get,
            counter::set,
            new TrackingConditionCodesReader(false, false, false, false),
            newPc::set
        );

        assertAll(
            () -> assertEquals(Dbcc.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_0000, counter.get()),
            () -> assertEquals(0x0FFE, newPc.get())
        );
    }

    @Test
    void executeFallsThroughWhenCounterUnderflowsToMinusOne() {
        AtomicInteger counter = new AtomicInteger(0x1234_0000);
        AtomicBoolean wrotePc = new AtomicBoolean(false);

        Dbcc.execute(
            0x1,
            0x1002,
            -4,
            counter::get,
            counter::set,
            new TrackingConditionCodesReader(false, false, false, false),
            value -> wrotePc.set(true)
        );

        assertAll(
            () -> assertEquals(0x1234_FFFF, counter.get()),
            () -> assertFalse(wrotePc.get())
        );
    }

    @Test
    void executeRejectsInvalidConditionsAndNullCollaborators() {
        TrackingConditionCodesReader conditionCodesReader = new TrackingConditionCodesReader(false, false, false, false);

        assertThrows(IllegalArgumentException.class, () -> Dbcc.execute(0x10, 0, 0, () -> 0, value -> { }, conditionCodesReader, value -> { }));
        assertThrows(NullPointerException.class, () -> Dbcc.execute(0x1, 0, 0, null, value -> { }, conditionCodesReader, value -> { }));
        assertThrows(NullPointerException.class, () -> Dbcc.execute(0x1, 0, 0, () -> 0, null, conditionCodesReader, value -> { }));
        assertThrows(NullPointerException.class, () -> Dbcc.execute(0x1, 0, 0, () -> 0, value -> { }, null, value -> { }));
        assertThrows(NullPointerException.class, () -> Dbcc.execute(0x1, 0, 0, () -> 0, value -> { }, conditionCodesReader, null));
    }

    @Test
    void isConditionTrueDelegatesToSharedConditionEvaluationForConditionalForms() {
        assertAll(
            () -> assertTrue(Dbcc.isConditionTrue(0x0, new TrackingConditionCodesReader(false, false, false, false))),
            () -> assertFalse(Dbcc.isConditionTrue(0x1, new TrackingConditionCodesReader(false, false, false, false))),
            () -> assertTrue(Dbcc.isConditionTrue(0x6, new TrackingConditionCodesReader(false, false, false, false))),
            () -> assertFalse(Dbcc.isConditionTrue(0x7, new TrackingConditionCodesReader(false, false, false, false)))
        );
    }

    private record TrackingConditionCodesReader(boolean negative, boolean zero, boolean overflow, boolean carry)
        implements Bcc.ConditionCodesReader {
        @Override
        public boolean isNegative() {
            return negative;
        }

        @Override
        public boolean isZero() {
            return zero;
        }

        @Override
        public boolean isOverflow() {
            return overflow;
        }

        @Override
        public boolean isCarry() {
            return carry;
        }
    }
}
