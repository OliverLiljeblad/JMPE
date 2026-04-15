package com.JMPE.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MmioTest {
    @Test
    void composesBigEndianWordAndLongAccessesFromByteCallbacks() {
        int[] bytes = new int[4];
        Mmio mmio = Mmio.readWrite(
            0x1000,
            bytes.length,
            offset -> bytes[offset],
            (offset, value) -> bytes[offset] = value
        );

        mmio.writeLong(0, 0x1234_5678);

        assertEquals(0x12, mmio.readByte(0));
        assertEquals(0x1234, mmio.readWord(0));
        assertEquals(0x1234_5678, mmio.readLong(0));
    }

    @Test
    void openBusReadsZeroAndIgnoresWrites() {
        Mmio mmio = Mmio.openBus(0x2000, 8);

        mmio.writeLong(0, 0xDEAD_BEEF);

        assertEquals(0, mmio.readByte(0));
        assertEquals(0, mmio.readWord(0));
        assertEquals(0, mmio.readLong(0));
    }
}
