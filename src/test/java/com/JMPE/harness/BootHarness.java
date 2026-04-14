package com.JMPE.harness;

import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.machine.MacPlusMachine;
import com.JMPE.util.RomLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class BootHarness {
    public static final int DEFAULT_ROM_BASE = 0x0040_0000;
    public static final int DEFAULT_RAM_BASE = 0x0000_0000;
    public static final int DEFAULT_RAM_SIZE = 0x0040_0000;
    public static final int DEFAULT_STEP_LIMIT = 32;
    private static final int MAC_PLUS_ROM_BYTES = 0x0002_0000;

    private static final String ROM_PROPERTY = "jmpe.rom";
    private static final String ROM_ENV = "JMPE_ROM";
    private static final String STEP_PROPERTY = "jmpe.boot.steps";
    private static final String STEP_ENV = "JMPE_BOOT_STEPS";
    private static final Path ROMS_DIRECTORY = Path.of("roms");

    private BootHarness() {
    }

    public static Optional<Path> findLocalRom() {
        Optional<Path> propertyRom = configuredRomPath(System.getProperty(ROM_PROPERTY), "system property " + ROM_PROPERTY);
        if (propertyRom.isPresent()) {
            return propertyRom;
        }

        Optional<Path> envRom = configuredRomPath(System.getenv(ROM_ENV), "environment variable " + ROM_ENV);
        if (envRom.isPresent()) {
            return envRom;
        }

        if (!Files.isDirectory(ROMS_DIRECTORY)) {
            return Optional.empty();
        }

        try (Stream<Path> entries = Files.list(ROMS_DIRECTORY)) {
            return entries
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().startsWith("."))
                .sorted()
                .findFirst()
                .map(Path::toAbsolutePath)
                .map(Path::normalize);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to scan roms/ for a local ROM", exception);
        }
    }

    public static int configuredStepLimit() {
        String raw = System.getProperty(STEP_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(STEP_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_STEP_LIMIT;
        }

        try {
            int stepLimit = Integer.parseInt(raw.trim());
            if (stepLimit <= 0) {
                throw new IllegalArgumentException(STEP_PROPERTY + " must be a positive integer");
            }
            return stepLimit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(STEP_PROPERTY + " must be a positive integer", exception);
        }
    }

    public static MacPlusMachine machineFromRomBytes(byte[] romBytes, int baseAddress) {
        return MacPlusMachine.fromRomBytes(romBytes, baseAddress, new Ram(DEFAULT_RAM_BASE, DEFAULT_RAM_SIZE));
    }

    public static MacPlusMachine loadLocalMacPlusRom(Path romPath) throws IOException {
        byte[] bytes = Files.readAllBytes(romPath);
        if (bytes.length < MAC_PLUS_ROM_BYTES) {
            throw new IllegalArgumentException("Mac Plus ROM must contain at least 128 KB of ROM data");
        }

        return MacPlusMachine.bootMachine(
            RomLoader.fromBytes(Arrays.copyOf(bytes, MAC_PLUS_ROM_BYTES), DEFAULT_ROM_BASE),
            new Ram(DEFAULT_RAM_BASE, DEFAULT_RAM_SIZE)
        );
    }

    public static BootRun runSteps(MacPlusMachine machine, int stepCount) {
        if (machine == null) {
            throw new IllegalArgumentException("machine must not be null");
        }
        if (stepCount <= 0) {
            throw new IllegalArgumentException("stepCount must be positive");
        }

        List<M68kCpu.StepReport> reports = new ArrayList<>(stepCount);
        List<String> logs = new ArrayList<>(stepCount);

        for (int step = 0; step < stepCount; step++) {
            try {
                reports.add(machine.step(logs::add));
            } catch (IllegalInstructionException | RuntimeException exception) {
                throw new AssertionError(
                    "Boot step " + step
                        + " failed at pc=" + String.format("0x%08X", machine.cpu().registers().programCounter())
                        + " after " + reports.size() + " completed steps. Last log: " + lastLog(logs),
                    exception
                );
            }
        }

        return new BootRun(List.copyOf(reports), List.copyOf(logs));
    }

    public record BootRun(List<M68kCpu.StepReport> reports, List<String> logs) {
        public int stepsCompleted() {
            return reports.size();
        }
    }

    private static Optional<Path> configuredRomPath(String rawPath, String sourceName) {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty();
        }

        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(sourceName + " points to a missing ROM file: " + path);
        }
        return Optional.of(path);
    }

    private static String lastLog(List<String> logs) {
        if (logs.isEmpty()) {
            return "<none>";
        }
        return logs.get(logs.size() - 1);
    }
}
