package com.JMPE.cpu.m68k.instructions.shift;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ROXL instruction.
 *
 * <p>ROXL rotates the destination to the left through the extend bit. X and C
 * are updated from the last bit shifted out, while V is always cleared.</p>
 */
public final class Roxl {
    public static final int EXECUTION_CYCLES = 4;

    private Roxl() {
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
        boolean extendInput,
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
        boolean extend = extendInput;
        boolean carry = false;
        if (count > 0) {
            for (int rotate = 0; rotate < count; rotate++) {
                carry = size.isNegative(result);
                result = size.mask(result << 1);
                if (extend) {
                    result |= 1;
                }
                extend = carry;
            }
        }

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
