package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

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
        Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int sourceValue = size.mask(sourceReader.read());
        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(destinationValue - sourceValue);
        updateConditionCodes(size, destinationValue, sourceValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }

    /**
     * Updates N, Z, V, C flags for CMP.
     * <p>
     * CMP intentionally does NOT set X (Extend); that flag is only modified by SUB, SUBQ, SUBX, and similar.
     * </p>
     */
    static void updateConditionCodes(
        Size size,
        int destinationValue,
        int sourceValue,
        int result,
        ConditionCodes conditionCodes
    ) {
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
    }
}
