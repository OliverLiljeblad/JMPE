package com.JMPE.cpu;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SinglestepRemainingShards {
    private static final int SHARD_COUNT = 11;

    private SinglestepRemainingShards() {
    }

    static Stream<DynamicNode> dynamicTests(int shardIndex) {
        if (shardIndex < 0 || shardIndex >= SHARD_COUNT) {
            throw new IllegalArgumentException("shardIndex must be in range 0.." + (SHARD_COUNT - 1));
        }

        Path corpusDirectory = SinglestepTestSupport.requireCorpusDirectory();
        List<String> remainingFiles = SinglestepCorpusFiles.remainingFiles(corpusDirectory);
        assumeTrue(!remainingFiles.isEmpty(), "No remaining external corpus files were found.");

        int start = remainingFiles.size() * shardIndex / SHARD_COUNT;
        int end = remainingFiles.size() * (shardIndex + 1) / SHARD_COUNT;
        List<String> shardFiles = remainingFiles.subList(start, end);
        assumeTrue(!shardFiles.isEmpty(), "No files were assigned to remaining shard " + shardIndex + ".");
        return SinglestepTestSupport.dynamicTests(corpusDirectory, shardFiles);
    }
}

class Singlestep_Remaining_01_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard01() {
        return SinglestepRemainingShards.dynamicTests(0);
    }
}

class Singlestep_Remaining_02_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard02() {
        return SinglestepRemainingShards.dynamicTests(1);
    }
}

class Singlestep_Remaining_03_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard03() {
        return SinglestepRemainingShards.dynamicTests(2);
    }
}

class Singlestep_Remaining_04_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard04() {
        return SinglestepRemainingShards.dynamicTests(3);
    }
}

class Singlestep_Remaining_05_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard05() {
        return SinglestepRemainingShards.dynamicTests(4);
    }
}

class Singlestep_Remaining_06_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard06() {
        return SinglestepRemainingShards.dynamicTests(5);
    }
}

class Singlestep_Remaining_07_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard07() {
        return SinglestepRemainingShards.dynamicTests(6);
    }
}

class Singlestep_Remaining_08_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard08() {
        return SinglestepRemainingShards.dynamicTests(7);
    }
}

class Singlestep_Remaining_09_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard09() {
        return SinglestepRemainingShards.dynamicTests(8);
    }
}

class Singlestep_Remaining_10_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard10() {
        return SinglestepRemainingShards.dynamicTests(9);
    }
}

class Singlestep_Remaining_11_Test {
    @TestFactory
    Stream<DynamicNode> runsRemainingCorpusShard11() {
        return SinglestepRemainingShards.dynamicTests(10);
    }
}
