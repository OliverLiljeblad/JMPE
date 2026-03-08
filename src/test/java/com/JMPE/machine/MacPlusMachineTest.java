package com.JMPE.machine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.bus.Rom;
import org.junit.jupiter.api.Test;

class MacPlusMachineTest {
    @Test
    void buildsMachineFromRomBytes() {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(new byte[] {
            0x00, 0x10, 0x00, 0x20, 0x12, 0x34, 0x56, 0x78
        }, 0x0040_0000);

        assertNotNull(machine.cpu());
        assertNotNull(machine.cpu().statusRegister());
        assertEquals(0x0040_0000, machine.rom().baseAddress());
    }

    @Test
    void keepsProvidedRomReference() {
        Rom rom = new Rom(0x0, new byte[] {0, 1, 2, 3, 4, 5, 6, 7});
        MacPlusMachine machine = new MacPlusMachine(rom);

        assertSame(rom, machine.rom());
    }
}
