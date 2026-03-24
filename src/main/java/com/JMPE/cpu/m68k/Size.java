package com.JMPE.cpu.m68k;

/**
 * Shared operand-size model used by the decoder and instruction executors.
 *
 * <p>
 * Sized instructions use {@link #BYTE}, {@link #WORD}, or {@link #LONG}.
 * {@link #UNSIZED} is reserved for instructions like {@code NOP}, {@code RTS},
 * and {@code JMP} that do not operate on byte/word/long data.
 * </p>
 */
public enum Size {
    BYTE(1, 0x0000_00FF, 0x0000_0080),
    WORD(2, 0x0000_FFFF, 0x0000_8000),
    LONG(4, 0xFFFF_FFFF, 0x8000_0000),
    UNSIZED(0, 0, 0);

    private final int bytes;
    private final int mask;
    private final int signBitMask;

    Size(int bytes, int mask, int signBitMask) {
        this.bytes = bytes;
        this.mask = mask;
        this.signBitMask = signBitMask;
    }

    public boolean isSized() {
        return this != UNSIZED;
    }

    public int bytes() {
        ensureSizedOperation("byte width");
        return bytes;
    }

    public int bits() {
        return bytes() * Byte.SIZE;
    }

    public int mask(int value) {
        ensureSizedOperation("bit mask");
        return value & mask;
    }

    public int signBitMask() {
        ensureSizedOperation("sign bit");
        return signBitMask;
    }

    public int signExtend(int value) {
        return switch (this) {
            case BYTE -> (byte) value;
            case WORD -> (short) value;
            case LONG -> value;
            case UNSIZED -> throw unsupported("sign extension");
        };
    }

    public boolean isNegative(int value) {
        return (mask(value) & signBitMask()) != 0;
    }

    public boolean isZero(int value) {
        return mask(value) == 0;
    }

    private void ensureSizedOperation(String operation) {
        if (!isSized()) {
            throw unsupported(operation);
        }
    }

    private IllegalStateException unsupported(String operation) {
        return new IllegalStateException("UNSIZED does not support " + operation);
    }
}
