package com.JMPE.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.JMPE.harness.BootHarness;
import com.JMPE.machine.MacPlusMachine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

class BootsToQuestionMarkTest {
    private static final int RESET_STACK_POINTER = 0x0000_2000;
    private static final int RESET_PROGRAM_COUNTER = BootHarness.DEFAULT_ROM_BASE + 0x0100;
    private static final int BSR_W = 0x6100;
    private static final int BSR_TO_RTS_DISPLACEMENT = 0x0006;
    private static final int NOP = 0x4E71;
    private static final int RTS = 0x4E75;

    @Test
    void stepsSyntheticResetProgramThroughBootHarness() {
        MacPlusMachine machine = BootHarness.machineFromRomBytes(
            romBytesWithProgramWords(BSR_W, BSR_TO_RTS_DISPLACEMENT, NOP, NOP, RTS),
            BootHarness.DEFAULT_ROM_BASE
        );

        BootHarness.BootRun run = BootHarness.runSteps(machine, 4);

        assertEquals(4, run.stepsCompleted());
        assertEquals(4, run.logs().size());
        assertEquals(RESET_PROGRAM_COUNTER, run.reports().get(0).before().programCounter());
        assertEquals(RESET_PROGRAM_COUNTER + 8, machine.cpu().registers().programCounter());
        assertEquals(RESET_STACK_POINTER, machine.cpu().registers().stackPointer());
        assertEquals(RESET_PROGRAM_COUNTER + 4, machine.bus().readLong(RESET_STACK_POINTER - 4));
    }

    @Test
    void runsLocalMacPlusRomForConfiguredBootSmokeSteps() throws IOException {
        Optional<Path> romPath = BootHarness.findLocalRom();
        assumeTrue(
            romPath.isPresent(),
            "Place a local Mac Plus ROM under roms/ or set -Djmpe.rom=/path/to/rom to enable the boot smoke test."
        );

        int stepLimit = BootHarness.configuredStepLimit();
        BootHarness.BootRun run = BootHarness.runSteps(BootHarness.loadLocalMacPlusRom(romPath.get()), stepLimit);

        assertEquals(stepLimit, run.stepsCompleted());
        assertEquals(stepLimit, run.logs().size());
        assertTrue(run.reports().stream().allMatch(report -> report.success()));
    }

    private static byte[] romBytesWithProgramWords(int... words) {
        byte[] romBytes = new byte[0x0200];
        writeLong(romBytes, 0x0000, RESET_STACK_POINTER);
        writeLong(romBytes, 0x0004, RESET_PROGRAM_COUNTER);

        int programOffset = RESET_PROGRAM_COUNTER - BootHarness.DEFAULT_ROM_BASE;
        for (int index = 0; index < words.length; index++) {
            writeWord(romBytes, programOffset + (index * 2), words[index]);
        }
        return romBytes;
    }

    private static void writeWord(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 8);
        target[offset + 1] = (byte) value;
    }

    private static void writeLong(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }
}
