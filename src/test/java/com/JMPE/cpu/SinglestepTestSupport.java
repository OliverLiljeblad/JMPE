package com.JMPE.cpu;

import com.JMPE.harness.CpuStateAdapter;
import com.JMPE.harness.MemoryAsserts;
import com.JMPE.harness.SingleStepLoader;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SinglestepTestSupport {
    private SinglestepTestSupport() {
    }

    static Path requireCorpusDirectory() {
        assumeTrue(SingleStepLoader.isEnabled(), SingleStepLoader.disabledMessage());
        Optional<Path> corpusDirectory = SingleStepLoader.findLocalCorpus();
        assumeTrue(corpusDirectory.isPresent(), SingleStepLoader.missingCorpusMessage());
        return corpusDirectory.get();
    }

    static Stream<DynamicNode> dynamicTests(Path corpusDirectory, List<String> fileNames) {
        return fileNames.stream()
            .map(fileName -> DynamicContainer.dynamicContainer(
                fileName,
                dynamicTestsForFile(corpusDirectory, fileName)
            ));
    }

    private static Stream<DynamicTest> dynamicTestsForFile(Path corpusDirectory, String fileName) {
        return SingleStepLoader.loadCaseSpecs(corpusDirectory, fileName)
            .stream()
            .map(caseSpec -> DynamicTest.dynamicTest(caseSpec.displayName(), () -> {
                CpuStateAdapter.RunResult result = CpuStateAdapter.execute(caseSpec);
                CpuStateAdapter.assertArchitectedState(caseSpec, result);
                MemoryAsserts.assertFinalRamMatches(caseSpec, result.bus(), result.lastLog());
            }));
    }
}
