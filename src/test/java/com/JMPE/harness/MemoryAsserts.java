package com.JMPE.harness;

import com.JMPE.bus.Bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MemoryAsserts {
    private MemoryAsserts() {
    }

    public static void assertFinalRamMatches(SingleStepLoader.CaseSpec caseSpec, Bus bus, String lastLog) {
        int[][] expectedRam = caseSpec.testCase().finalState().ram();
        if (expectedRam == null) {
            return;
        }

        for (int[] cell : expectedRam) {
            if (cell == null || cell.length != 2) {
                throw new IllegalArgumentException("Single-step RAM cell must contain [address, byte]");
            }

            int address = cell[0];
            int expected = cell[1] & 0xFF;
            int actual = bus.readByte(address);
            assertEquals(expected, actual, DiffPrinter.memoryMismatch(caseSpec, address, expected, actual, lastLog));
        }
    }
}
