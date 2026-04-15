package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 NEGX instruction.
 */
public final class Negx {
    public static final int EXECUTION_CYCLES = Subx.EXECUTION_CYCLES;

    private Negx() {
    }

    @FunctionalInterface
    public interface DestinationReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public static int execute(
        Size size,
        DestinationReader destinationReader,
        DestinationWriter destinationWriter,
        boolean extendSet,
        boolean zeroSet,
        Sub.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        return Subx.execute(
            size,
            destinationReader::read,
            () -> 0,
            destinationWriter::write,
            extendSet,
            zeroSet,
            conditionCodes
        );
    }
}
