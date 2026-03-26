package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Shared helper for sized data-register writes in the dispatch layer.
 *
 * <p>
 * BYTE and WORD writes preserve the unaffected upper bits of the destination data register, while
 * LONG writes replace the whole register.
 * </p>
 */
public final class DataRegisterWriter {
    private DataRegisterWriter() {
    }

    public static void write(M68kCpu cpu, int register, Size size, int value) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(size, "size must not be null");

        int currentValue = cpu.registers().data(register);
        int maskedValue = switch (size) {
            case BYTE, WORD, LONG -> size.mask(value);
            case UNSIZED -> throw new IllegalArgumentException("Data-register writes require a sized operation");
        };

        int nextValue = switch (size) {
            case BYTE -> (currentValue & 0xFFFF_FF00) | maskedValue;
            case WORD -> (currentValue & 0xFFFF_0000) | maskedValue;
            case LONG -> maskedValue;
            case UNSIZED -> throw new IllegalArgumentException("Data-register writes require a sized operation");
        };
        cpu.registers().setData(register, nextValue);
    }
}
