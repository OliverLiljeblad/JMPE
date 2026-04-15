package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Btst_Test {
    @Test
    void executeWrapsRegisterBitNumberModulo32() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        int cycles = Btst.execute(Size.LONG, () -> 33, () -> 0x0000_0002, zeroFlag);

        assertAll(
            () -> assertEquals(Btst.EXECUTION_CYCLES, cycles),
            () -> assertFalse(zeroFlag.zero)
        );
    }

    @Test
    void executeWrapsMemoryBitNumberModulo8() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        Btst.execute(Size.BYTE, () -> 9, () -> 0x0000_0002, zeroFlag);

        assertFalse(zeroFlag.zero);
    }

    @Test
    void executeSetsZeroWhenBitIsClear() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        Btst.execute(Size.LONG, () -> 3, () -> 0x0000_0002, zeroFlag);

        assertTrue(zeroFlag.zero);
    }

    @Test
    void executeRejectsNullInputsAndUnsupportedSize() {
        TrackingZeroFlag zeroFlag = new TrackingZeroFlag();

        assertThrows(NullPointerException.class, () -> Btst.execute(null, () -> 0, () -> 0, zeroFlag));
        assertThrows(NullPointerException.class, () -> Btst.execute(Size.BYTE, null, () -> 0, zeroFlag));
        assertThrows(NullPointerException.class, () -> Btst.execute(Size.BYTE, () -> 0, null, zeroFlag));
        assertThrows(NullPointerException.class, () -> Btst.execute(Size.BYTE, () -> 0, () -> 0, null));
        assertThrows(IllegalArgumentException.class, () -> Btst.execute(Size.WORD, () -> 0, () -> 0, zeroFlag));
    }

    private static final class TrackingZeroFlag implements Btst.ZeroFlag {
        private boolean zero;

        @Override
        public void setZero(boolean value) {
            zero = value;
        }
    }
}
