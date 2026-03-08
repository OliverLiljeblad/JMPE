package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 RTS instruction.
 * This helper pops a decoded return address from the stack and writes it to the program counter.
 * Instruction decoding and stack/register access are handled by the CPU core.
 */
public final class Rts {
    public static final int EXECUTION_CYCLES = 16;

    private Rts() {
    }

    @FunctionalInterface
    public interface StackPopLong {
        int pop();
    }

    public static int execute(StackPopLong stackPopLong, PcWriter pcWriter) {
        Objects.requireNonNull(stackPopLong, "stackPopLong must not be null");
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");

        int returnAddress = stackPopLong.pop();
        pcWriter.write(returnAddress);
        return EXECUTION_CYCLES;
    }
}
