package com.JMPE.cpu;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

class Singlestep_Move_Test {
    @TestFactory
    Stream<DynamicNode> runsMoveFamilySmokeCasesFromExternalCorpus() {
        Path corpusDirectory = SinglestepTestSupport.requireCorpusDirectory();
        return SinglestepTestSupport.dynamicTests(corpusDirectory, SinglestepCorpusFiles.MOVE_FILES);
    }
}
