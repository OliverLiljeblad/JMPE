package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ROL instruction.
 * This helper rotates a decoded sized destination to the left, writes the sized result, and updates the CCR flags.
 * Instruction decoding, count normalization, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Rol {
    public static final int EXECUTION_CYCLES = 4;

    private Rol() {
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
            Move.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        validateCount(count);

        int result = size.mask(destinationReader.read());
        boolean carry = false;
        if (count > 0) {
            for (int rotate = 0; rotate < count; rotate++) {
                carry = size.isNegative(result);
                result = size.mask(result << 1);
                if (carry) {
                    result |= 1;
                }
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
