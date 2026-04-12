package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataRegisterWriterTest {
    @Test
    void writesByteWithoutTouchingUpperBits() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5678);

        DataRegisterWriter.write(cpu, 0, Size.BYTE, 0x00AB);

        assertEquals(0x1234_56AB, cpu.registers().data(0));
    }

    @Test
    void writesWordWithoutTouchingUpperBits() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5678);

        DataRegisterWriter.write(cpu, 0, Size.WORD, 0x0000_ABCD);

        assertEquals(0x1234_ABCD, cpu.registers().data(0));
    }

    @Test
    void writesLongAcrossWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5678);

        DataRegisterWriter.write(cpu, 0, Size.LONG, 0x89AB_CDEF);

        assertEquals(0x89AB_CDEF, cpu.registers().data(0));
    }

    @Test
    void rejectsUnsizedWrites() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> DataRegisterWriter.write(cpu, 0, Size.UNSIZED, 0));
    }
}
