package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.arithmetic.Add; 
import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 ADDI instruction.
 * ADDI adds an immediate value to a destination operand and updates the CCR.
 */
public final class Addi {

    public static final int EXECUTION_CYCLES_DN = 8;

    private Addi() {}

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
        Add.ConditionCodes conditionCodes          // reuse Add.ConditionCodes directly
    ) {
        Objects.requireNonNull(size,              "size must not be null");
        Objects.requireNonNull(sourceReader,      "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes,    "conditionCodes must not be null");

        int srcValue = size.mask(sourceReader.read());
        int dstValue = size.mask(destinationReader.read());
        int result   = size.mask(dstValue + srcValue);

        destinationWriter.write(result);
        updateConditionCodes(size, srcValue, dstValue, result, conditionCodes);

        return EXECUTION_CYCLES_DN;
    }

    private static void updateConditionCodes(
        Size size,
        int srcValue,
        int dstValue,
        int result,
        Add.ConditionCodes conditionCodes
    ) {
        boolean srcNeg    = size.isNegative(srcValue);
        boolean dstNeg    = size.isNegative(dstValue);
        boolean resultNeg = size.isNegative(result);

        long unsignedSrc = Integer.toUnsignedLong(srcValue);
        long unsignedDst = Integer.toUnsignedLong(dstValue);
        long maxValue    = Integer.toUnsignedLong(size.mask(-1));

        boolean carry    = (unsignedSrc + unsignedDst) > maxValue;
        boolean overflow = (srcNeg == dstNeg) && (resultNeg != dstNeg);

        conditionCodes.setNegative(resultNeg);
        conditionCodes.setZero(size.isZero(result));
        conditionCodes.setOverflow(overflow);
        conditionCodes.setCarry(carry);
        conditionCodes.setExtend(carry);
    }
}
