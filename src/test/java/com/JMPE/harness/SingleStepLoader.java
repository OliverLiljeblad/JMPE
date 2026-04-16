package com.JMPE.harness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public final class SingleStepLoader {
    public static final int DEFAULT_CASE_LIMIT = 25;

    private static final String ENABLE_PROPERTY = "jmpe.680x0.enable";
    private static final String ENABLE_ENV = "JMPE_680X0_ENABLE";
    private static final String CYCLE_PROPERTY = "jmpe.680x0.cycles";
    private static final String CYCLE_ENV = "JMPE_680X0_CYCLES";
    private static final String CORPUS_PROPERTY = "jmpe.680x0.dir";
    private static final String CORPUS_ENV = "JMPE_680X0_DIR";
    private static final String CASE_LIMIT_PROPERTY = "jmpe.680x0.cases";
    private static final String CASE_LIMIT_ENV = "JMPE_680X0_CASES";
    private static final Path DEFAULT_CORPUS_DIRECTORY = Path.of(
        System.getProperty("user.home"),
        "cpu-testdata",
        "680x0",
        "68000",
        "v1"
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SingleStepLoader() {
    }

    public static boolean isEnabled() {
        String raw = System.getProperty(ENABLE_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(ENABLE_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    public static String disabledMessage() {
        return "Set -D" + ENABLE_PROPERTY + "=true to enable the external 68000 conformance smoke tests.";
    }

    public static boolean compareCycles() {
        String raw = System.getProperty(CYCLE_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(CYCLE_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    public static Optional<Path> findLocalCorpus() {
        Optional<Path> propertyDirectory = configuredDirectory(System.getProperty(CORPUS_PROPERTY),
            "system property " + CORPUS_PROPERTY);
        if (propertyDirectory.isPresent()) {
            return propertyDirectory;
        }

        Optional<Path> envDirectory = configuredDirectory(System.getenv(CORPUS_ENV),
            "environment variable " + CORPUS_ENV);
        if (envDirectory.isPresent()) {
            return envDirectory;
        }

        if (Files.isDirectory(DEFAULT_CORPUS_DIRECTORY)) {
            return Optional.of(DEFAULT_CORPUS_DIRECTORY.toAbsolutePath().normalize());
        }
        return Optional.empty();
    }

    public static int configuredCaseLimit() {
        String raw = System.getProperty(CASE_LIMIT_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(CASE_LIMIT_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_CASE_LIMIT;
        }

        String normalized = raw.trim();
        if (normalized.equalsIgnoreCase("all")) {
            return Integer.MAX_VALUE;
        }

        try {
            int caseLimit = Integer.parseInt(normalized);
            if (caseLimit <= 0) {
                throw new IllegalArgumentException(CASE_LIMIT_PROPERTY + " must be a positive integer or 'all'");
            }
            return caseLimit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(CASE_LIMIT_PROPERTY + " must be a positive integer or 'all'",
                exception);
        }
    }

    public static String missingCorpusMessage() {
        return "Clone SingleStepTests/680x0 to ~/cpu-testdata/680x0 or set -D"
            + CORPUS_PROPERTY + "=/path/to/68000/v1 to enable the external 68000 conformance smoke tests.";
    }

    public static List<CaseSpec> loadCaseSpecs(Path corpusDirectory, String... fileNames) {
        return loadCaseSpecs(corpusDirectory, configuredCaseLimit(), List.of(fileNames));
    }

    public static List<CaseSpec> loadCaseSpecs(Path corpusDirectory, int caseLimit, List<String> fileNames) {
        Objects.requireNonNull(corpusDirectory, "corpusDirectory must not be null");
        Objects.requireNonNull(fileNames, "fileNames must not be null");
        if (caseLimit <= 0) {
            throw new IllegalArgumentException("caseLimit must be positive");
        }

        List<CaseSpec> loadedCases = new ArrayList<>();
        for (String fileName : fileNames) {
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("fileNames must not contain blank values");
            }
            loadedCases.addAll(loadCases(corpusDirectory.resolve(fileName), caseLimit));
        }
        return List.copyOf(loadedCases);
    }

    private static List<CaseSpec> loadCases(Path file, int caseLimit) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Single-step corpus file is missing: " + file.toAbsolutePath().normalize());
        }

        List<CaseSpec> cases = new ArrayList<>(Math.min(caseLimit, DEFAULT_CASE_LIMIT));
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(file));
             JsonParser parser = OBJECT_MAPPER.createParser(gzipInputStream)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Single-step corpus file does not contain a JSON array: "
                    + file.toAbsolutePath().normalize());
            }

            while (parser.nextToken() != JsonToken.END_ARRAY && cases.size() < caseLimit) {
                cases.add(new CaseSpec(file.getFileName().toString(), OBJECT_MAPPER.readValue(parser, SingleStepCase.class)));
            }
            return List.copyOf(cases);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load single-step corpus file " + file.toAbsolutePath().normalize(),
                exception);
        }
    }

    private static Optional<Path> configuredDirectory(String rawPath, String sourceName) {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty();
        }

        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(sourceName + " points to a missing directory: " + path);
        }
        return Optional.of(path);
    }

    public record CaseSpec(String fileName, SingleStepCase testCase) {
        public String displayName() {
            return fileName + " :: " + testCase.name();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SingleStepCase(
        String name,
        Snapshot initial,
        @JsonProperty("final") Snapshot finalState,
        int length
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snapshot(
        long d0,
        long d1,
        long d2,
        long d3,
        long d4,
        long d5,
        long d6,
        long d7,
        long a0,
        long a1,
        long a2,
        long a3,
        long a4,
        long a5,
        long a6,
        long usp,
        long ssp,
        int sr,
        long pc,
        int[] prefetch,
        int[][] ram
    ) {
    }
}
