package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 JMP instruction.
 * This helper writes a decoded absolute target address to the program counter.
 * Decoding and effective-address calculation are performed by the CPU core.
 */
public final class Jmp {
    public static final int EXECUTION_CYCLES = 8;

    private Jmp() {
    }

    public static int execute(int targetAddress, PcWriter pcWriter) {
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");
        pcWriter.write(targetAddress);
        return EXECUTION_CYCLES;
    }
}
