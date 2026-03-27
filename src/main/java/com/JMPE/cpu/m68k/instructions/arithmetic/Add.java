package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ADD instruction.
 * This helper reads decoded sized operands, writes the sized sum, and updates the exposed CCR flags.
 * Instruction decoding, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Add {
    public static final int EXECUTION_CYCLES = 4;

    private Add() {
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
     * Extends the shared NZVC contract to include the X (Extend) flag, which ADD must set equal to carry.
     * This mirrors the Addq.ConditionCodes pattern and is required for correct multi-precision arithmetic (e.g., ADDX).
     */
    public interface ConditionCodes extends com.JMPE.cpu.m68k.instructions.ConditionCodes {
        void setExtend(boolean value);
    }

    public static int execute(
        Size size,
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
        int result = size.mask(destinationValue + sourceValue);
        destinationWriter.write(result);
        updateConditionCodes(size, sourceValue, destinationValue, result, conditionCodes);
        return EXECUTION_CYCLES;
    }

    private static void updateConditionCodes(
        Size size,
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
}
