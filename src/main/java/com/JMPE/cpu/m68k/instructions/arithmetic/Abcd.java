package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the 68000 {@code ABCD} instruction helper.
 */
public final class Abcd {
    public static final int EXECUTION_CYCLES = 6;

    private Abcd() {
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

    public interface ConditionCodes {
        void clearZero();
        void setCarry(boolean value);
        void setExtend(boolean value);
    }

    public static int execute(
        Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        DestinationWriter destinationWriter,
        boolean extendSet,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        requireByteSize(size, "ABCD");

        PackedBcd.Result result = PackedBcd.add(
            Size.BYTE.mask(sourceReader.read()),
            Size.BYTE.mask(destinationReader.read()),
            extendSet
        );
        destinationWriter.write(result.value());
        if (result.value() != 0) {
            conditionCodes.clearZero();
        }
        conditionCodes.setCarry(result.carry());
        conditionCodes.setExtend(result.carry());
        return EXECUTION_CYCLES;
    }

    private static void requireByteSize(Size size, String operation) {
        if (size != Size.BYTE) {
            throw new IllegalArgumentException(operation + " on the Macintosh Plus 68000 supports only BYTE size");
        }
    }
}
