package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the 68000 {@code MOVEP} instruction helper.
 */
public final class Movep {
    public static final int EXECUTION_CYCLES = 8;

    private Movep() {
    }

    @FunctionalInterface
    public interface DataReader {
        int read();
    }

    @FunctionalInterface
    public interface DataWriter {
        void write(int value);
    }

    @FunctionalInterface
    public interface ByteReader {
        int read(int address);
    }

    @FunctionalInterface
    public interface ByteWriter {
        void write(int address, int value);
    }

    public static int moveMemoryToRegister(Size size, int address, ByteReader byteReader, DataWriter dataWriter) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(byteReader, "byteReader must not be null");
        Objects.requireNonNull(dataWriter, "dataWriter must not be null");
        requireWordOrLong(size);

        int value = switch (size) {
            case WORD -> ((byteReader.read(address) & 0xFF) << 8)
                | (byteReader.read(address + 2) & 0xFF);
            case LONG -> ((byteReader.read(address) & 0xFF) << 24)
                | ((byteReader.read(address + 2) & 0xFF) << 16)
                | ((byteReader.read(address + 4) & 0xFF) << 8)
                | (byteReader.read(address + 6) & 0xFF);
            case BYTE, UNSIZED -> throw new IllegalArgumentException("MOVEP supports only WORD or LONG size");
        };
        dataWriter.write(value);
        return EXECUTION_CYCLES;
    }

    public static int moveRegisterToMemory(Size size, DataReader dataReader, int address, ByteWriter byteWriter) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(dataReader, "dataReader must not be null");
        Objects.requireNonNull(byteWriter, "byteWriter must not be null");
        requireWordOrLong(size);

        int value = dataReader.read();
        switch (size) {
            case WORD -> {
                byteWriter.write(address, (value >>> 8) & 0xFF);
                byteWriter.write(address + 2, value & 0xFF);
            }
            case LONG -> {
                byteWriter.write(address, (value >>> 24) & 0xFF);
                byteWriter.write(address + 2, (value >>> 16) & 0xFF);
                byteWriter.write(address + 4, (value >>> 8) & 0xFF);
                byteWriter.write(address + 6, value & 0xFF);
            }
            case BYTE, UNSIZED -> throw new IllegalArgumentException("MOVEP supports only WORD or LONG size");
        }
        return EXECUTION_CYCLES;
    }

    private static void requireWordOrLong(Size size) {
        if (size != Size.WORD && size != Size.LONG) {
            throw new IllegalArgumentException("MOVEP supports only WORD or LONG size");
        }
    }
}
