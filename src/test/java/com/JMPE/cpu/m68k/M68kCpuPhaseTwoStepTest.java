package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import org.junit.jupiter.api.Test;

class M68kCpuPhaseTwoStepTest {
    @Test
    void stepExecutesMoveqAndSignExtendsImmediate() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x70FF), new DispatchTable());

        assertEquals(0xFFFF_FFFF, cpu.registers().data(0));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
    }

    @Test
    void stepExecutesAddqToAddressRegisterWithoutChangingFlags() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setAddress(0, 0x2000);
        cpu.statusRegister().setRawValue(0x001F);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x5248), new DispatchTable());

        assertEquals(0x2001, cpu.registers().address(0));
        assertEquals(0x001F, cpu.statusRegister().rawValue());
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
    }

    @Test
    void stepExecutesSubqLongToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 10);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x5180), new DispatchTable());

        assertEquals(2, cpu.registers().data(0));
        assertEquals(0x1002, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(4, report.cycles());
    }

    @Test
    void stepExecutesBraByte() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x6004), new DispatchTable());

        assertEquals(0x1006, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(10, report.cycles());
    }

    @Test
    void stepExecutesBsrWordAndPushesReturnAddress() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setStackPointer(0x1100);
        AddressSpace bus = busWithWords(0x1000, 0x6100, 0x0004);

        M68kCpu.StepReport report = cpu.step(bus, new DispatchTable());

        assertEquals(0x1006, cpu.registers().programCounter());
        assertEquals(0x10FC, cpu.registers().stackPointer());
        assertEquals(0x0000_1004, bus.readLong(0x10FC));
        assertTrue(report.success());
        assertEquals(18, report.cycles());
    }

    @Test
    void stepExecutesBneByteWhenConditionIsTrue() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.statusRegister().setZero(false);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x6602), new DispatchTable());

        assertEquals(0x1004, cpu.registers().programCounter());
        assertTrue(report.success());
        assertEquals(10, report.cycles());
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
