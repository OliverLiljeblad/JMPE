package com.JMPE.cpu;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

class Singlestep_Add_Test {
    @TestFactory
    Stream<DynamicNode> runsAddFamilySmokeCasesFromExternalCorpus() {
        Path corpusDirectory = SinglestepTestSupport.requireCorpusDirectory();
        return SinglestepTestSupport.dynamicTests(corpusDirectory, SinglestepCorpusFiles.ADD_FILES);
    }
}
