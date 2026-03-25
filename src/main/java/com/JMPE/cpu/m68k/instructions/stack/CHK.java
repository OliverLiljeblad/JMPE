// 
// CHK - Check Register Against Bounds

// Syntax: CHK <ea>, Dn
// Sizes: Word (.W) and Long (.L) [.L only on 68020+]

package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

public final class CHK {

    public static final int EXECUTION_CYCLES = 10;

    private CHK() {
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

        int upperBound = operandReader.read(eaMode, eaReg, size);
        int regVal = registerReader.read(dn);

        if (size == Size.WORD || size == Size.LONG) {
            upperBound = signExtendWord(upperBound);
            regVal = signExtendWord(regVal);
        }

        if (regVal < 0) {
            conditionCodes.setN(true);
            exceptionTrigger.trigger(6);
        } else if (regVal > upperBound) {
            conditionCodes.setN(false);
            exceptionTrigger.trigger(6);
        }

        return EXECUTION_CYCLES;
    }

    private static int signExtendWord(int value) {
        return (value << 16) >> 16;
    }
}
