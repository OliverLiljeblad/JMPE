package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.Rom;
import org.junit.jupiter.api.Test;

class M68kCpuTest {
    @Test
    void resetFromRomLoadsBootstrapVectorsAndSupervisorState() {
        M68kCpu cpu = new M68kCpu();
        Rom rom = new Rom(0x0040_0000, new byte[] {
            0x00, 0x00, 0x20, 0x00, // initial SSP: 0x00002000
            0x00, 0x40, 0x01, 0x00  // initial PC:  0x00400100
        });

        cpu.resetFromRom(rom);

        assertEquals(0x0000_2000, cpu.registers().stackPointer());
        assertEquals(0x0040_0100, cpu.registers().programCounter());
        assertTrue(cpu.statusRegister().isSupervisorSet());
        assertEquals(7, cpu.statusRegister().interruptMask());
    }

    @Test
    void rejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new M68kCpu(null, new StatusRegister()));
        assertThrows(IllegalArgumentException.class, () -> new M68kCpu(new Registers(), null));

        M68kCpu cpu = new M68kCpu();
        assertThrows(IllegalArgumentException.class, () -> cpu.resetFromRom(null));
    }
}
