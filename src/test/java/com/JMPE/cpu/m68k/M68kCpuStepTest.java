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
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class M68kCpuStepTest {
    private static final int CLR_B_D0 = 0x4200;
    private static final int CLR_BYTE_INITIAL = 0x1234_5678;
    private static final int CLR_BYTE_RESULT = 0x1234_5600;
    private static final int NOT_B_D0 = 0x4600;
    private static final int NOT_BYTE_INITIAL = 0x1234_5600;
    private static final int NOT_BYTE_RESULT = 0x1234_56FF;
    private static final int ANDI_B_D0 = 0x0200;
    private static final int ANDI_BYTE_IMMEDIATE = 0x0080;
    private static final int ANDI_BYTE_INITIAL = 0x1234_56FF;
    private static final int ANDI_BYTE_RESULT = 0x1234_5680;
    private static final int ANDI_TO_CCR = 0x023C;
    private static final int ANDI_TO_CCR_IMMEDIATE = 0x0015;
    private static final int ANDI_TO_CCR_INITIAL_SR = 0x251F;
    private static final int ANDI_TO_CCR_RESULT_SR = 0x2515;
    private static final int ANDI_TO_SR = 0x027C;
    private static final int ANDI_TO_SR_IMMEDIATE = 0x20F0;
    private static final int ANDI_TO_SR_INITIAL_SR = 0xA71F;
    private static final int ANDI_TO_SR_RESULT_SR = 0x2010;
    private static final int ANDI_TO_SR_USER_MODE_SR = 0x071F;
    private static final int ORI_B_D0 = 0x0000;
    private static final int ORI_BYTE_IMMEDIATE = 0x0080;
    private static final int ORI_BYTE_INITIAL = 0x1234_5600;
    private static final int ORI_BYTE_RESULT = 0x1234_5680;
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
    void stepFetchesDecodesDispatchesWritesDataRegisterAndUpdatesFlagsForClrDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureClrScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, CLR_B_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(CLR_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(CLR_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(CLR_BYTE_RESULT, cpu.registers().data(0));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CLR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void traceClrDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureClrScenario(cpu);
        AddressSpace bus = busWithOpword(0x0000_1000, CLR_B_D0);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[clr-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[clr-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[clr-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[clr-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[clr-trace] execute " + message)
        );

        System.out.printf("[clr-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
    void stepFetchesDecodesDispatchesWritesDataRegisterAndUpdatesFlagsForNotDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureNotScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, NOT_B_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(NOT_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(NOT_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(NOT_BYTE_RESULT, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NOT"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void traceNotDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureNotScenario(cpu);
        AddressSpace bus = busWithOpword(0x0000_1000, NOT_B_D0);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[not-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[not-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[not-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[not-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[not-trace] execute " + message)
        );

        System.out.printf("[not-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
    void stepFetchesDecodesDispatchesWritesDataRegisterAndUpdatesFlagsForAndiImmediateToDataRegister()
            throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureAndiScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, ANDI_B_D0, ANDI_BYTE_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(ANDI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(ANDI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(ANDI_BYTE_RESULT, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void traceAndiImmediateToDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureAndiScenario(cpu);
        AddressSpace bus = busWithWords(0x0000_1000, ANDI_B_D0, ANDI_BYTE_IMMEDIATE);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[andi-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[andi-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[andi-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[andi-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[andi-trace] execute " + message)
        );

        System.out.printf("[andi-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
        assertEquals(0x0000_1004, cpu.registers().programCounter());
    }

    @Test
    void stepFetchesDecodesDispatchesWritesConditionCodeRegisterForAndiToCcr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureAndiToCcrScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, ANDI_TO_CCR, ANDI_TO_CCR_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(ANDI_TO_CCR_INITIAL_SR, report.before().statusRegister());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(ANDI_TO_CCR_RESULT_SR, report.after().statusRegister());
        assertEquals(ANDI_TO_CCR_RESULT_SR, cpu.statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI_TO_CCR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepFetchesDecodesDispatchesWritesStatusRegisterForAndiToSr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureAndiToSrScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(ANDI_TO_SR_INITIAL_SR, report.before().statusRegister());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(ANDI_TO_SR_RESULT_SR, report.after().statusRegister());
        assertEquals(ANDI_TO_SR_RESULT_SR, cpu.statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI_TO_SR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepLogsAndRethrowsPrivilegeViolationForAndiToSrInUserMode() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureAndiToSrUserModeScenario(cpu);
        List<String> logs = new ArrayList<>();

        PrivilegeViolation thrown = assertThrows(
                PrivilegeViolation.class,
                () -> cpu.step(
                        busWithWords(0x0000_1000, ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE),
                        new DispatchTable(),
                        logs::add
                )
        );

        assertEquals("ANDI to SR requires supervisor mode", thrown.getMessage());
        assertEquals(0x0000_1004, cpu.registers().programCounter());
        assertEquals(ANDI_TO_SR_USER_MODE_SR, cpu.statusRegister().rawValue());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=ANDI_TO_SR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepFetchesDecodesDispatchesWritesDataRegisterAndUpdatesFlagsForOriImmediateToDataRegister()
            throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureOriScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, ORI_B_D0, ORI_BYTE_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(ORI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(ORI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(ORI_BYTE_RESULT, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ORI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void traceOriImmediateToDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureOriScenario(cpu);
        AddressSpace bus = busWithWords(0x0000_1000, ORI_B_D0, ORI_BYTE_IMMEDIATE);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[ori-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[ori-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[ori-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[ori-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[ori-trace] execute " + message)
        );

        System.out.printf("[ori-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
        assertEquals(0x0000_1004, cpu.registers().programCounter());
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
        return busWithWords(baseAddress, opword);
    }

    private static AddressSpace busWithWords(int baseAddress, int... words) {
        Ram ram = new Ram(baseAddress, 64);
        AddressSpace bus = new AddressSpace();
        bus.addRegion(ram);
        for (int index = 0; index < words.length; index++) {
            bus.writeWord(baseAddress + (index * 2), words[index]);
        }
        return bus;
    }

    private static void configureTstScenario(M68kCpu cpu) {
        cpu.registers().setData(0, TST_NEGATIVE_BYTE);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);
    }

    private static void configureClrScenario(M68kCpu cpu) {
        cpu.registers().setData(0, CLR_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setZero(false);
    }

    private static void configureNotScenario(M68kCpu cpu) {
        cpu.registers().setData(0, NOT_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static void configureAndiScenario(M68kCpu cpu) {
        cpu.registers().setData(0, ANDI_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static void configureAndiToCcrScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(ANDI_TO_CCR_INITIAL_SR);
    }

    private static void configureAndiToSrScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(ANDI_TO_SR_INITIAL_SR);
    }

    private static void configureAndiToSrUserModeScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(ANDI_TO_SR_USER_MODE_SR);
    }

    private static void configureOriScenario(M68kCpu cpu) {
        cpu.registers().setData(0, ORI_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }
}
