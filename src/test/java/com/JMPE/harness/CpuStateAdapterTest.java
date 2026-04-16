package com.JMPE.harness;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CpuStateAdapterTest {
    @Test
    void executesInstructionSeededOnlyFromPrefetch() {
        SingleStepLoader.CaseSpec caseSpec = new SingleStepLoader.CaseSpec(
            "NOP.json.gz",
            new SingleStepLoader.SingleStepCase(
                "4e71 [NOP] synthetic",
                snapshot(0x0000_1000L, 0x0000, new int[] {0x4E71, 0x0000}),
                snapshot(0x0000_1002L, 0x0000, new int[] {0x0000, 0x0000}),
                4
            )
        );

        CpuStateAdapter.RunResult result = CpuStateAdapter.execute(caseSpec);

        CpuStateAdapter.assertArchitectedState(caseSpec, result);
        MemoryAsserts.assertFinalRamMatches(caseSpec, result.bus(), result.lastLog());
        assertNotNull(result.lastLog());
        assertTrue(result.lastLog().contains("op=NOP"), result.lastLog());
    }

    private static SingleStepLoader.Snapshot snapshot(long pc, int sr, int[] prefetch) {
        return new SingleStepLoader.Snapshot(
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0,
            0x0000_3000L,
            0x0000_2000L,
            sr,
            pc,
            prefetch,
            new int[0][]
        );
    }
}
