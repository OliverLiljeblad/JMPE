package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Bcc_Test {
    @Test
    void executeWritesProgramCounterWhenConditionIsTrue() {
        AtomicInteger newPc = new AtomicInteger(Integer.MIN_VALUE);

        int cycles = Bcc.execute(
                Bcc.Condition.EQ,
                0x1000,
                0x20,
                new TrackingConditionCodesReader(false, true, false, false),
                newPc::set
        );

        assertAll(
                () -> assertEquals(Bcc.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0x1020, newPc.get())
        );
    }

    @Test
    void executeLeavesProgramCounterUntouchedWhenConditionIsFalse() {
        AtomicBoolean wrotePc = new AtomicBoolean(false);

        int cycles = Bcc.execute(
                Bcc.Condition.EQ,
                0x1000,
                0x20,
                new TrackingConditionCodesReader(false, false, false, false),
                value -> wrotePc.set(true)
        );

        assertAll(
                () -> assertEquals(Bcc.EXECUTION_CYCLES, cycles),
                () -> assertFalse(wrotePc.get())
        );
    }

    @Test
    void isConditionTrueEvaluatesRepresentativeUnsignedAndSignedCases() {
        assertAll(
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.HI, new TrackingConditionCodesReader(false, false, false, false))),
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.LS, new TrackingConditionCodesReader(false, false, false, true))),
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.GE, new TrackingConditionCodesReader(true, false, true, false))),
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.LT, new TrackingConditionCodesReader(true, false, false, false))),
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.GT, new TrackingConditionCodesReader(false, false, false, false))),
                () -> assertTrue(Bcc.isConditionTrue(Bcc.Condition.LE, new TrackingConditionCodesReader(false, true, false, false))),
                () -> assertFalse(Bcc.isConditionTrue(Bcc.Condition.CC, new TrackingConditionCodesReader(false, false, false, true)))
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
