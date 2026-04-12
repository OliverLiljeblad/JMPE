package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import org.junit.jupiter.api.Test;

class M68kCpuPhaseThreeStepTest {
    @Test
    void stepExecutesOrWordDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x0F0F);
        cpu.registers().setData(1, 0x00FF);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x8240), new DispatchTable());

        assertEquals(0x0FFF, cpu.registers().data(1));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertFalse(cpu.statusRegister().isZeroSet());
    }

    @Test
    void stepExecutesAndWordDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x0F0F);
        cpu.registers().setData(1, 0x00FF);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xC240), new DispatchTable());

        assertEquals(0x000F, cpu.registers().data(1));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertFalse(cpu.statusRegister().isZeroSet());
    }

    @Test
    void stepExecutesSubByteDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 1);
        cpu.registers().setData(1, 0);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x9200), new DispatchTable());

        assertEquals(0x0000_00FF, cpu.registers().data(1));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void stepExecutesAddWordDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 1);
        cpu.registers().setData(1, 0x7FFF);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xD240), new DispatchTable());

        assertEquals(0x0000_8000, cpu.registers().data(1));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
    }

    @Test
    void stepExecutesCmpWordDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x1234);
        cpu.registers().setData(1, 0x1234);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xB240), new DispatchTable());

        assertEquals(0x1234, cpu.registers().data(1));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isNegativeSet());
    }

    @Test
    void stepExecutesEorWordDataRegisterToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x00FF);
        cpu.registers().setData(1, 0x00F0);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xB340), new DispatchTable());

        assertEquals(0x000F, cpu.registers().data(0));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertFalse(cpu.statusRegister().isZeroSet());
    }

    @Test
    void stepExecutesAslByteImmediate() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x80);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xE300), new DispatchTable());

        assertEquals(0x0000_0000, cpu.registers().data(0) & 0xFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void stepExecutesAsrByteWithRegisterCount() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(1, 1);
        cpu.registers().setData(2, 0x80);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xE222), new DispatchTable());

        assertEquals(0x0000_00C0, cpu.registers().data(2) & 0xFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isCarrySet());
    }

    @Test
    void stepExecutesLslWordWithRegisterCount() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(4, 0x4000);
        cpu.registers().setData(5, 1);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xEB6C), new DispatchTable());

        assertEquals(0x0000_8000, cpu.registers().data(4) & 0xFFFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isCarrySet());
    }

    @Test
    void stepExecutesLsrByteImmediate() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(3, 0x01);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xE20B), new DispatchTable());

        assertEquals(0x0000_0000, cpu.registers().data(3) & 0xFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void stepExecutesRolByteImmediate() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(3, 0x80);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xE31B), new DispatchTable());

        assertEquals(0x0000_0001, cpu.registers().data(3) & 0xFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isCarrySet());
    }

    @Test
    void stepExecutesRorByteImmediate() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(3, 0x01);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0xE21B), new DispatchTable());

        assertEquals(0x0000_0080, cpu.registers().data(3) & 0xFF);
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isCarrySet());
    }

    private static AddressSpace busWithWords(int baseAddress, int... words) {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 0x2000));
        for (int index = 0; index < words.length; index++) {
            bus.writeWord(baseAddress + (index * 2), words[index]);
        }
        return bus;
    }
}
