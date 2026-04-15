package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Implements the execution semantics of the Motorola 68000 EXT instruction.
 */
public final class Ext {
    public static final int EXECUTION_CYCLES = 4;

    private Ext() {
    }

    public static int execute(
        Size size,
        int registerValue,
        IntConsumer destinationWriter,
        ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        int result = size.mask(sourceSize(size).signExtend(registerValue));
        destinationWriter.accept(result);
        Move.updateConditionCodes(result, size, conditionCodes);
        return EXECUTION_CYCLES;
    }

    private static Size sourceSize(Size resultSize) {
        return switch (resultSize) {
            case WORD -> Size.BYTE;
            case LONG -> Size.WORD;
            default -> throw new IllegalArgumentException("EXT size must be WORD or LONG");
        };
    }
}
