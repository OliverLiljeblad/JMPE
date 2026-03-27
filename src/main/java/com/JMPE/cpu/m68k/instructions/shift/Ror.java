package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ROR instruction.
 * This helper rotates a decoded sized destination to the right, writes the sized result, and updates the CCR flags.
 * Instruction decoding, count normalization, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Ror {
    public static final int EXECUTION_CYCLES = 4;

    private Ror() {
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
        int signBitMask = size.signBitMask();
        boolean carry = false;
        if (count > 0) {
            for (int rotate = 0; rotate < count; rotate++) {
                carry = (result & 1) != 0;
                result = result >>> 1;
                if (carry) {
                    result |= signBitMask;
                }
                result = size.mask(result);
            }
        }

        destinationWriter.write(result);
        conditionCodes.setNegative(size.isNegative(result));
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(false);
        conditionCodes.setCarry(carry);
        return EXECUTION_CYCLES;
    }

    private static void validateCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
    }
}
