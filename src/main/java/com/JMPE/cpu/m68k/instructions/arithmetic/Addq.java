package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ADDQ instruction.
 * This helper adds a decoded quick value to a decoded sized destination, writes the sized result, and updates arithmetic CCR flags.
 * Instruction decoding, quick-value normalization, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Addq {
    public static final int EXECUTION_CYCLES = 4;
    private static final int MIN_QUICK_VALUE = 1;
    private static final int MAX_QUICK_VALUE = 8;

    private Addq() {
    }

    @FunctionalInterface
    public interface DestinationReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public interface ConditionCodes extends Move.ConditionCodes {
        void setExtend(boolean value);
    }

    public static int execute(
            Move.Size size,
            int quickValue,
            DestinationReader destinationReader,
            DestinationWriter destinationWriter,
            ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        validateQuickValue(quickValue);

        int sourceValue = size.mask(quickValue);
        int destinationValue = size.mask(destinationReader.read());
        int result = size.mask(destinationValue + sourceValue);
        destinationWriter.write(result);
        updateConditionCodes(size, sourceValue, destinationValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }

    private static void updateConditionCodes(
            Move.Size size,
            int sourceValue,
            int destinationValue,
            int result,
            ConditionCodes conditionCodes
    ) {
        boolean sourceNegative = size.isNegative(sourceValue);
        boolean destinationNegative = size.isNegative(destinationValue);
        boolean resultNegative = size.isNegative(result);
        long unsignedSource = Integer.toUnsignedLong(sourceValue);
        long unsignedDestination = Integer.toUnsignedLong(destinationValue);
        long maxValue = Integer.toUnsignedLong(size.mask(-1));
        boolean carry = (unsignedSource + unsignedDestination) > maxValue;
        boolean overflow = (sourceNegative == destinationNegative) && (resultNegative != destinationNegative);

        conditionCodes.setNegative(resultNegative);
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(overflow);
        conditionCodes.setCarry(carry);
        conditionCodes.setExtend(carry);
    }

    private static void validateQuickValue(int quickValue) {
        if (quickValue < MIN_QUICK_VALUE || quickValue > MAX_QUICK_VALUE) {
            throw new IllegalArgumentException("quickValue must be in range 1..8");
        }
    }
}
