package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of Motorola 68000 Bcc conditional branch instructions.
 * This helper evaluates a decoded condition code from supplied CCR readers and branches by
 * writing PC + displacement only when the condition is true.
 * Decoding and displacement/effective-address preparation remain CPU-core responsibilities.
 */
public final class Bcc {
    public static final int EXECUTION_CYCLES = 10;

    private Bcc() {
    }

    public enum Condition {
        HI, LS, CC, CS, NE, EQ, VC, VS, PL, MI, GE, LT, GT, LE
    }

    public interface ConditionCodesReader {
        boolean isNegative();

        boolean isZero();

        boolean isOverflow();

        boolean isCarry();
    }

    public static int execute(
            Condition condition,
            int pc,
            int displacement,
            ConditionCodesReader conditionCodesReader,
            PcWriter pcWriter
    ) {
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(conditionCodesReader, "conditionCodesReader must not be null");
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");

        if (isConditionTrue(condition, conditionCodesReader)) {
            pcWriter.write(pc + displacement);
        }
        return EXECUTION_CYCLES;
    }

    public static boolean isConditionTrue(Condition condition, ConditionCodesReader conditionCodesReader) {
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(conditionCodesReader, "conditionCodesReader must not be null");

        boolean n = conditionCodesReader.isNegative();
        boolean z = conditionCodesReader.isZero();
        boolean v = conditionCodesReader.isOverflow();
        boolean c = conditionCodesReader.isCarry();
        return switch (condition) {
            case HI -> !c && !z;
            case LS -> c || z;
            case CC -> !c;
            case CS -> c;
            case NE -> !z;
            case EQ -> z;
            case VC -> !v;
            case VS -> v;
            case PL -> !n;
            case MI -> n;
            case GE -> n == v;
            case LT -> n != v;
            case GT -> !z && (n == v);
            case LE -> z || (n != v);
        };
    }
}
