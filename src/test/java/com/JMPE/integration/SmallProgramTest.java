package com.JMPE.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.Decoder;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.dispatch.Op;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
    void rejectsAndiToSrInUserModeThroughMachineLayer() {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(ANDI_TO_SR, ANDI_TO_SR_IMMEDIATE),
                ROM_BASE
        );
        configureAndiToSrUserModeScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        PrivilegeViolation thrown = assertThrows(
                PrivilegeViolation.class,
                () -> machine.step(logs::add)
        );

        assertEquals("ANDI to SR requires supervisor mode", thrown.getMessage());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(ANDI_TO_SR_USER_MODE_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=ANDI_TO_SR"));
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
    void rejectsEoriToSrInUserModeThroughMachineLayer() {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(
                romBytesWithInstructionWords(EORI_TO_SR, EORI_TO_SR_IMMEDIATE),
                ROM_BASE
        );
        configureEoriToSrUserModeScenario(machine.cpu());
        List<String> logs = new ArrayList<>();

        PrivilegeViolation thrown = assertThrows(
                PrivilegeViolation.class,
                () -> machine.step(logs::add)
        );

        assertEquals("EORI to SR requires supervisor mode", thrown.getMessage());
        assertEquals(INITIAL_PROGRAM_COUNTER + 4, machine.cpu().registers().programCounter());
        assertEquals(EORI_TO_SR_USER_MODE_SR, machine.cpu().statusRegister().rawValue());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=EORI_TO_SR"));
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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
            machine.rom().baseAddress(), machine.bus().getClass().getSimpleName());
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

    private static byte[] romBytesWithSingleInstruction(int opword) {
        return romBytesWithInstructionWords(opword);
    }

    private static byte[] romBytesWithInstructionWords(int... words) {
        byte[] bytes = new byte[0x0200];

        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x20;
        bytes[3] = 0x00;

        bytes[4] = 0x00;
        bytes[5] = 0x40;
        bytes[6] = 0x01;
        bytes[7] = 0x00;

        for (int index = 0; index < words.length; index++) {
            int word = words[index];
            int offset = INSTRUCTION_OFFSET + (index * 2);
            bytes[offset] = (byte) ((word >>> 8) & 0xFF);
            bytes[offset + 1] = (byte) (word & 0xFF);
        }
        return bytes;
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
}
