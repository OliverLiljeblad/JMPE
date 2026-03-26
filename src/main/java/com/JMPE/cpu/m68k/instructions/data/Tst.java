package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 TST instruction.
 * This helper reads a decoded operand, masks it to the instruction size, and updates the CCR flags.
 * Instruction decoding, effective-address resolution, and operand access are handled by the CPU core.
 */
public final class Tst {
    public static final int EXECUTION_CYCLES = 4;

    private Tst() {
    }

    @FunctionalInterface
    public interface OperandReader {
        int read();
    }

    public static int execute(
            Size size,
            OperandReader reader,
            ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int value = size.mask(reader.read());
        Move.updateConditionCodes(value, size, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
