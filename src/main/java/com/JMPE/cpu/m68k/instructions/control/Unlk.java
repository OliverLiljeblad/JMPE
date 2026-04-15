package com.JMPE.cpu.m68k.instructions.control;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 UNLK instruction.
 */
public final class Unlk {
    public static final int EXECUTION_CYCLES = 12;

    private Unlk() {
    }

    @FunctionalInterface
    public interface StackPointerWriter {
        void write(int value);
    }

    @FunctionalInterface
    public interface StackLongReader {
        int read(int address);
    }

    @FunctionalInterface
    public interface AddressRegisterWriter {
        void write(int value);
    }

    public static int execute(
        int addressRegisterValue,
        StackPointerWriter stackPointerWriter,
        StackLongReader stackLongReader,
        AddressRegisterWriter addressRegisterWriter
    ) {
        Objects.requireNonNull(stackPointerWriter, "stackPointerWriter must not be null");
        Objects.requireNonNull(stackLongReader, "stackLongReader must not be null");
        Objects.requireNonNull(addressRegisterWriter, "addressRegisterWriter must not be null");

        stackPointerWriter.write(addressRegisterValue);
        int restoredAddressRegisterValue = stackLongReader.read(addressRegisterValue);
        addressRegisterWriter.write(restoredAddressRegisterValue);
        stackPointerWriter.write(addressRegisterValue + Integer.BYTES);
        return EXECUTION_CYCLES;
    }
}
