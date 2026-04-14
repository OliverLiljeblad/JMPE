package com.JMPE.cpu.m68k.instructions.control;

import java.util.Objects;

/**
 * Implements the execution semantics of Motorola 68000 Scc instructions.
 */
public final class Scc {
    public static final int EXECUTION_CYCLES = 4;

    private Scc() {
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public static int execute(boolean conditionTrue, DestinationWriter destinationWriter) {
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");

        destinationWriter.write(conditionTrue ? 0xFF : 0x00);
        return EXECUTION_CYCLES;
    }
}
