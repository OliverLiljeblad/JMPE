package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 NEG instruction.
 */
public final class Neg {
    public static final int EXECUTION_CYCLES = 4;

    private Neg() {
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
        Sub.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(-destinationValue);
        destinationWriter.write(result);
        Sub.updateConditionCodes(size, 0, destinationValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
