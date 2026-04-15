package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bchg_Test {
    @Test
    void executeWrapsRegisterBitNumberModulo32AndTogglesBit() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        int cycles = Bchg.execute(Size.LONG, () -> 33, () -> 0x0000_0000, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(Bchg.EXECUTION_CYCLES, cycles),
            () -> assertTrue(zeroFlag.zero),
            () -> assertEquals(0x0000_0002, writtenValue.get())
        );
    }

    @Test
    void executeWrapsMemoryBitNumberModulo8() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bchg.execute(Size.BYTE, () -> 9, () -> 0x03, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertFalse(zeroFlag.zero),
            () -> assertEquals(0x01, writtenValue.get())
        );
    }

    @Test
    void executeClearsBitAndClearsZeroWhenBitWasAlreadySet() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Bchg.execute(Size.BYTE, () -> 1, () -> 0x02, writtenValue::set, zeroFlag);

        assertAll(
            () -> assertEquals(0x00, writtenValue.get()),
            () -> assertFalse(zeroFlag.zero)
        );
    }

    @Test
    void executeRejectsNullInputsAndUnsupportedSize() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        assertThrows(NullPointerException.class, () -> Bchg.execute(null, () -> 0, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bchg.execute(Size.BYTE, null, () -> 0, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bchg.execute(Size.BYTE, () -> 0, null, value -> { }, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bchg.execute(Size.BYTE, () -> 0, () -> 0, null, zeroFlag));
        assertThrows(NullPointerException.class, () -> Bchg.execute(Size.BYTE, () -> 0, () -> 0, value -> { }, null));
        assertThrows(IllegalArgumentException.class, () -> Bchg.execute(Size.WORD, () -> 0, () -> 0, value -> { }, zeroFlag));
    }

    private static final class TrackingZeroFlag implements Bchg.ZeroFlag {
        private boolean zero;

        @Override
        public void setZero(boolean value) {
            zero = value;
        }
    }
}
