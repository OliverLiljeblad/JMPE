package com.JMPE.cpu.m68k.instructions.control;

/**
 * Implements the CPU-visible execution semantics of the Motorola 68000 RESET instruction.
 *
 * <p>The external device reset line is not modeled yet, so this helper currently captures the
 * CPU-side behavior only: the instruction is privileged, does not change registers or flags, and
 * consumes its execution cost.</p>
 */
public final class Reset {
    public static final int EXECUTION_CYCLES = 132;

    private Reset() {
    }

    public static int execute() {
        return EXECUTION_CYCLES;
    }
}
