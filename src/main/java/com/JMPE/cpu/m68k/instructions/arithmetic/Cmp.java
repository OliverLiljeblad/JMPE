package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 CMP instruction.
 * This helper subtracts a decoded sized source from a decoded sized destination and updates the CCR flags without storing the result.
 * Instruction decoding, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Cmp {
    public static final int EXECUTION_CYCLES = 4;

    private Cmp() {
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
        Move.Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        Move.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int sourceValue = size.mask(sourceReader.read());
        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(destinationValue - sourceValue);
        Sub.updateConditionCodes(size, destinationValue, sourceValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
