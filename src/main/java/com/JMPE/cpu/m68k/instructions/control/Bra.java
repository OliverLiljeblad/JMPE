package com.JMPE.cpu.m68k.instructions.control;

import com.JMPE.cpu.m68k.instructions.common.PcWriter;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 BRA instruction.
 * This helper applies a decoded signed displacement to the supplied PC and writes the result.
 * Instruction decoding and displacement extraction are handled by the CPU core.
 */
public final class Bra {
    public static final int EXECUTION_CYCLES = 10;

    private Bra() {
    }

    public static int execute(int pc, int displacement, PcWriter pcWriter) {
        Objects.requireNonNull(pcWriter, "pcWriter must not be null");
        pcWriter.write(pc + displacement);
        return EXECUTION_CYCLES;
    }
}
