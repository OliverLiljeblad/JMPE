package com.JMPE.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OverlayMemoryRegionTest {
    @Test
    void readsRomUntilOverlayIsClearedThenExposesBackingMemory() {
        Rom rom = new Rom(0x0040_0000, new byte[] {
            0x00, 0x00, 0x20, 0x00,
            0x00, 0x40, 0x01, 0x00
        });
        Ram ram = new Ram(0x0000_0000, 0x2000);
        OverlayMemoryRegion region = new OverlayMemoryRegion(0x0000_0000, 0x0040_0000, rom, ram, true);

        region.writeLong(0x0004, 0xDEAD_BEEF);

        assertTrue(region.isOverlayEnabled());
        assertEquals(0x0040_0100, region.readLong(0x0004));

        region.setOverlayEnabled(false);
        region.writeLong(0x0004, 0xDEAD_BEEF);

        assertEquals(0xDEAD_BEEF, region.readLong(0x0004));
    }
}
