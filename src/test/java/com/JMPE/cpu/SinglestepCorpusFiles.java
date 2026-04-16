package com.JMPE.cpu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

final class SinglestepCorpusFiles {
    static final List<String> ADD_FILES = List.of(
        "ADD.b.json.gz",
        "ADD.w.json.gz",
        "ADD.l.json.gz",
        "ADDA.w.json.gz",
        "ADDA.l.json.gz",
        "ADDX.b.json.gz",
        "ADDX.w.json.gz",
        "ADDX.l.json.gz"
    );

    static final List<String> MOVE_FILES = List.of(
        "MOVE.b.json.gz",
        "MOVE.w.json.gz",
        "MOVE.l.json.gz",
        "MOVE.q.json.gz",
        "MOVEA.w.json.gz",
        "MOVEA.l.json.gz",
        "MOVEM.w.json.gz",
        "MOVEM.l.json.gz",
        "MOVEP.w.json.gz",
        "MOVEP.l.json.gz"
    );

    static final List<String> BRANCH_FILES = List.of(
        "Bcc.json.gz",
        "BSR.json.gz",
        "DBcc.json.gz",
        "JMP.json.gz",
        "JSR.json.gz"
    );

    private static final Set<String> WIRED_FILE_SET = Stream.of(ADD_FILES, MOVE_FILES, BRANCH_FILES)
        .collect(LinkedHashSet::new, LinkedHashSet::addAll, LinkedHashSet::addAll);

    private SinglestepCorpusFiles() {
    }

    static List<String> remainingFiles(Path corpusDirectory) {
        Objects.requireNonNull(corpusDirectory, "corpusDirectory must not be null");

        try (Stream<Path> paths = Files.list(corpusDirectory)) {
            return paths
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(fileName -> fileName.endsWith(".json.gz"))
                .filter(fileName -> !WIRED_FILE_SET.contains(fileName))
                .sorted()
                .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                "Failed to enumerate single-step corpus files in " + corpusDirectory.toAbsolutePath().normalize(),
                exception
            );
        }
    }
}
