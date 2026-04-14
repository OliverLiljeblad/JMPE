package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Implements the execution semantics of the Motorola 68000 SWAP instruction.
 */
public final class Swap {
    public static final int EXECUTION_CYCLES = 4;

    private Swap() {
    }

    public static int execute(
        int registerValue,
        IntConsumer destinationWriter,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int result = (registerValue << 16) | ((registerValue >>> 16) & 0xFFFF);
        destinationWriter.accept(result);
        Move.updateConditionCodes(result, Size.LONG, conditionCodes);
        return EXECUTION_CYCLES;
    }
}
