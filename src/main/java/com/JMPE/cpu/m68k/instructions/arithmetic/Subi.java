package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 SUBI instruction.
 */
public final class Subi {
    public static final int EXECUTION_CYCLES_DN = 8;

    private Subi() {
    }

    @FunctionalInterface
    public interface SourceReader {
        int read();
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
        SourceReader sourceReader,
        DestinationReader destinationReader,
        DestinationWriter destinationWriter,
        Sub.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int sourceValue = size.mask(sourceReader.read());
        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(destinationValue - sourceValue);

        destinationWriter.write(result);
        Sub.updateConditionCodes(size, destinationValue, sourceValue, result, conditionCodes);
        return EXECUTION_CYCLES_DN;
    }
}
