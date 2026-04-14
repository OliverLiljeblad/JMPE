package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of Motorola 68000 DBcc instructions.
 */
public final class Dbcc {
    public static final int EXECUTION_CYCLES = 10;

    private Dbcc() {
    }

    @FunctionalInterface
    public interface CounterReader {
        int read();
    }

    @FunctionalInterface
    public interface CounterWriter {
        void write(int value);
    }

    public static int execute(
        int rawCondition,
        int branchBase,
        int displacement,
        CounterReader counterReader,
        CounterWriter counterWriter,
        Bcc.ConditionCodesReader conditionCodesReader,
        PcWriter pcWriter
    ) {
        Objects.requireNonNull(counterReader, "counterReader must not be null");
        Objects.requireNonNull(counterWriter, "counterWriter must not be null");
        Objects.requireNonNull(conditionCodesReader, "conditionCodesReader must not be null");
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");

        if (isConditionTrue(rawCondition, conditionCodesReader)) {
            return EXECUTION_CYCLES;
        }

        int counter = counterReader.read();
        int decremented = ((counter & 0xFFFF) - 1) & 0xFFFF;
        counterWriter.write((counter & 0xFFFF_0000) | decremented);
        if (decremented != 0xFFFF) {
            pcWriter.write(branchBase + displacement);
        }
        return EXECUTION_CYCLES;
    }

    static boolean isConditionTrue(int rawCondition, Bcc.ConditionCodesReader conditionCodesReader) {
        return switch (rawCondition) {
            case 0x0 -> true;
            case 0x1 -> false;
            case 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF ->
                Bcc.isConditionTrue(Bcc.Condition.values()[rawCondition - 0x2], conditionCodesReader);
            default -> throw new IllegalArgumentException("DBcc condition must be in range 0x0..0xF");
        };
    }
}
