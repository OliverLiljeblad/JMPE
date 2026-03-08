package com.JMPE.cpu.m68k.instructions.control;

/**
 * Implements the execution semantics of the Motorola 68000 NOP instruction.
 * This helper performs no state changes and only returns the instruction cycle cost.
 * Instruction decoding and scheduling remain CPU-core responsibilities.
 */
public final class Nop {
    public static final int EXECUTION_CYCLES = 4;

    private Nop() {
    }

    public static int execute() {
        return EXECUTION_CYCLES;
    }
}
