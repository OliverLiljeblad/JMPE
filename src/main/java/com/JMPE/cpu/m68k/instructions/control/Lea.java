package com.JMPE.cpu.m68k.instructions.control;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 LEA instruction.
 * This helper writes a decoded effective address into a decoded address register destination.
 * Effective-address calculation and register selection remain CPU-core responsibilities.
 */
public final class Lea {
    public static final int EXECUTION_CYCLES = 4;

    private Lea() {
    }

    @FunctionalInterface
    public interface AddressRegisterWriter {
        void write(int registerIndex, int value);
    }

    public static int execute(
            int registerIndex,
            int effectiveAddress,
            AddressRegisterWriter writer
    ) {
        Objects.requireNonNull(writer, "writer must not be null");

        writer.write(registerIndex, effectiveAddress);
        return EXECUTION_CYCLES;
    }
}
