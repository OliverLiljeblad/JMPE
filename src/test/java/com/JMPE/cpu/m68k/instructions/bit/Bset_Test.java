package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bset_Test {
    @Test
    void executeWrapsRegisterBitNumberModulo32AndWritesBackResult() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        int cycles = Bset.execute(Size.LONG, () -> 33, () -> 0x0000_0000, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(Bset.EXECUTION_CYCLES, cycles),
            () -> assertTrue(zeroFlag.zero),
            () -> assertEquals(0x0000_0002, writtenValue.get())
        );
    }

    @Test
    void executeWrapsMemoryBitNumberModulo8() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bset.execute(Size.BYTE, () -> 9, () -> 0x00, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertTrue(zeroFlag.zero),
            () -> assertEquals(0x02, writtenValue.get())
        );
    }

    @Test
    void executeClearsZeroWhenBitWasAlreadySet() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bset.execute(Size.BYTE, () -> 1, () -> 0x02, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(0x02, writtenValue.get()),
            () -> assertFalse(zeroFlag.zero)
        );
    }

    @Test
    void executeRejectsNullInputsAndUnsupportedSize() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        assertThrows(NullPointerException.class, () -> Bset.execute(null, () -> 0, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bset.execute(Size.BYTE, null, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bset.execute(Size.BYTE, () -> 0, null, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bset.execute(Size.BYTE, () -> 0, () -> 0, null, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bset.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, null));
        assertThrows(IllegalArgumentException.class, () -> Bset.execute(Size.WORD, () -> 0, () -> 0, value -> { }, zeroFlag));
    }

    private static final class TrackingZeroFlag implements Bset.ZeroFlag {
        private boolean zero;

        @Override
        public void setZero(boolean value) {
            zero = value;
        }
    }
}
