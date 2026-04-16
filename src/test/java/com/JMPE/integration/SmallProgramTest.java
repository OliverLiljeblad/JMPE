package com.JMPE.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.Decoder;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.dispatch.Op;
import com.JMPE.cpu.m68k.exceptions.ExceptionVector;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.machine.MacPlusMachine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class SmallProgramTest {
    private static final int ROM_BASE = 0x0040_0000;
    private static final int INITIAL_STACK_POINTER = 0x0000_2000;
    private static final int INITIAL_PROGRAM_COUNTER = 0x0040_0100;
    private static final int INSTRUCTION_OFFSET = INITIAL_PROGRAM_COUNTER - ROM_BASE;
    private static final int NOP = 0x4E71;
    private static final int STOP = 0x4E72;
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
    private static final int TST_B_D0 = 0x4A00;
    private static final int TST_W_A0 = 0x4A50;
    private static final int VIA_IER_ADDRESS = 0x00E8_1C00;
    private static final int GROUP0_FRAME_WORD_SUPERVISOR_PROGRAM_READ = 0x001E;
    private static final int GROUP0_FRAME_WORD_TST_SUPERVISOR_DATA_READ = 0x4A55;
    private static final int TEN_INSTRUCTION_PROGRAM_INITIAL_SR = 0x271F;
    private static final int TEN_INSTRUCTION_PROGRAM_FINAL_D0 = 0x1234_5600;
    private static final int TEN_INSTRUCTION_PROGRAM_FINAL_SR = 0x2010;
    private static final int TEN_INSTRUCTION_PROGRAM_FINAL_PC = INITIAL_PROGRAM_COUNTER + 0x20;
    private static final int TEN_INSTRUCTION_PROGRAM_STEP_COUNT = 10;
    private static final int TEN_INSTRUCTION_PROGRAM_TOTAL_CYCLES = 40;
    private static final List<String> TEN_INSTRUCTION_PROGRAM_OPCODES = List.of(
        "NOP",
        "CLR",
        "NOT",
        "ANDI",
        "ANDI_TO_CCR",
        "ORI",
        "EORI",
        "CMPI",
        "TST",
        "ANDI_TO_SR"
    );

    @Test
    void stepsNopThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(NOP), ROM_BASE);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, report.after().programCounter());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NOP"));
        assertTrue(logs.get(0).contains("cycles=4"));
    }

    @Test
    void traceNopThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(NOP), ROM_BASE);

        System.out.printf("[machine-nop-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-nop-trace] reset ssp=0x%08X pc=0x%08X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter());
        System.out.printf("[machine-nop-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-nop-trace] execute " + message)
        );

        System.out.printf("[machine-nop-trace] result success=%s cycles=%d finalPc=0x%08X%n",
            report.success(), report.cycles(), machine.cpu().registers().programCounter());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
    }

    @Test
    void stepsClrDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(CLR_B_D0), ROM_BASE);
        configureClrScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(CLR_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, report.after().programCounter());
        assertEquals(CLR_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(CLR_BYTE_RESULT, machine.cpu().registers().data(0));
        assertFalse(machine.cpu().statusRegister().isNegativeSet());
        assertTrue(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CLR"));
    }

    @Test
    void traceClrDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(CLR_B_D0), ROM_BASE);
        configureClrScenario(machine.cpu());

        System.out.printf("[machine-clr-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-clr-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-clr-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-clr-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-clr-trace] execute " + message)
        );

        System.out.printf("[machine-clr-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(CLR_BYTE_RESULT, machine.cpu().registers().data(0));
    }

    @Test
    void stepsNotDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(NOT_B_D0), ROM_BASE);
        configureNotScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(NOT_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, report.after().programCounter());
        assertEquals(NOT_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(NOT_BYTE_RESULT, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=NOT"));
    }

    @Test
    void traceNotDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(NOT_B_D0), ROM_BASE);
        configureNotScenario(machine.cpu());

        System.out.printf("[machine-not-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-not-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-not-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-not-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-not-trace] execute " + message)
        );

        System.out.printf("[machine-not-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(NOT_BYTE_RESULT, machine.cpu().registers().data(0));
    }

    @Test
    void stepsAndiImmediateToDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_B_D0, ANDI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureAndiScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(ANDI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(ANDI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(ANDI_BYTE_RESULT, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI"));
    }

    @Test
    void traceAndiImmediateToDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_B_D0, ANDI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureAndiScenario(machine.cpu());

        System.out.printf("[machine-andi-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-andi-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-andi-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-andi-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-andi-trace] execute " + message)
        );

        System.out.printf("[machine-andi-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(ANDI_BYTE_RESULT, machine.cpu().registers().data(0));
    }

    @Test
    void stepsAndiToCcrThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_TO_CCR, ANDI_TO_CCR_IMMEDIATE),
                ROM_BASE
        );
        configureAndiToCcrScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(ANDI_TO_CCR_INITIAL_SR, report.before().statusRegister());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(ANDI_TO_CCR_RESULT_SR, report.after().statusRegister());
        assertEquals(ANDI_TO_CCR_RESULT_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI_TO_CCR"));
    }

    @Test
    void stepsAndiToSrThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE),
                ROM_BASE
        );
        configureAndiToSrScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(ANDI_TO_SR_INITIAL_SR, report.before().statusRegister());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(ANDI_TO_SR_RESULT_SR, report.after().statusRegister());
        assertEquals(ANDI_TO_SR_RESULT_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI_TO_SR"));
    }

    @Test
    void vectorsPrivilegeViolationForAndiToSrInUserModeThroughMachineLayer() throws IllegalInstructionException {
        Ram lowMemory = new Ram(0x0000_0000, 0x4000);
        lowMemory.writeLong(ExceptionVector.PRIVILEGE_VIOLATION.vectorAddress(), 0x0000_0400);
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE),
                ROM_BASE,
                lowMemory
        );
        configureAndiToSrUserModeScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(0x0000_0400, report.after().programCounter());
        assertEquals(0x0000_0400, machine.cpu().registers().programCounter());
        assertEquals(0x271F, machine.cpu().statusRegister().rawValue());
        assertEquals(INITIAL_STACK_POINTER - 6, machine.cpu().registers().supervisorStackPointer());
        assertEquals(ANDI_TO_SR_USER_MODE_SR, lowMemory.readWord(INITIAL_STACK_POINTER - 6));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, lowMemory.readLong(INITIAL_STACK_POINTER - 4));
        assertEquals(0, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ANDI_TO_SR"));
    }

    @Test
    void stepsOriImmediateToDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ORI_B_D0, ORI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureOriScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(ORI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(ORI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(ORI_BYTE_RESULT, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ORI"));
    }

    @Test
    void traceOriImmediateToDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ORI_B_D0, ORI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureOriScenario(machine.cpu());

        System.out.printf("[machine-ori-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-ori-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-ori-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-ori-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-ori-trace] execute " + message)
        );

        System.out.printf("[machine-ori-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(ORI_BYTE_RESULT, machine.cpu().registers().data(0));
    }

    @Test
    void stepsEoriImmediateToDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_B_D0, EORI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureEoriScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(EORI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(EORI_BYTE_RESULT, report.after().dataRegister(0));
        assertEquals(EORI_BYTE_RESULT, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI"));
    }

    @Test
    void traceEoriImmediateToDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_B_D0, EORI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureEoriScenario(machine.cpu());

        System.out.printf("[machine-eori-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-eori-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-eori-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-eori-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-eori-trace] execute " + message)
        );

        System.out.printf("[machine-eori-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(EORI_BYTE_RESULT, machine.cpu().registers().data(0));
    }

    @Test
    void stepsEoriToCcrThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_TO_CCR, EORI_TO_CCR_IMMEDIATE),
                ROM_BASE
        );
        configureEoriToCcrScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(EORI_TO_CCR_INITIAL_SR, report.before().statusRegister());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(EORI_TO_CCR_RESULT_SR, report.after().statusRegister());
        assertEquals(EORI_TO_CCR_RESULT_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI_TO_CCR"));
    }

    @Test
    void stepsEoriToSrThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_TO_SR, EORI_TO_SR_IMMEDIATE),
                ROM_BASE
        );
        configureEoriToSrScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(EORI_TO_SR_INITIAL_SR, report.before().statusRegister());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(EORI_TO_SR_RESULT_SR, report.after().statusRegister());
        assertEquals(EORI_TO_SR_RESULT_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI_TO_SR"));
    }

    @Test
    void vectorsPrivilegeViolationForEoriToSrInUserModeThroughMachineLayer() throws IllegalInstructionException {
        Ram lowMemory = new Ram(0x0000_0000, 0x4000);
        lowMemory.writeLong(ExceptionVector.PRIVILEGE_VIOLATION.vectorAddress(), 0x0000_0400);
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_TO_SR, EORI_TO_SR_IMMEDIATE),
                ROM_BASE,
                lowMemory
        );
        configureEoriToSrUserModeScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(0x0000_0400, report.after().programCounter());
        assertEquals(0x0000_0400, machine.cpu().registers().programCounter());
        assertEquals(0x271F, machine.cpu().statusRegister().rawValue());
        assertEquals(INITIAL_STACK_POINTER - 6, machine.cpu().registers().supervisorStackPointer());
        assertEquals(EORI_TO_SR_USER_MODE_SR, lowMemory.readWord(INITIAL_STACK_POINTER - 6));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, lowMemory.readLong(INITIAL_STACK_POINTER - 4));
        assertEquals(0, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=EORI_TO_SR"));
    }

    @Test
    void stepsCmpiDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(CMPI_B_D0, CMPI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureCmpiScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(CMPI_BYTE_INITIAL, report.before().dataRegister(0));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, report.after().programCounter());
        assertEquals(CMPI_BYTE_INITIAL, report.after().dataRegister(0));
        assertEquals(CMPI_BYTE_INITIAL, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertTrue(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=CMPI"));
    }

    @Test
    void traceCmpiDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(CMPI_B_D0, CMPI_BYTE_IMMEDIATE),
                ROM_BASE
        );
        configureCmpiScenario(machine.cpu());

        System.out.printf("[machine-cmpi-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-cmpi-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-cmpi-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-cmpi-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-cmpi-trace] execute " + message)
        );

        System.out.printf("[machine-cmpi-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
        assertEquals(CMPI_BYTE_INITIAL, machine.cpu().registers().data(0));
    }

    @Test
    void stepsTstDataRegisterThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(TST_B_D0), ROM_BASE);
        configureTstScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, report.after().programCounter());
        assertEquals(0x0000_0080, machine.cpu().registers().data(0));
        assertTrue(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertEquals(4, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=TST"));
    }

    @Test
    void traceTstDataRegisterThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleInstruction(TST_B_D0), ROM_BASE);
        configureTstScenario(machine.cpu());

        System.out.printf("[machine-tst-trace] machine romBase=0x%08X bus=%s%n",
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-tst-trace] reset ssp=0x%08X pc=0x%08X d0=0x%08X sr=0x%04X%n",
            machine.cpu().registers().stackPointer(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue());
        System.out.printf("[machine-tst-trace] fetch opword=0x%04X pc=0x%08X%n",
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.cpu().registers().programCounter());

        DecodedInstruction decoded = new Decoder().decode(
            machine.bus().readWord(machine.cpu().registers().programCounter()),
            machine.bus(),
            machine.cpu().registers().programCounter() + 2
        );
        System.out.printf("[machine-tst-trace] decode opcode=%s size=%s src=%s dst=%s nextPc=0x%08X%n",
            decoded.opcode(), decoded.size(), decoded.src(), decoded.dst(), decoded.nextPc());

        M68kCpu.StepReport report = machine.step(
            message -> System.out.println("[machine-tst-trace] execute " + message)
        );

        System.out.printf("[machine-tst-trace] result success=%s cycles=%d finalPc=0x%08X d0=0x%08X sr=0x%04X X=%s N=%s Z=%s V=%s C=%s%n",
            report.success(),
            report.cycles(),
            machine.cpu().registers().programCounter(),
            machine.cpu().registers().data(0),
            machine.cpu().statusRegister().rawValue(),
            machine.cpu().statusRegister().isExtendSet(),
            machine.cpu().statusRegister().isNegativeSet(),
            machine.cpu().statusRegister().isZeroSet(),
            machine.cpu().statusRegister().isOverflowSet(),
            machine.cpu().statusRegister().isCarrySet());

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 2, machine.cpu().registers().programCounter());
        assertEquals(4, report.cycles());
    }

    @Test
    void vectorsAddressErrorForOddOpcodeFetchThroughMachineLayer() throws IllegalInstructionException {
        Ram lowMemory = new Ram(0x0000_0000, 0x4000);
        lowMemory.writeLong(ExceptionVector.ADDRESS_ERROR.vectorAddress(), 0x0000_0400);
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
            romBytesWithResetVectors(INITIAL_STACK_POINTER, INITIAL_PROGRAM_COUNTER + 1, NOP),
            ROM_BASE,
            lowMemory
        );
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 1, report.before().programCounter());
        assertEquals(0x0000_0400, report.after().programCounter());
        assertEquals(0x0000_0400, machine.cpu().registers().programCounter());
        assertEquals(0x2700, machine.cpu().statusRegister().rawValue());
        assertEquals(INITIAL_STACK_POINTER - 14, machine.cpu().registers().supervisorStackPointer());
        assertEquals(GROUP0_FRAME_WORD_SUPERVISOR_PROGRAM_READ, lowMemory.readWord(INITIAL_STACK_POINTER - 14));
        assertEquals(INITIAL_PROGRAM_COUNTER + 1, lowMemory.readLong(INITIAL_STACK_POINTER - 12));
        assertEquals(0x0000, lowMemory.readWord(INITIAL_STACK_POINTER - 8));
        assertEquals(0x2700, lowMemory.readWord(INITIAL_STACK_POINTER - 6));
        assertEquals(INITIAL_PROGRAM_COUNTER + 1, lowMemory.readLong(INITIAL_STACK_POINTER - 4));
        assertEquals(0, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ADDRESS_ERROR"));
    }

    @Test
    void vectorsBusErrorForUnmappedDataReadThroughMachineLayer() throws IllegalInstructionException {
        Ram lowMemory = new Ram(0x0000_0000, 0x4000);
        lowMemory.writeLong(ExceptionVector.BUS_ERROR.vectorAddress(), 0x0000_0400);
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
            romBytesWithSingleInstruction(TST_W_A0),
            ROM_BASE,
            lowMemory
        );
        machine.cpu().registers().setAddress(0, 0x0050_0000);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = machine.step(logs::add);

        assertTrue(report.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, report.before().programCounter());
        assertEquals(0x0000_0400, report.after().programCounter());
        assertEquals(0x0000_0400, machine.cpu().registers().programCounter());
        assertEquals(0x2700, machine.cpu().statusRegister().rawValue());
        assertEquals(INITIAL_STACK_POINTER - 14, machine.cpu().registers().supervisorStackPointer());
        assertEquals(GROUP0_FRAME_WORD_TST_SUPERVISOR_DATA_READ, lowMemory.readWord(INITIAL_STACK_POINTER - 14));
        assertEquals(0x0050_0000, lowMemory.readLong(INITIAL_STACK_POINTER - 12));
        assertEquals(TST_W_A0, lowMemory.readWord(INITIAL_STACK_POINTER - 8));
        assertEquals(0x2700, lowMemory.readWord(INITIAL_STACK_POINTER - 6));
        assertEquals(INITIAL_PROGRAM_COUNTER, lowMemory.readLong(INITIAL_STACK_POINTER - 4));
        assertEquals(0, report.cycles());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=TST"));
    }

    @Test
    void takesViaInterruptAndWakesStoppedCpuThroughMachineLayer() throws IllegalInstructionException {
        Ram lowMemory = new Ram(0x0000_0000, 0x4000);
        lowMemory.writeLong(ExceptionVector.interruptAutovectorNumber(1) * 4, 0x0000_0400);
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
            romBytesWithInstructionWords(STOP, 0x2000, NOP),
            ROM_BASE,
            lowMemory
        );
        machine.bus().writeByte(VIA_IER_ADDRESS, 0x82);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport first = machine.step(logs::add);
        M68kCpu.StepReport second = machine.step(logs::add);

        assertTrue(first.success());
        assertEquals(INITIAL_PROGRAM_COUNTER, first.before().programCounter());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, first.after().programCounter());
        assertEquals(4, first.cycles());
        assertTrue(second.success());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, second.before().programCounter());
        assertEquals(0x0000_0400, second.after().programCounter());
        assertEquals(0x0000_0400, machine.cpu().registers().programCounter());
        assertFalse(machine.cpu().isStopped());
        assertEquals(0x2100, machine.cpu().statusRegister().rawValue());
        assertEquals(INITIAL_STACK_POINTER - 6, machine.cpu().registers().supervisorStackPointer());
        assertEquals(0x2000, lowMemory.readWord(INITIAL_STACK_POINTER - 6));
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, lowMemory.readLong(INITIAL_STACK_POINTER - 4));
        assertEquals(44, second.cycles());
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=STOP"));
        assertTrue(logs.get(1).contains("[m68k-step] OK op=INTERRUPT_LEVEL_1"));
    }

    @Test
    void stepsTenInstructionProgramThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
            romBytesWithInstructionWords(tenInstructionProgramWords()),
            ROM_BASE
        );
        configureTenInstructionProgramScenario(machine.cpu());
        List<String> logs = new ArrayList<>();
        List<M68kCpu.StepReport> reports = new ArrayList<>();

        for (int index = 0; index < TEN_INSTRUCTION_PROGRAM_STEP_COUNT; index++) {
            reports.add(machine.step(logs::add));
        }

        assertEquals(TEN_INSTRUCTION_PROGRAM_STEP_COUNT, reports.size());
        assertEquals(TEN_INSTRUCTION_PROGRAM_STEP_COUNT, logs.size());

        int expectedPc = INITIAL_PROGRAM_COUNTER;
        int totalCycles = 0;
        for (int index = 0; index < TEN_INSTRUCTION_PROGRAM_STEP_COUNT; index++) {
            M68kCpu.StepReport report = reports.get(index);
            assertTrue(report.success());
            assertEquals(expectedPc, report.before().programCounter());
            assertTrue(logs.get(index).contains("[m68k-step] OK op=" + TEN_INSTRUCTION_PROGRAM_OPCODES.get(index)));

            totalCycles += report.cycles();
            expectedPc = report.after().programCounter();
        }

        assertEquals(TEN_INSTRUCTION_PROGRAM_INITIAL_SR, reports.get(0).before().statusRegister());
        assertEquals(CLR_BYTE_INITIAL, reports.get(0).before().dataRegister(0));
        assertEquals(TEN_INSTRUCTION_PROGRAM_TOTAL_CYCLES, totalCycles);
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_PC, expectedPc);
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_D0,
            reports.get(TEN_INSTRUCTION_PROGRAM_STEP_COUNT - 1).after().dataRegister(0));
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_SR,
            reports.get(TEN_INSTRUCTION_PROGRAM_STEP_COUNT - 1).after().statusRegister());
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_PC, machine.cpu().registers().programCounter());
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_D0, machine.cpu().registers().data(0));
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_SR, machine.cpu().statusRegister().rawValue());
        assertTrue(machine.cpu().statusRegister().isExtendSet());
        assertFalse(machine.cpu().statusRegister().isNegativeSet());
        assertFalse(machine.cpu().statusRegister().isZeroSet());
        assertFalse(machine.cpu().statusRegister().isOverflowSet());
        assertFalse(machine.cpu().statusRegister().isCarrySet());
    }

    @Test
    void traceTenInstructionProgramThroughMachineLayerToConsole() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
            romBytesWithInstructionWords(tenInstructionProgramWords()),
            ROM_BASE
        );
        configureTenInstructionProgramScenario(machine.cpu());
        List<M68kCpu.StepReport> reports = new ArrayList<>();
        int successfulSteps = 0;
        int totalCycles = 0;

        System.out.printf("[machine-ten-step-trace] start steps=%d romBase=0x%08X bus=%s%n",
            TEN_INSTRUCTION_PROGRAM_STEP_COUNT,
            machine.rom().base(), machine.bus().getClass().getSimpleName());
        System.out.printf("[machine-ten-step-trace] initial ssp=%s pc=%s d0=%s sr=%s flags=%s%n",
            formatHex(machine.cpu().registers().stackPointer()),
            formatHex(machine.cpu().registers().programCounter()),
            formatHex(machine.cpu().registers().data(0)),
            formatWord(machine.cpu().statusRegister().rawValue()),
            formatFlags(machine.cpu().statusRegister().rawValue()));

        for (int index = 0; index < TEN_INSTRUCTION_PROGRAM_STEP_COUNT; index++) {
            int stepNumber = index + 1;
            int pc = machine.cpu().registers().programCounter();
            int opword = machine.bus().readWord(pc);
            DecodedInstruction decoded = new Decoder().decode(opword, machine.bus(), pc + 2);

            System.out.printf("[machine-ten-step-trace] step=%d/%d running op=%s size=%s pc=%s d0=%s sr=%s flags=%s%n",
                stepNumber,
                TEN_INSTRUCTION_PROGRAM_STEP_COUNT,
                decoded.opcode(),
                decoded.size(),
                formatHex(pc),
                formatHex(machine.cpu().registers().data(0)),
                formatWord(machine.cpu().statusRegister().rawValue()),
                formatFlags(machine.cpu().statusRegister().rawValue()));

            try {
                M68kCpu.StepReport report = machine.step();
                reports.add(report);
                successfulSteps++;
                totalCycles += report.cycles();

                System.out.printf("[machine-ten-step-trace] step=%d/%d result=SUCCESS op=%s cycles=%d pc=%s->%s d0=%s->%s sr=%s->%s flags=%s->%s%n",
                    stepNumber,
                    TEN_INSTRUCTION_PROGRAM_STEP_COUNT,
                    decoded.opcode(),
                    report.cycles(),
                    formatHex(report.before().programCounter()),
                    formatHex(report.after().programCounter()),
                    formatHex(report.before().dataRegister(0)),
                    formatHex(report.after().dataRegister(0)),
                    formatWord(report.before().statusRegister()),
                    formatWord(report.after().statusRegister()),
                    formatFlags(report.before().statusRegister()),
                    formatFlags(report.after().statusRegister()));
            } catch (IllegalInstructionException | RuntimeException exception) {
                System.out.printf("[machine-ten-step-trace] step=%d/%d result=FAIL op=%s error=\"%s\" pc=%s d0=%s sr=%s flags=%s%n",
                    stepNumber,
                    TEN_INSTRUCTION_PROGRAM_STEP_COUNT,
                    decoded.opcode(),
                    exception.getMessage() == null ? "<no-message>" : exception.getMessage(),
                    formatHex(machine.cpu().registers().programCounter()),
                    formatHex(machine.cpu().registers().data(0)),
                    formatWord(machine.cpu().statusRegister().rawValue()),
                    formatFlags(machine.cpu().statusRegister().rawValue()));
                printTenInstructionProgramSummary(successfulSteps, totalCycles, machine.cpu());
                throw exception;
            }
        }

        printTenInstructionProgramSummary(successfulSteps, totalCycles, machine.cpu());

        assertEquals(TEN_INSTRUCTION_PROGRAM_STEP_COUNT, successfulSteps);
        assertEquals(TEN_INSTRUCTION_PROGRAM_STEP_COUNT, reports.size());
        assertEquals(TEN_INSTRUCTION_PROGRAM_TOTAL_CYCLES, totalCycles);
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_PC, machine.cpu().registers().programCounter());
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_D0, machine.cpu().registers().data(0));
        assertEquals(TEN_INSTRUCTION_PROGRAM_FINAL_SR, machine.cpu().statusRegister().rawValue());
    }

    private static void printTenInstructionProgramSummary(int successfulSteps, int totalCycles, M68kCpu cpu) {
        System.out.printf("[machine-ten-step-trace] summary successful=%d/%d failed=%d totalCycles=%d finalPc=%s d0=%s sr=%s flags=%s%n",
            successfulSteps,
            TEN_INSTRUCTION_PROGRAM_STEP_COUNT,
            TEN_INSTRUCTION_PROGRAM_STEP_COUNT - successfulSteps,
            totalCycles,
            formatHex(cpu.registers().programCounter()),
            formatHex(cpu.registers().data(0)),
            formatWord(cpu.statusRegister().rawValue()),
            formatFlags(cpu.statusRegister().rawValue()));
    }

    private static String formatFlags(int statusRegister) {
        int ccr = statusRegister & 0xFF;
        return "X=" + ((ccr >>> 4) & 1)
            + ",N=" + ((ccr >>> 3) & 1)
            + ",Z=" + ((ccr >>> 2) & 1)
            + ",V=" + ((ccr >>> 1) & 1)
            + ",C=" + (ccr & 1);
    }

    private static String formatHex(int value) {
        return String.format("0x%08X", value);
    }

    private static String formatWord(int value) {
        return String.format("0x%04X", value & 0xFFFF);
    }

    private static byte[] romBytesWithSingleInstruction(int opword) {
        return romBytesWithInstructionWords(opword);
    }

    private static int[] tenInstructionProgramWords() {
        return new int[] {
            NOP,
            CLR_B_D0,
            NOT_B_D0,
            ANDI_B_D0, ANDI_BYTE_IMMEDIATE,
            ANDI_TO_CCR, ANDI_TO_CCR_IMMEDIATE,
            ORI_B_D0, ORI_BYTE_IMMEDIATE,
            EORI_B_D0, EORI_BYTE_IMMEDIATE,
            CMPI_B_D0, CMPI_BYTE_IMMEDIATE,
            TST_B_D0,
            ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE
        };
    }

    private static byte[] romBytesWithInstructionWords(int... words) {
        return romBytesWithResetVectors(INITIAL_STACK_POINTER, INITIAL_PROGRAM_COUNTER, words);
    }

    private static byte[] romBytesWithResetVectors(int initialStackPointer, int initialProgramCounter, int... words) {
        byte[] bytes = new byte[0x0200];
        int programOffset = initialProgramCounter - ROM_BASE;

        writeLong(bytes, 0x0000, initialStackPointer);
        writeLong(bytes, 0x0004, initialProgramCounter);

        for (int index = 0; index < words.length; index++) {
            int word = words[index];
            int offset = programOffset + (index * 2);
            bytes[offset] = (byte) ((word >>> 8) & 0xFF);
            bytes[offset + 1] = (byte) (word & 0xFF);
        }
        return bytes;
    }

    private static void writeLong(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    private static void configureTstScenario(M68kCpu cpu) {
        cpu.registers().setData(0, 0x0000_0080);
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

    private static void configureTenInstructionProgramScenario(M68kCpu cpu) {
        cpu.registers().setData(0, CLR_BYTE_INITIAL);
        cpu.statusRegister().setRawValue(TEN_INSTRUCTION_PROGRAM_INITIAL_SR);
    }
}
