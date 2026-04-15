package com.JMPE.cpu.m68k.instructions.bit;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 BTST instruction.
 */
public final class Btst {
    public static final int EXECUTION_CYCLES = 4;

    private Btst() {
    }

    @FunctionalInterface
    public interface OperandReader {
        int read();
    }

    @FunctionalInterface
    public interface ZeroFlag {
        void setZero(boolean value);
    }

    public static int execute(Size size,
                              OperandReader bitNumberReader,
                              OperandReader destinationReader,
                              ZeroFlag zeroFlag) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(bitNumberReader, "bitNumberReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(zeroFlag, "zeroFlag must not be null");

        if (size != Size.BYTE && size != Size.LONG) {
            throw new IllegalArgumentException("BTST size must be BYTE or LONG");
        }

        int bitNumber = bitNumberReader.read();
        int value = size.mask(destinationReader.read());
        int bitIndex = bitNumber & (size == Size.LONG ? 31 : 7);

        zeroFlag.setZero(((value >>> bitIndex) & 1) == 0);
        return EXECUTION_CYCLES;
    }
}
