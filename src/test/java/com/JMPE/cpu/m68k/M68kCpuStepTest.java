package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.dispatch.Op;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class M68kCpuStepTest {
    private static final int TST_B_D0 = 0x4A00;
    private static final int TST_NEGATIVE_BYTE = 0x0000_0080;

    @Test
    void stepFetchesDecodesDispatchesAndAdvancesPcForNop() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x1234_5678);
        cpu.statusRegister().setCarry(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, 0x4E71), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x1234_5678, report.after().dataRegister(0));
        assertEquals(0x0000_1002, cpu.registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NOP"));
        assertTrue(logs.get(0).contains("cycles=4"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void traceNopPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        AddressSpace bus = busWithOpword(0x0000_1000, 0x4E71);
        DispatchTable dispatchTable = new DispatchTable();

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[nop-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[nop-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[nop-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[nop-trace] execute " + message)
        );

        assertTrue(report.success());
        assertEquals(0x0000_1002, cpu.registers().programCounter());
    }

    @Test
    void stepFetchesDecodesDispatchesAndUpdatesFlagsForTstDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureTstScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, TST_B_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(TST_NEGATIVE_BYTE, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=TST"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void traceTstDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureTstScenario(cpu);
        AddressSpace bus = busWithOpword(0x0000_1000, TST_B_D0);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[tst-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[tst-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[tst-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[tst-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[tst-trace] execute " + message)
        );

        System.out.printf("[tst-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue(),
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().isNegativeSet(),
            cpu.statusRegister().isZeroSet(),
            cpu.statusRegister().isOverflowSet(),
            cpu.statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(0x0000_1002, cpu.registers().programCounter());
    }

    @Test
    void stepLogsAndRethrowsIllegalInstructions() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        List<String> logs = new ArrayList<>();

        IllegalInstructionException thrown = assertThrows(
            IllegalInstructionException.class,
            () -> cpu.step(busWithOpword(0x0000_1000, 0x4E74), new DispatchTable(), logs::add)
        );

        assertTrue(thrown.getMessage().contains("does not correspond to a valid instruction opcode"));
        assertEquals(0x0000_1000, cpu.registers().programCounter());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=0x4E74"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001000"));
    }

    @Test
    void stepLogsAndRethrowsMissingHandlerFailures() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        List<String> logs = new ArrayList<>();

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> cpu.step(busWithOpword(0x0000_1000, 0x4E71), DispatchTable.empty(), logs::add)
        );

        assertEquals("No handler registered for opcode NOP", thrown.getMessage());
        assertEquals(0x0000_1002, cpu.registers().programCounter());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=NOP"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepRejectsNullInputs() {
        M68kCpu cpu = new M68kCpu();
        DispatchTable dispatchTable = new DispatchTable();
        AddressSpace bus = busWithOpword(0x0000_1000, 0x4E71);

        assertThrows(NullPointerException.class, () -> cpu.step(null, dispatchTable, ignored -> {
        }));
        assertThrows(NullPointerException.class, () -> cpu.step(bus, null, ignored -> {
        }));
        assertThrows(NullPointerException.class, () -> cpu.step(bus, dispatchTable, null));
    }

    private static AddressSpace busWithOpword(int baseAddress, int opword) {
        Ram ram = new Ram(baseAddress, 64);
        AddressSpace bus = new AddressSpace();
        bus.addRegion(ram);
        bus.writeWord(baseAddress, opword);
        return bus;
    }

    private static void configureTstScenario(M68kCpu cpu) {
        cpu.registers().setData(0, TST_NEGATIVE_BYTE);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);
    }
}
