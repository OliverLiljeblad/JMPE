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

class Singlestep_Branch_Test {
    private static final List<String> FILES = List.of(
        "Bcc.json.gz",
        "BSR.json.gz",
        "DBcc.json.gz",
        "JMP.json.gz",
        "JSR.json.gz"
    );

    @TestFactory
    Stream<DynamicTest> runsBranchFamilySmokeCasesFromExternalCorpus() {
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
