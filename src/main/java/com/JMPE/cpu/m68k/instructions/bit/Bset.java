package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 BSET instruction.
 */
public final class Bset {
    public static final int EXECUTION_CYCLES = 4;

    private Bset() {
    }

    @FunctionalInterface
    public interface OperandReader {
        int read();
    }

    @FunctionalInterface
    public interface OperandWriter {
        void write(int value);
    }

    @FunctionalInterface
    public interface ZeroFlag {
        void setZero(boolean value);
    }

    public static int execute(
        Size size,
        OperandReader bitNumberReader,
        OperandReader destinationReader,
        OperandWriter destinationWriter,
        ZeroFlag zeroFlag
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(bitNumberReader, "bitNumberReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(zeroFlag, "zeroFlag must not be null");

        if (size != Size.BYTE && size != Size.LONG) {
            throw new IllegalArgumentException("BSET size must be BYTE or LONG");
        }

        int value = size.mask(destinationReader.read());
        int bitNumber = bitNumberReader.read();
        int bitIndex = bitNumber & (size == Size.LONG ? 31 : 7);
        int bitMask = 1 << bitIndex;

        zeroFlag.setZero((value & bitMask) == 0);
        destinationWriter.write(size.mask(value | bitMask));
        return EXECUTION_CYCLES;
    }
}
