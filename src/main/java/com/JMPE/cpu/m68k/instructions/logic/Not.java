package com.JMPE.cpu.m68k.instructions.logic;

import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 NOT instruction.
 * This helper reads a decoded sized destination operand, writes the sized bitwise complement, and updates the CCR flags.
 * Instruction decoding, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Not {
    public static final int EXECUTION_CYCLES = 4;

    private Not() {
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
            Move.Size size,
            DestinationReader destinationReader,
            DestinationWriter destinationWriter,
            Move.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(~destinationValue);
        destinationWriter.write(result);
        Move.updateConditionCodes(result, size, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
