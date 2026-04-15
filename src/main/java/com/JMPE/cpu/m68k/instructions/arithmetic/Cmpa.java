package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the execution semantics of CMPA.
 */
public final class Cmpa {
    public static final int EXECUTION_CYCLES = Cmp.EXECUTION_CYCLES;

    private Cmpa() {
    }

    @FunctionalInterface
    public interface SourceReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationReader {
        int read();
    }

    public static int execute(
        Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        if (size != Size.WORD && size != Size.LONG) {
            throw new IllegalArgumentException("CMPA must use WORD or LONG size");
        }

        int sourceValue = sourceReader.read();
        int destinationValue = destinationReader.read();
        int signedSource = size == Size.WORD
            ? (short) Size.WORD.mask(sourceValue)
            : sourceValue;
        int result = destinationValue - signedSource;
        Cmp.updateConditionCodes(Size.LONG, destinationValue, signedSource, result, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
