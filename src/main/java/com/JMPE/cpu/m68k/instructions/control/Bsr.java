package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 BSR instruction.
 * This helper pushes a decoded return address and then branches by applying a decoded displacement to the supplied PC.
 * Instruction decoding and displacement/return-address calculation are handled by the CPU core.
 */
public final class Bsr {
    public static final int EXECUTION_CYCLES = 18;

    private Bsr() {
    }

    @FunctionalInterface
    public interface StackPushLong {
        void push(int value);
    }

    public static int execute(
            int pc,
            int displacement,
            int returnAddress,
            StackPushLong stackPushLong,
            PcWriter pcWriter
    ) {
        Objects.requireNonNull(stackPushLong, "stackPushLong must not be null");
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");

        stackPushLong.push(returnAddress);
        pcWriter.write(pc + displacement);
        return EXECUTION_CYCLES;
    }
}
