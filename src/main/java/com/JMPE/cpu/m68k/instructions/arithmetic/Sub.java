package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 SUB instruction.
 * This helper reads decoded sized operands, writes the sized difference, and updates the exposed CCR flags.
 * Instruction decoding, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Sub {
    public static final int EXECUTION_CYCLES = 4;

    private Sub() {
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

    /**
     * CCR mutator surface for SUB behavior.
     * <p>
     * SUB sets N, Z, V, C from the subtraction result, and also sets X (Extend) equal to C (Carry).
     * CMP uses a separate code path and does NOT set X.
     * </p>
     */
    public interface ConditionCodes extends Move.ConditionCodes {
        void setExtend(boolean value);
    }

    public static int execute(
        Move.Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        DestinationWriter destinationWriter,
        ConditionCodes conditionCodes
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
        updateConditionCodes(size, destinationValue, sourceValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }

    static void updateConditionCodes(
        Move.Size size,
        int destinationValue,
        int sourceValue,
        int result,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        boolean sourceNegative = size.isNegative(sourceValue);
        boolean destinationNegative = size.isNegative(destinationValue);
        boolean resultNegative = size.isNegative(result);
        long unsignedSource = Integer.toUnsignedLong(sourceValue);
        long unsignedDestination = Integer.toUnsignedLong(destinationValue);
        boolean carry = unsignedSource > unsignedDestination;
        boolean overflow = (destinationNegative != sourceNegative) && (resultNegative != destinationNegative);

        conditionCodes.setNegative(resultNegative);
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(overflow);
        conditionCodes.setCarry(carry);
        // X (Extend) is set identical to C (Carry) for SUB
        conditionCodes.setExtend(carry);
    }
}
