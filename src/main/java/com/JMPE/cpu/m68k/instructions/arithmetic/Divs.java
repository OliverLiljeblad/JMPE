package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.DivideByZeroException;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the 68000 {@code DIVS.W <ea>,Dn} instruction helper.
 */
public final class Divs {
    public static final int EXECUTION_CYCLES = 12;

    private Divs() {
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

        int divisor = Size.WORD.signExtend(sourceReader.read());
        if (divisor == 0) {
            throw new DivideByZeroException();
        }

        int dividend = destinationReader.read();
        long quotient = (long) dividend / divisor;
        if (quotient < Short.MIN_VALUE || quotient > Short.MAX_VALUE) {
            conditionCodes.setOverflow(true);
            conditionCodes.setCarry(false);
            return EXECUTION_CYCLES;
        }

        long remainder = (long) dividend % divisor;
        int packedResult = (((int) remainder & 0xFFFF) << Short.SIZE) | ((int) quotient & 0xFFFF);
        destinationWriter.write(packedResult);
        conditionCodes.setNegative((((int) quotient) & 0x8000) != 0);
        conditionCodes.setZero((((int) quotient) & 0xFFFF) == 0);
        conditionCodes.setOverflow(false);
        conditionCodes.setCarry(false);
        return EXECUTION_CYCLES;
    }

    private static void requireWordSize(Size size) {
        if (size != Size.WORD) {
            throw new IllegalArgumentException("DIVS on the Macintosh Plus 68000 supports only WORD size");
        }
    }
}
