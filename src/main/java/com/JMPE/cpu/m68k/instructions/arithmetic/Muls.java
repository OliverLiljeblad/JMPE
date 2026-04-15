package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the 68000 {@code MULS.W <ea>,Dn} instruction helper.
 */
public final class Muls {
    public static final int EXECUTION_CYCLES = 8;

    private Muls() {
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
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        requireWordSize(size);

        int sourceValue = Size.WORD.signExtend(sourceReader.read());
        int destinationValue = Size.WORD.signExtend(destinationReader.read());
        int result = sourceValue * destinationValue;
        destinationWriter.write(result);
        conditionCodes.setNegative(result < 0);
        conditionCodes.setZero(result == 0);
        conditionCodes.setOverflow(false);
        conditionCodes.setCarry(false);
        return EXECUTION_CYCLES;
    }

    private static void requireWordSize(Size size) {
        if (size != Size.WORD) {
            throw new IllegalArgumentException("MULS on the Macintosh Plus 68000 supports only WORD size");
        }
    }
}
