package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bclr_Test {
    @Test
    void executeWrapsRegisterBitNumberModulo32AndClearsBit() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        int cycles = Bclr.execute(Size.LONG, () -> 33, () -> 0x0000_0003, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(Bclr.EXECUTION_CYCLES, cycles),
            () -> assertFalse(zeroFlag.zero),
            () -> assertEquals(0x0000_0001, writtenValue.get())
        );
    }

    @Test
    void executeWrapsMemoryBitNumberModulo8() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bclr.execute(Size.BYTE, () -> 9, () -> 0x03, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertFalse(zeroFlag.zero),
            () -> assertEquals(0x01, writtenValue.get())
        );
    }

    @Test
    void executeSetsZeroWhenBitWasAlreadyClear() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bclr.execute(Size.BYTE, () -> 0, () -> 0x02, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(0x02, writtenValue.get()),
            () -> assertTrue(zeroFlag.zero)
        );
    }

    @Test
    void executeRejectsNullInputsAndUnsupportedSize() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        assertThrows(NullPointerException.class, () -> Bclr.execute(null, () -> 0, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bclr.execute(Size.BYTE, null, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bclr.execute(Size.BYTE, () -> 0, null, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bclr.execute(Size.BYTE, () -> 0, () -> 0, null, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bclr.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, null));
        assertThrows(IllegalArgumentException.class, () -> Bclr.execute(Size.WORD, () -> 0, () -> 0, value -> { }, zeroFlag));
    }

    private static final class TrackingZeroFlag implements Bclr.ZeroFlag {
        private boolean zero;

        @Override
        public void setZero(boolean value) {
            zero = value;
        }
    }
}
