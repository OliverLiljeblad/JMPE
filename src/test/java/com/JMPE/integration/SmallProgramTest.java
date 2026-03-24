package com.JMPE.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.Decoder;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.dispatch.Op;
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

        Op handler = new DispatchTable().lookup(decoded.opcode());
        System.out.printf("[machine-tst-trace] dispatch handler=%s%n", handler.getClass().getSimpleName());

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
        byte[] bytes = new byte[0x0200];

        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x20;
        bytes[3] = 0x00;

        bytes[4] = 0x00;
        bytes[5] = 0x40;
        bytes[6] = 0x01;
        bytes[7] = 0x00;

        bytes[INSTRUCTION_OFFSET] = (byte) ((opword >>> 8) & 0xFF);
        bytes[INSTRUCTION_OFFSET + 1] = (byte) (opword & 0xFF);
        return bytes;
    }

    private static void configureTstScenario(M68kCpu cpu) {
        cpu.registers().setData(0, 0x0000_0080);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);
    }
}
