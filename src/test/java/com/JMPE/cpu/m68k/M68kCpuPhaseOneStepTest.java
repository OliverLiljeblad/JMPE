package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class M68kCpuPhaseOneStepTest {
    @Test
    void stepExecutesMoveWordImmediateToDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x303C, 0x8001), new DispatchTable(), logs::add);

        assertEquals(0x0000_8001, cpu.registers().data(0));
        assertEquals(0x1004, cpu.registers().programCounter());
        assertTrue(report.success());
        assertTrue(logs.get(0).contains("op=MOVE"));
    }

    @Test
    void stepExecutesLeaAbsoluteLong() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);

        M68kCpu.StepReport report = cpu.step(busWithWords(0x1000, 0x41F9, 0x0000, 0x2000), new DispatchTable(), ignored -> {
        });

        assertEquals(0x0000_2000, cpu.registers().address(0));
        assertEquals(0x1006, cpu.registers().programCounter());
        assertTrue(report.success());
    }

    @Test
    void stepExecutesJsrAbsoluteLongAndPushesReturnAddress() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setStackPointer(0x1100);
        AddressSpace bus = busWithWords(0x1000, 0x4EB9, 0x0000, 0x2000);

        cpu.step(bus, new DispatchTable(), ignored -> {
        });

        assertEquals(0x0000_2000, cpu.registers().programCounter());
        assertEquals(0x10FC, cpu.registers().stackPointer());
        assertEquals(0x0000_1006, bus.readLong(0x10FC));
    }

    @Test
    void stepLogsAndRethrowsPrivilegeViolationForMoveToSrInUserMode() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x1000);
        cpu.registers().setData(0, 0x2700);
        cpu.statusRegister().setSupervisor(false);
        List<String> logs = new ArrayList<>();

        PrivilegeViolation thrown = assertThrows(
            PrivilegeViolation.class,
            () -> cpu.step(busWithWords(0x1000, 0x46C0), new DispatchTable(), logs::add)
        );

        assertEquals("MOVE to SR requires supervisor mode", thrown.getMessage());
        assertTrue(logs.get(0).contains("ERR op=MOVE_TO_SR"));
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
