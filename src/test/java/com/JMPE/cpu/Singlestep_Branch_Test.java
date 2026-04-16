package com.JMPE.cpu;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

class Singlestep_Branch_Test {
    @TestFactory
    Stream<DynamicNode> runsBranchFamilySmokeCasesFromExternalCorpus() {
        Path corpusDirectory = SinglestepTestSupport.requireCorpusDirectory();
        return SinglestepTestSupport.dynamicTests(corpusDirectory, SinglestepCorpusFiles.BRANCH_FILES);
    }
}
