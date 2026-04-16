package com.JMPE.cpu;

import com.JMPE.harness.CpuStateAdapter;
import com.JMPE.harness.MemoryAsserts;
import com.JMPE.harness.SingleStepLoader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class Singlestep_Add_Test {
    private static final List<String> FILES = List.of(
        "ADD.b.json.gz",
        "ADD.w.json.gz",
        "ADD.l.json.gz",
        "ADDA.w.json.gz",
        "ADDA.l.json.gz",
        "ADDX.b.json.gz",
        "ADDX.w.json.gz",
        "ADDX.l.json.gz"
    );

    @TestFactory
    Stream<DynamicTest> runsAddFamilySmokeCasesFromExternalCorpus() {
        assumeTrue(SingleStepLoader.isEnabled(), SingleStepLoader.disabledMessage());
        Optional<Path> corpusDirectory = SingleStepLoader.findLocalCorpus();
        assumeTrue(corpusDirectory.isPresent(), SingleStepLoader.missingCorpusMessage());

        return SingleStepLoader.loadCaseSpecs(corpusDirectory.get(), FILES.toArray(String[]::new))
            .stream()
            .map(caseSpec -> DynamicTest.dynamicTest(caseSpec.displayName(), () -> {
                CpuStateAdapter.RunResult result = CpuStateAdapter.execute(caseSpec);
                CpuStateAdapter.assertArchitectedState(caseSpec, result);
                MemoryAsserts.assertFinalRamMatches(caseSpec, result.bus(), result.lastLog());
            }));
    }
}
