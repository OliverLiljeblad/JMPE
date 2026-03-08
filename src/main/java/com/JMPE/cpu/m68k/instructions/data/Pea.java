package com.JMPE.cpu.m68k.instructions.data;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 PEA instruction.
 * This helper pushes a decoded effective address as a longword onto the stack.
 * Effective-address calculation and stack access are handled by the CPU core.
 */
public final class Pea {
    public static final int EXECUTION_CYCLES = 12;

    private Pea() {
    }

    @FunctionalInterface
    public interface StackPushLong {
        void push(int value);
    }

    public static int execute(
            int effectiveAddress,
            StackPushLong stackPushLong
    ) {
        Objects.requireNonNull(stackPushLong, "stackPushLong must not be null");

        stackPushLong.push(effectiveAddress);
        return EXECUTION_CYCLES;
    }
}
