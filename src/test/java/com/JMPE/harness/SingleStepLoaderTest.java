package com.JMPE.harness;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

class SingleStepLoaderTest {
    private static final String ENABLE_PROPERTY = "jmpe.680x0.enable";
    private static final String CYCLE_PROPERTY = "jmpe.680x0.cycles";
    private static final String CASE_LIMIT_PROPERTY = "jmpe.680x0.cases";

    @TempDir
    Path tempDir;

    @Test
    void loadCaseSpecsReadsGzippedJsonAndRespectsCaseLimit() throws IOException {
        Path corpusFile = tempDir.resolve("NOP.json.gz");
        writeGzip(
            corpusFile,
            """
                [
                  {
                    "name": "4e71 [NOP] 1",
                    "initial": {
                      "d0": 0, "d1": 0, "d2": 0, "d3": 0, "d4": 0, "d5": 0, "d6": 0, "d7": 0,
                      "a0": 0, "a1": 0, "a2": 0, "a3": 0, "a4": 0, "a5": 0, "a6": 0,
                      "usp": 4096, "ssp": 8192, "sr": 0, "pc": 12288,
                      "prefetch": [20081, 0],
                      "ram": []
                    },
                    "final": {
                      "d0": 0, "d1": 0, "d2": 0, "d3": 0, "d4": 0, "d5": 0, "d6": 0, "d7": 0,
                      "a0": 0, "a1": 0, "a2": 0, "a3": 0, "a4": 0, "a5": 0, "a6": 0,
                      "usp": 4096, "ssp": 8192, "sr": 0, "pc": 12290,
                      "prefetch": [0, 0],
                      "ram": []
                    },
                    "length": 4
                  },
                  {
                    "name": "4e71 [NOP] 2",
                    "initial": {
                      "d0": 1, "d1": 0, "d2": 0, "d3": 0, "d4": 0, "d5": 0, "d6": 0, "d7": 0,
                      "a0": 0, "a1": 0, "a2": 0, "a3": 0, "a4": 0, "a5": 0, "a6": 0,
                      "usp": 4096, "ssp": 8192, "sr": 0, "pc": 16384,
                      "prefetch": [20081, 0],
                      "ram": []
                    },
                    "final": {
                      "d0": 1, "d1": 0, "d2": 0, "d3": 0, "d4": 0, "d5": 0, "d6": 0, "d7": 0,
                      "a0": 0, "a1": 0, "a2": 0, "a3": 0, "a4": 0, "a5": 0, "a6": 0,
                      "usp": 4096, "ssp": 8192, "sr": 0, "pc": 16386,
                      "prefetch": [0, 0],
                      "ram": []
                    },
                    "length": 4
                  }
                ]
                """
        );

        List<SingleStepLoader.CaseSpec> caseSpecs =
            SingleStepLoader.loadCaseSpecs(tempDir, 1, List.of("NOP.json.gz"));

        assertEquals(1, caseSpecs.size());
        assertEquals("NOP.json.gz", caseSpecs.getFirst().fileName());
        assertEquals("4e71 [NOP] 1", caseSpecs.getFirst().testCase().name());
        assertArrayEquals(new int[] {0x4E71, 0}, caseSpecs.getFirst().testCase().initial().prefetch());
    }

    @Test
    void featureFlagsDefaultToDisabledAndReadSystemProperties() {
        withSystemProperty(ENABLE_PROPERTY, null, () -> {
            withSystemProperty(CYCLE_PROPERTY, null, () -> {
                assertFalse(SingleStepLoader.isEnabled());
                assertFalse(SingleStepLoader.compareCycles());
            });
        });

        withSystemProperty(ENABLE_PROPERTY, "true", () -> assertTrue(SingleStepLoader.isEnabled()));
        withSystemProperty(CYCLE_PROPERTY, "true", () -> assertTrue(SingleStepLoader.compareCycles()));
    }

    @Test
    void configuredCaseLimitSupportsDefaultIntegerAndAll() {
        withSystemProperty(CASE_LIMIT_PROPERTY, null,
            () -> assertEquals(SingleStepLoader.DEFAULT_CASE_LIMIT, SingleStepLoader.configuredCaseLimit()));
        withSystemProperty(CASE_LIMIT_PROPERTY, "7",
            () -> assertEquals(7, SingleStepLoader.configuredCaseLimit()));
        withSystemProperty(CASE_LIMIT_PROPERTY, "all",
            () -> assertEquals(Integer.MAX_VALUE, SingleStepLoader.configuredCaseLimit()));
    }

    private static void writeGzip(Path file, String content) throws IOException {
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(file));
             OutputStreamWriter writer = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static void withSystemProperty(String key, String value, ThrowingRunnable runnable) {
        String previous = System.getProperty(key);
        try {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
            runnable.run();
        } catch (Exception exception) {
            throw new AssertionError("System property test helper failed for " + key, exception);
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
