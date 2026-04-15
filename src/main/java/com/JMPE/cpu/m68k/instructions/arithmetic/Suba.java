package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Implements the execution semantics of SUBA.
 *
 * <p>SUBA subtracts a word- or long-sized source from a full 32-bit address
 * register destination. Word sources are sign-extended before subtraction, and
 * the CCR is unaffected.</p>
 */
public final class Suba {
    public static final int EXECUTION_CYCLES = 8;

    private Suba() {
    }

    @FunctionalInterface
    public interface SourceReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationReader {
        int read();
    }

    @FunctionalInterface
    public interface DestinationWriter {
        void write(int value);
    }

    public static int execute(
        Size size,
        SourceReader sourceReader,
        DestinationReader destinationReader,
        DestinationWriter destinationWriter
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        Objects.requireNonNull(destinationReader, "destinationReader must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        if (size != Size.WORD && size != Size.LONG) {
            throw new IllegalArgumentException("SUBA must use WORD or LONG size");
        }

        int sourceValue = sourceReader.read();
        int destinationValue = destinationReader.read();
        int signedSource = size == Size.WORD
            ? (short) Size.WORD.mask(sourceValue)
            : sourceValue;

        destinationWriter.write(destinationValue - signedSource);
        return EXECUTION_CYCLES;
    }
}
