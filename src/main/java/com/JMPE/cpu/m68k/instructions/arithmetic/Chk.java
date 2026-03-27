package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the Motorola 68000 {@code CHK <ea>,Dn} instruction helper for the Mac Plus target.
 *
 * <p>
 * The Macintosh Plus uses a 68000 CPU, so only the word-sized {@code CHK.W} form is valid here.
 * The 68020+ long-sized variant is intentionally rejected by this helper.
 * </p>
 */
public final class Chk {
    public static final int CHK_EXCEPTION_VECTOR = 6;

    public static final int EXECUTION_CYCLES = 10;

    private Chk() {
    }

    @FunctionalInterface
    public interface OperandReader {
        int read(int eaMode, int eaReg, Size size);
    }

    @FunctionalInterface
    public interface RegisterReader {
        int read(int dn);
    }

    @FunctionalInterface
    public interface ExceptionTrigger {
        void trigger(int exceptionNumber);
    }

    public interface ConditionCodes {
        void setN(boolean value);
    }

    public static int execute(
        int eaMode,
        int eaReg,
        int dn,
        Size size,
        OperandReader operandReader,
        RegisterReader registerReader,
        ExceptionTrigger exceptionTrigger,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(operandReader, "operandReader must not be null");
        Objects.requireNonNull(registerReader, "registerReader must not be null");
        Objects.requireNonNull(exceptionTrigger, "exceptionTrigger must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        requireMacPlusWordSize(size);

        int upperBound = Size.WORD.signExtend(operandReader.read(eaMode, eaReg, Size.WORD));
        int regVal = Size.WORD.signExtend(registerReader.read(dn));

        if (regVal < 0) {
            conditionCodes.setN(true);
            exceptionTrigger.trigger(CHK_EXCEPTION_VECTOR);
        } else if (regVal > upperBound) {
            conditionCodes.setN(false);
            exceptionTrigger.trigger(CHK_EXCEPTION_VECTOR);
        }

        return EXECUTION_CYCLES;
    }

    private static void requireMacPlusWordSize(Size size) {
        if (size != Size.WORD) {
            throw new IllegalArgumentException("CHK on the Macintosh Plus 68000 supports only WORD size");
        }
    }
}
