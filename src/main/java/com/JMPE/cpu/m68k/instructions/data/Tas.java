package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 TAS instruction.
 */
public final class Tas {
    public static final int EXECUTION_CYCLES = 4;

    private Tas() {
    }

    @FunctionalInterface
    public interface OperandReader {
        int read();
    }

    @FunctionalInterface
    public interface OperandWriter {
        void write(int value);
    }

    public static int execute(OperandReader reader, OperandWriter writer, ConditionCodes conditionCodes) {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int value = Size.BYTE.mask(reader.read());
        Move.updateConditionCodes(value, Size.BYTE, conditionCodes);
        writer.write(value | 0x80);
        return EXECUTION_CYCLES;
    }
}
