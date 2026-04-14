package com.JMPE.machine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.Ram;
import com.JMPE.bus.Rom;
import com.JMPE.cpu.m68k.M68kCpu;
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

    @Test
    void supportsAdditionalMemoryRegionsForBootScaffolding() {
        MacPlusMachine machine = new MacPlusMachine(
            new Rom(0x0040_0000, new byte[] {
                0x00, 0x00, 0x20, 0x00,
                0x00, 0x40, 0x01, 0x00
            }),
            new M68kCpu(),
            new Ram(0x0000_0000, 0x2000)
        );

        machine.bus().writeLong(0x0000_0100, 0x1234_5678);

        assertEquals(0x1234_5678, machine.bus().readLong(0x0000_0100));
        assertEquals(0x0040_0100, machine.cpu().registers().programCounter());
        assertEquals(0x0040_0100, machine.bus().readLong(0x0040_0004));
    }

    @Test
    void bootMachineStartsWithOverlayAndMapsEarlyProbeWindows() {
        Rom rom = new Rom(0x0040_0000, new byte[] {
            0x00, 0x00, 0x20, 0x00,
            0x00, 0x40, 0x01, 0x00
        });
        Ram ram = new Ram(0x0000_0000, 0x2000);
        MacPlusMachine machine = MacPlusMachine.bootMachine(rom, new M68kCpu(), ram);

        machine.bus().writeLong(0x0000_0004, 0xDEAD_BEEF);

        assertEquals(0x0040_0100, machine.bus().readLong(0x0000_0004));
        assertEquals(0, machine.bus().readLong(0x00F8_0000));
        assertEquals(0, machine.bus().readByte(0x009F_FFF7));

        machine.bus().writeByte(0x00E8_0600, 0x10);
        machine.bus().writeByte(0x00E8_0200, 0x00);
        machine.bus().writeLong(0x0000_0004, 0xDEAD_BEEF);

        assertEquals(0xDEAD_BEEF, machine.bus().readLong(0x0000_0004));
    }
}
