package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.DivideByZeroException;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the 68000 {@code DIVU.W <ea>,Dn} instruction helper.
 */
public final class Divu {
    public static final int EXECUTION_CYCLES = 12;

    private static final long MAX_UNSIGNED_QUOTIENT = 0xFFFFL;

    private Divu() {
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

        long divisor = Integer.toUnsignedLong(Size.WORD.mask(sourceReader.read()));
        if (divisor == 0) {
            throw new DivideByZeroException();
        }

        long dividend = Integer.toUnsignedLong(destinationReader.read());
        long quotient = dividend / divisor;
        if (quotient > MAX_UNSIGNED_QUOTIENT) {
            conditionCodes.setOverflow(true);
            conditionCodes.setCarry(false);
            return EXECUTION_CYCLES;
        }

        long remainder = dividend % divisor;
        int packedResult = ((int) remainder << Short.SIZE) | ((int) quotient & 0xFFFF);
        destinationWriter.write(packedResult);
        conditionCodes.setNegative((quotient & 0x8000L) != 0);
        conditionCodes.setZero((quotient & 0xFFFFL) == 0);
        conditionCodes.setOverflow(false);
        conditionCodes.setCarry(false);
        return EXECUTION_CYCLES;
    }

    private static void requireWordSize(Size size) {
        if (size != Size.WORD) {
            throw new IllegalArgumentException("DIVU on the Macintosh Plus 68000 supports only WORD size");
        }
    }
}
