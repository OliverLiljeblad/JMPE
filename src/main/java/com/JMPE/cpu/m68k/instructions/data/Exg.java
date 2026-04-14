package com.JMPE.cpu.m68k.instructions.data;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 EXG instruction.
 */
public final class Exg {
    public static final int EXECUTION_CYCLES = 6;

    private Exg() {
    }

    @FunctionalInterface
    public interface RegisterReader {
        int read();
    }

    @FunctionalInterface
    public interface RegisterWriter {
        void write(int value);
    }

    public static int execute(
        RegisterReader sourceReader,
        RegisterReader destinationReader,
        RegisterWriter sourceWriter,
        RegisterWriter destinationWriter
    ) {
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(sourceWriter, "sourceWriter must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");

        int sourceValue = sourceReader.read();
        int destinationValue = destinationReader.read();
        sourceWriter.write(destinationValue);
        destinationWriter.write(sourceValue);
        return EXECUTION_CYCLES;
    }
}
