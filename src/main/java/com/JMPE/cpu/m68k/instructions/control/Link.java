package com.JMPE.cpu.m68k.instructions.control;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 LINK instruction.
 */
public final class Link {
    public static final int EXECUTION_CYCLES = 16;

    private Link() {
    }

    @FunctionalInterface
    public interface StackPointerReader {
        int read();
    }

    @FunctionalInterface
    public interface StackPointerWriter {
        void write(int value);
    }

    @FunctionalInterface
    public interface StackLongWriter {
        void write(int address, int value);
    }

    @FunctionalInterface
    public interface AddressRegisterWriter {
        void write(int value);
    }

    public static int execute(
        int addressRegisterValue,
        int displacement,
        StackPointerReader stackPointerReader,
        StackPointerWriter stackPointerWriter,
        StackLongWriter stackLongWriter,
        AddressRegisterWriter addressRegisterWriter
    ) {
        Objects.requireNonNull(stackPointerReader, "stackPointerReader must not be null");
        Objects.requireNonNull(stackPointerWriter, "stackPointerWriter must not be null");
        Objects.requireNonNull(stackLongWriter, "stackLongWriter must not be null");
        Objects.requireNonNull(addressRegisterWriter, "addressRegisterWriter must not be null");

        int linkedStackPointer = stackPointerReader.read() - Integer.BYTES;
        stackPointerWriter.write(linkedStackPointer);
        stackLongWriter.write(linkedStackPointer, addressRegisterValue);
        addressRegisterWriter.write(linkedStackPointer);
        stackPointerWriter.write(linkedStackPointer + displacement);
        return EXECUTION_CYCLES;
    }
}
