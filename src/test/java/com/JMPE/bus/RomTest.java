package com.JMPE.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RomTest {
    @Test
    void readsBigEndianValuesFromMappedAddresses() {
        Rom rom = new Rom(0x0040_0000, new byte[] {
            (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78
        });

        assertEquals(0x12, rom.readByte(0x0040_0000));
        assertEquals(0x1234, rom.readWord(0x0040_0000));
        assertEquals(0x1234_5678L, rom.readLong(0x0040_0000));
        assertTrue(rom.contains(0x0040_0001));
    }

    @Test
    void rejectsOutOfBoundsReadAndAllWrites() {
        Rom rom = new Rom(0x0000_0000, new byte[] {
            (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD
        });

        assertThrows(IndexOutOfBoundsException.class, () -> rom.readWord(0x0000_0003));
        assertThrows(UnsupportedOperationException.class, () -> rom.writeByte(0x0000_0000, 0xFF));
    }

    @Test
    void decodesResetVectors() {
        Rom rom = new Rom(0x0040_0000, new byte[] {
            0x00, 0x00, 0x20, 0x00,
            0x00, 0x40, 0x01, 0x00
        });

        assertEquals(0x0000_2000, rom.initialSupervisorStackPointer());
        assertEquals(0x0040_0100, rom.initialProgramCounter());
    }

    @Test
    void rejectsResetVectorReadWhenRomIsTooSmall() {
        Rom rom = new Rom(0x0000_0000, new byte[] {
            (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD
        });

        assertThrows(IllegalStateException.class, rom::initialSupervisorStackPointer);
        assertThrows(IllegalStateException.class, rom::initialProgramCounter);
    }
}
