package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 JSR instruction.
 * This helper pushes a decoded return address and then writes the decoded target PC.
 * Decoding, return-address derivation, and effective-address resolution belong to the CPU core.
 */
public final class Jsr {
    public static final int EXECUTION_CYCLES = 16;

    private Jsr() {
    }

    @FunctionalInterface
    public interface StackPushLong {
        void push(int value);
    }

    public static int execute(
            int returnAddress,
            int targetAddress,
            StackPushLong stackPushLong,
            PcWriter pcWriter
    ) {
        Objects.requireNonNull(stackPushLong, "stackPushLong must not be null");
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");

        stackPushLong.push(returnAddress);
        pcWriter.write(targetAddress);
        return EXECUTION_CYCLES;
    }
}
