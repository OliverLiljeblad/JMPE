package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ASL instruction.
 * This helper arithmetically shifts a decoded sized destination to the left, writes the sized result, and updates the CCR flags.
 * Instruction decoding, count normalization, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Asl {
    public static final int EXECUTION_CYCLES = 4;

    private Asl() {
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
            int count,
            DestinationReader destinationReader,
            DestinationWriter destinationWriter,
            ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        validateCount(count);

        int result = size.mask(destinationReader.read());
        boolean carry = false;
        boolean overflow = false;
        if (count > 0) {
            for (int shift = 0; shift < count; shift++) {
                boolean signBeforeShift = size.isNegative(result);
                carry = signBeforeShift;
                result = size.mask(result << 1);
                boolean signAfterShift = size.isNegative(result);
                if (signBeforeShift != signAfterShift) {
                    overflow = true;
                }
            }
        }

        destinationWriter.write(result);
        conditionCodes.setNegative(size.isNegative(result));
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(overflow);
        conditionCodes.setCarry(carry);
        if (count > 0) {
            conditionCodes.setExtend(carry);
        }
        return EXECUTION_CYCLES;
    }

    private static void validateCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
    }
}
