package com.JMPE.cpu.m68k.instructions.data;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 CLR instruction.
 * This helper writes a sized zero result to the decoded destination operand and updates the CCR flags.
 * Instruction decoding, effective-address resolution, and operand access remain CPU-core responsibilities.
 */
public final class Clr {
    public static final int EXECUTION_CYCLES = 4;

    private Clr() {
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public static int execute(
            Move.Size size,
            DestinationWriter destinationWriter,
            Move.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int result = size.mask(0);
        destinationWriter.write(result);
        Move.updateConditionCodes(result, size, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
