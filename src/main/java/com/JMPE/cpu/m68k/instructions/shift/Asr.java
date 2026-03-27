package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ASR instruction.
 * This helper arithmetically shifts a decoded sized destination to the right, writes the sized result, and updates the CCR flags.
 * Instruction decoding, count normalization, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Asr {
    public static final int EXECUTION_CYCLES = 4;

    private Asr() {
    }

    @FunctionalInterface
    public interface DestinationReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public interface ConditionCodes extends com.JMPE.cpu.m68k.instructions.ConditionCodes {
        void setExtend(boolean value);
    }

    public static int execute(
            Size size,
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

        int maskedValue = size.mask(destinationReader.read());
        int current = size.signExtend(maskedValue);
        boolean carry = false;
        if (count > 0) {
            for (int shift = 0; shift < count; shift++) {
                carry = (current & 1) != 0;
                current = current >> 1;
            }
        }

        int result = size.mask(current);
        destinationWriter.write(result);
        conditionCodes.setNegative(size.isNegative(result));
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(false);
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
