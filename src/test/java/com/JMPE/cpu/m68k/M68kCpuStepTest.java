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
import com.JMPE.cpu.m68k.instructions.bit.Bchg;
import com.JMPE.cpu.m68k.instructions.bit.Bclr;
import com.JMPE.cpu.m68k.instructions.bit.Bset;
import com.JMPE.cpu.m68k.instructions.bit.Btst;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.arithmetic.Adda;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addi;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addx;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmpa;
import com.JMPE.cpu.m68k.instructions.arithmetic.Neg;
import com.JMPE.cpu.m68k.instructions.arithmetic.Negx;
import com.JMPE.cpu.m68k.instructions.arithmetic.Suba;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subx;
import com.JMPE.cpu.m68k.instructions.control.Dbcc;
import com.JMPE.cpu.m68k.instructions.control.Scc;
import com.JMPE.cpu.m68k.instructions.data.Exg;
import com.JMPE.cpu.m68k.instructions.data.Ext;
import com.JMPE.cpu.m68k.instructions.data.Swap;
import com.JMPE.cpu.m68k.instructions.shift.Roxl;
import com.JMPE.cpu.m68k.instructions.shift.Roxr;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subi;
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
    private static final int EORI_B_D0 = 0x0A00;
    private static final int EORI_BYTE_IMMEDIATE = 0x0080;
    private static final int EORI_BYTE_INITIAL = 0x1234_5600;
    private static final int EORI_BYTE_RESULT = 0x1234_5680;
    private static final int EORI_TO_CCR = 0x0A3C;
    private static final int EORI_TO_CCR_IMMEDIATE = 0x0015;
    private static final int EORI_TO_CCR_INITIAL_SR = 0x251F;
    private static final int EORI_TO_CCR_RESULT_SR = 0x250A;
    private static final int EORI_TO_SR = 0x0A7C;
    private static final int EORI_TO_SR_IMMEDIATE = 0x20F0;
    private static final int EORI_TO_SR_INITIAL_SR = 0xA71F;
    private static final int EORI_TO_SR_RESULT_SR = 0x87EF;
    private static final int EORI_TO_SR_USER_MODE_SR = 0x071F;
    private static final int CMPI_B_D0 = 0x0C00;
    private static final int CMPI_BYTE_IMMEDIATE = 0x0001;
    private static final int CMPI_BYTE_INITIAL = 0x1234_5600;
    private static final int SUBI_B_D0 = 0x0400;
    private static final int ADDI_B_D0 = 0x0600;
    private static final int TST_B_D0 = 0x4A00;
    private static final int TST_NEGATIVE_BYTE = 0x0000_0080;
    private static final int BCHG_D0_D1 = 0x0141;
    private static final int BTST_D0_D1 = 0x0101;
    private static final int BTST_BIT_NUMBER = 33;
    private static final int BTST_TEST_VALUE = 0x0000_0002;
    private static final int BCLR_B_IMMEDIATE_DISP_A1 = 0x08A9;
    private static final int BSET_B_D4_A1 = 0x09D1;
    private static final int NEGX_B_D0 = 0x4000;
    private static final int NEG_B_D0 = 0x4400;
    private static final int SWAP_D1 = 0x4841;
    private static final int EXT_W_D0 = 0x4880;
    private static final int DBRA_D0_DISP = 0x51C8;
    private static final int SNE_D0 = 0x56C0;
    private static final int CMPA_L_A2_A1 = 0xB3CA;
    private static final int CMPM_B_A5_A3 = 0xB70D;
    private static final int SUBX_B_D4_D3 = 0x9704;
    private static final int ADDA_W_D0_A0 = 0xD0C0;
    private static final int ADDX_B_D4_D3 = 0xD704;
    private static final int SUBA_L_D0_A0 = 0x91C0;
    private static final int EXG_D1_D2 = 0xC342;
    private static final int ROXL_B_D1_D2 = 0xE332;
    private static final int ROXR_L_1_D2 = 0xE292;

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
    void stepFetchesDecodesDispatchesWritesDataRegisterAndUpdatesFlagsForEoriImmediateToDataRegister()
            throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureEoriScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, EORI_B_D0, EORI_BYTE_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(EORI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(EORI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(EORI_BYTE_RESULT, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void traceEoriImmediateToDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureEoriScenario(cpu);
        AddressSpace bus = busWithWords(0x0000_1000, EORI_B_D0, EORI_BYTE_IMMEDIATE);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[eori-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[eori-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[eori-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[eori-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[eori-trace] execute " + message)
        );

        System.out.printf("[eori-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
    void stepFetchesDecodesDispatchesWritesConditionCodeRegisterForEoriToCcr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureEoriToCcrScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, EORI_TO_CCR, EORI_TO_CCR_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(EORI_TO_CCR_INITIAL_SR, report.before().statusRegister());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(EORI_TO_CCR_RESULT_SR, report.after().statusRegister());
        assertEquals(EORI_TO_CCR_RESULT_SR, cpu.statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI_TO_CCR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepFetchesDecodesDispatchesWritesStatusRegisterForEoriToSr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureEoriToSrScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, EORI_TO_SR, EORI_TO_SR_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(EORI_TO_SR_INITIAL_SR, report.before().statusRegister());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(EORI_TO_SR_RESULT_SR, report.after().statusRegister());
        assertEquals(EORI_TO_SR_RESULT_SR, cpu.statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI_TO_SR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepLogsAndRethrowsPrivilegeViolationForEoriToSrInUserMode() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureEoriToSrUserModeScenario(cpu);
        List<String> logs = new ArrayList<>();

        PrivilegeViolation thrown = assertThrows(
                PrivilegeViolation.class,
                () -> cpu.step(
                        busWithWords(0x0000_1000, EORI_TO_SR, EORI_TO_SR_IMMEDIATE),
                        new DispatchTable(),
                        logs::add
                )
        );

        assertEquals("EORI to SR requires supervisor mode", thrown.getMessage());
        assertEquals(0x0000_1004, cpu.registers().programCounter());
        assertEquals(EORI_TO_SR_USER_MODE_SR, cpu.statusRegister().rawValue());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=EORI_TO_SR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndUpdatesFlagsForCmpiDataRegister() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureCmpiScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(
                busWithWords(0x0000_1000, CMPI_B_D0, CMPI_BYTE_IMMEDIATE),
                new DispatchTable(),
                logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(CMPI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(CMPI_BYTE_INITIAL, report.after().dataRegister(0));
        assertEquals(CMPI_BYTE_INITIAL, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CMPI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void traceCmpiImmediateToDataRegisterPipelineToConsole() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureCmpiScenario(cpu);
        AddressSpace bus = busWithWords(0x0000_1000, CMPI_B_D0, CMPI_BYTE_IMMEDIATE);
        DispatchTable dispatchTable = new DispatchTable();

        System.out.printf("[cmpi-trace] setup pc=0x%08X d0=0x%08X sr=0x%04X%n",
            cpu.registers().programCounter(),
            cpu.registers().data(0),
            cpu.statusRegister().rawValue());

        int fetchedOpword = bus.readWord(cpu.registers().programCounter());
        System.out.printf("[cmpi-trace] fetch opword=0x%04X pc=0x%08X%n",
            fetchedOpword, cpu.registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            fetchedOpword,
            bus,
            cpu.registers().programCounter() + 2
        );
        System.out.printf("[cmpi-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        Op handler = dispatchTable.lookup(decoded.opcode());
        System.out.printf("[cmpi-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

        M68kCpu.StepReport report = cpu.step(
            bus,
            dispatchTable,
            message -> System.out.println("[cmpi-trace] execute " + message)
        );

        System.out.printf("[cmpi-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
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
    void stepFetchesDecodesDispatchesAndAddsImmediateForAddi() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_007F);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithWords(0x0000_1000, ADDI_B_D0, 0x0001), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(0x0000_0080, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertFalse(cpu.statusRegister().isExtendSet());
        assertEquals(Addi.EXECUTION_CYCLES_DN, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ADDI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndSubtractsImmediateForSubi() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_0000);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithWords(0x0000_1000, SUBI_B_D0, 0x0001), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1004, report.after().programCounter());
        assertEquals(0x0000_00FF, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Subi.EXECUTION_CYCLES_DN, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=SUBI"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001004"));
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
    void stepFetchesDecodesDispatchesAndUpdatesZeroForBtstRegisterForm() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        configureBtstScenario(cpu);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, BTST_D0_D1), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(BTST_BIT_NUMBER, cpu.registers().data(0));
        assertEquals(BTST_TEST_VALUE, cpu.registers().data(1));
        assertFalse(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Btst.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=BTST"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndUpdatesAddressRegisterForSuba() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_0020);
        cpu.registers().setAddress(0, 0x0000_1000);
        cpu.statusRegister().setRawValue(0x2715);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, SUBA_L_D0_A0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0FE0, cpu.registers().address(0));
        assertEquals(0x2715, cpu.statusRegister().rawValue());
        assertEquals(Suba.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=SUBA"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndUpdatesAddressRegisterForAdda() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_FFFF);
        cpu.registers().setAddress(0, 0x0000_1000);
        cpu.statusRegister().setRawValue(0x2715);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, ADDA_W_D0_A0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0FFF, cpu.registers().address(0));
        assertEquals(0x2715, cpu.statusRegister().rawValue());
        assertEquals(Adda.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ADDA"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndUpdatesFlagsForCmpa() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setAddress(2, 0x0000_0002);
        cpu.registers().setAddress(1, 0x0000_0001);
        cpu.statusRegister().setExtend(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, CMPA_L_A2_A1), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0001, cpu.registers().address(1));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Cmpa.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CMPA"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndPostincrementsBothOperandsForCmpm() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setAddress(5, 0x0000_2000);
        cpu.registers().setAddress(3, 0x0000_2002);
        cpu.statusRegister().setExtend(true);
        List<String> logs = new ArrayList<>();

        AddressSpace bus = busWithOpword(0x0000_1000, CMPM_B_A5_A3);
        bus.addRegion(new Ram(0x0000_2000, 0x100));
        bus.writeByte(0x0000_2000, 0x01);
        bus.writeByte(0x0000_2002, 0x01);

        M68kCpu.StepReport report = cpu.step(bus, new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_2001, cpu.registers().address(5));
        assertEquals(0x0000_2003, cpu.registers().address(3));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CMPM"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndRotatesThroughExtendForRoxl() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(1, 1);
        cpu.registers().setData(2, 0x0000_0080);
        cpu.statusRegister().setExtend(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, ROXL_B_D1_D2), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0001, cpu.registers().data(2));
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Roxl.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ROXL"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndRotatesThroughExtendForRoxr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(2, 0x0000_0001);
        cpu.statusRegister().setExtend(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, ROXR_L_1_D2), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x8000_0000, cpu.registers().data(2));
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertEquals(Roxr.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ROXR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndSignExtendsByteToWordForExt() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x1234_5680);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, EXT_W_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x1234_FF80, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Ext.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EXT"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndNegatesDestinationForNeg() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_0001);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, NEG_B_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_00FF, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Neg.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NEG"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndNegatesDestinationWithExtendForNegx() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x0000_00FF);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setZero(false);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, NEGX_B_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0000, cpu.registers().data(0) & 0xFF);
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Negx.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NEGX"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndSwapsRegisterHalvesForSwap() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(1, 0x1234_5678);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, SWAP_D1), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x5678_1234, cpu.registers().data(1));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Swap.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=SWAP"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndExchangesRegistersForExg() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(1, 0x1111_2222);
        cpu.registers().setData(2, 0x3333_4444);
        cpu.statusRegister().setRawValue(0xA71F);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, EXG_D1_D2), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x3333_4444, cpu.registers().data(1));
        assertEquals(0x1111_2222, cpu.registers().data(2));
        assertEquals(0xA71F, cpu.statusRegister().rawValue());
        assertEquals(Exg.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EXG"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndSubtractsWithExtendForSubx() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(4, 0x0000_00FF);
        cpu.registers().setData(3, 0x0000_0000);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setZero(false);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, SUBX_B_D4_D3), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0000, cpu.registers().data(3) & 0xFF);
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Subx.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=SUBX"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndAddsWithExtendForAddx() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(4, 0x0000_0000);
        cpu.registers().setData(3, 0x0000_00FF);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setZero(false);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, ADDX_B_D4_D3), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0000, cpu.registers().data(3) & 0xFF);
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
        assertEquals(Addx.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ADDX"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndWritesMemoryForBset() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(4, 0);
        cpu.registers().setAddress(1, 0x0000_2000);
        cpu.statusRegister().setCarry(true);
        List<String> logs = new ArrayList<>();

        AddressSpace bus = busWithOpword(0x0000_1000, BSET_B_D4_A1);
        bus.addRegion(new Ram(0x0000_2000, 0x100));
        bus.writeByte(0x0000_2000, 0x00);

        M68kCpu.StepReport report = cpu.step(bus, new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x01, bus.readByte(0x0000_2000));
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertEquals(Bset.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=BSET"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndTogglesBitForBchg() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 1);
        cpu.registers().setData(1, 0x0000_0000);
        cpu.statusRegister().setCarry(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, BCHG_D0_D1), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x0000_0002, cpu.registers().data(1));
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertEquals(Bchg.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=BCHG"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndWritesMemoryForBclr() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setAddress(1, 0x0000_2000);
        cpu.statusRegister().setCarry(true);
        List<String> logs = new ArrayList<>();

        AddressSpace bus = busWithWords(0x0000_1000, BCLR_B_IMMEDIATE_DISP_A1, 0x0000, 0x0400);
        bus.addRegion(new Ram(0x0000_2400, 0x100));
        bus.writeByte(0x0000_2400, 0x03);

        M68kCpu.StepReport report = cpu.step(bus, new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1006, report.after().programCounter());
        assertEquals(0x02, bus.readByte(0x0000_2400));
        assertFalse(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertEquals(Bclr.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=BCLR"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001006"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndBranchesForDbra() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 1);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithWords(0x0000_1000, DBRA_D0_DISP, 0x0004), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1006, report.after().programCounter());
        assertEquals(0x0000_0000, cpu.registers().data(0));
        assertEquals(Dbcc.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=DBcc"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001006"));
    }

    @Test
    void stepFetchesDecodesDispatchesAndWritesConditionalByteForScc() throws IllegalInstructionException {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.registers().setData(0, 0x1234_5600);
        cpu.statusRegister().setZero(false);
        cpu.statusRegister().setCarry(true);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.step(busWithOpword(0x0000_1000, SNE_D0), new DispatchTable(), logs::add);

        assertTrue(report.success());
        assertEquals(0x0000_1000, report.before().programCounter());
        assertEquals(0x0000_1002, report.after().programCounter());
        assertEquals(0x1234_56FF, cpu.registers().data(0));
        assertFalse(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertEquals(Scc.EXECUTION_CYCLES, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=Scc"));
        assertTrue(logs.get(0).contains("pc=0x00001000->0x00001002"));
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

    private static void configureBtstScenario(M68kCpu cpu) {
        cpu.registers().setData(0, BTST_BIT_NUMBER);
        cpu.registers().setData(1, BTST_TEST_VALUE);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(true);
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

    private static void configureEoriScenario(M68kCpu cpu) {
        cpu.registers().setData(0, EORI_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static void configureEoriToCcrScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(EORI_TO_CCR_INITIAL_SR);
    }

    private static void configureEoriToSrScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(EORI_TO_SR_INITIAL_SR);
    }

    private static void configureEoriToSrUserModeScenario(M68kCpu cpu) {
        cpu.statusRegister().setRawValue(EORI_TO_SR_USER_MODE_SR);
    }

    private static void configureCmpiScenario(M68kCpu cpu) {
        cpu.registers().setData(0, CMPI_BYTE_INITIAL);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(false);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }
}
