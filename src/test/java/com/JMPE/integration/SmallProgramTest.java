package com.JMPE.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.machine.MacPlusMachine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class SmallProgramTest {
    private static final int ROM_BASE = 0x0040_0000;
    private static final int INITIAL_STACK_POINTER = 0x0000_2000;
    private static final int INITIAL_PROGRAM_COUNTER = 0x0040_0100;
    private static final int NOP_OFFSET = INITIAL_PROGRAM_COUNTER - ROM_BASE;

    @Test
    void stepsNopThroughMachineLayer() throws IllegalInstructionException {
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleNop(), ROM_BASE);
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
        MacPlusMachine machine = MacPlusMachine.fromRomBytes(romBytesWithSingleNop(), ROM_BASE);

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

    private static byte[] romBytesWithSingleNop() {
        byte[] bytes = new byte[0x0200];

        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x20;
        bytes[3] = 0x00;

        bytes[4] = 0x00;
        bytes[5] = 0x40;
        bytes[6] = 0x01;
        bytes[7] = 0x00;

        bytes[NOP_OFFSET] = 0x4E;
        bytes[NOP_OFFSET + 1] = 0x71;
        return bytes;
    }
}
