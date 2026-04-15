package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 SUBX instruction.
 */
public final class Subx {
    public static final int EXECUTION_CYCLES = 4;

    private Subx() {
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
        boolean extendSet,
        boolean zeroSet,
        Sub.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int sourceValue = size.mask(sourceReader.read());
        int destinationValue = size.mask(destinationReader.read());
        int extend = extendSet ? 1 : 0;
        int result = size.mask(destinationValue - sourceValue - extend);
        destinationWriter.write(result);
        updateConditionCodes(size, sourceValue, destinationValue, result, extend, zeroSet, conditionCodes);
        return EXECUTION_CYCLES;
    }

    private static void updateConditionCodes(
        Size size,
        int sourceValue,
        int destinationValue,
        int result,
        int extend,
        boolean zeroSet,
        Sub.ConditionCodes conditionCodes
    ) {
        long unsignedDestination = Integer.toUnsignedLong(destinationValue);
        long unsignedSubtrahend = Integer.toUnsignedLong(sourceValue) + extend;
        boolean carry = unsignedDestination < unsignedSubtrahend;

        long signedResult = (long) size.signExtend(destinationValue) - size.signExtend(sourceValue) - extend;
        boolean overflow = signedResult < signedMin(size) || signedResult > signedMax(size);

        conditionCodes.setNegative(size.isNegative(result));
        conditionCodes.setZero(zeroSet && size.isZero(result));
        conditionCodes.setOverflow(overflow);
        conditionCodes.setCarry(carry);
        conditionCodes.setExtend(carry);
    }

    private static long signedMin(Size size) {
        return switch (size) {
            case BYTE -> Byte.MIN_VALUE;
            case WORD -> Short.MIN_VALUE;
            case LONG -> Integer.MIN_VALUE;
            case UNSIZED -> throw new IllegalStateException("UNSIZED does not support signed bounds");
        };
    }

    private static long signedMax(Size size) {
        return switch (size) {
            case BYTE -> Byte.MAX_VALUE;
            case WORD -> Short.MAX_VALUE;
            case LONG -> Integer.MAX_VALUE;
            case UNSIZED -> throw new IllegalStateException("UNSIZED does not support signed bounds");
        };
    }
}
