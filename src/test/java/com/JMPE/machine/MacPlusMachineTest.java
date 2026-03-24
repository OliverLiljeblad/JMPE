package com.JMPE.machine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.Rom;
import org.junit.jupiter.api.Test;

class MacPlusMachineTest {
    @Test
    void buildsMachineFromRomBytes() {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(new byte[] {
            0x00, 0x00, 0x20, 0x00,
            0x00, 0x40, 0x01, 0x00
        }, 0x0040_0000);

        assertNotNull(machine.cpu());
        assertNotNull(machine.bus());
        assertNotNull(machine.cpu().statusRegister());
        assertEquals(0x0040_0000, machine.rom().baseAddress());
        assertEquals(0x0040_0100, machine.bus().readLong(0x0040_0004));
        assertEquals(0x0000_2000, machine.cpu().registers().stackPointer());
        assertEquals(0x0040_0100, machine.cpu().registers().programCounter());
        assertTrue(machine.cpu().statusRegister().isSupervisorSet());
        assertEquals(7, machine.cpu().statusRegister().interruptMask());
    }

    @Test
    void keepsProvidedRomReference() {
        Rom rom = new Rom(0x0, new byte[] {0, 0, 32, 0, 0, 0, 0, 8});
        MacPlusMachine machine = new MacPlusMachine(rom);

        assertSame(rom, machine.rom());
        assertNotNull(machine.bus());
        assertEquals(0x0000_2000, machine.cpu().registers().stackPointer());
        assertEquals(0x0000_0008, machine.cpu().registers().programCounter());
    }
}
