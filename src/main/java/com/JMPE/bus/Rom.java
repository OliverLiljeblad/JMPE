package com.JMPE.bus;

import java.util.Arrays;

/**
 * Read-only memory region mapped into the emulator address space.
 * <p>
 * ROM bytes are immutable after construction so accidental writes fail fast.
 */
public final class Rom implements MemoryRegion {
    private static final int RESET_STACK_POINTER_OFFSET = 0;
    private static final int RESET_PROGRAM_COUNTER_OFFSET = 4;
    private static final int RESET_VECTOR_BYTES = 8;

    private final int base;
    private final byte[] bytes;

    public Rom(int base, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("ROM bytes must not be null or empty");
        }
        this.base = base;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public int base() {
        return base;
    }

    public int size() {
        return bytes.length;
    }

    public boolean contains(int address) {
        long offset = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(base);
        return offset >= 0 && offset < bytes.length;
    }

    public int readByte(int address) {
        return Byte.toUnsignedInt(bytes[offsetFor(address, 1)]);
    }

    public int readWord(int address) {
        int offset = offsetFor(address, 2);
        return (Byte.toUnsignedInt(bytes[offset]) << 8)
            | Byte.toUnsignedInt(bytes[offset + 1]);
    }

    public int readLong(int address) {
        int offset = offsetFor(address, 4);
        return (Byte.toUnsignedInt(bytes[offset]) << 24)
            | (Byte.toUnsignedInt(bytes[offset + 1]) << 16)
            | (Byte.toUnsignedInt(bytes[offset + 2]) << 8)
            | Byte.toUnsignedInt(bytes[offset + 3]);
    }

    /**
     * Reads the initial supervisor stack pointer from vector table offset 0x000000.
     */
    public int initialSupervisorStackPointer() {
        ensureHasResetVectors();
        return (int) readLong(base + RESET_STACK_POINTER_OFFSET);
    }

    /**
     * Reads the initial program counter from vector table offset 0x000004.
     */
    public int initialProgramCounter() {
        ensureHasResetVectors();
        return (int) readLong(base + RESET_PROGRAM_COUNTER_OFFSET);
    }

    /*
     * NOTE: ROM is immutable by design; Exceptions are for debug purposes.
     *       Expected behaviour of writing to ROM is NOP.
     */
    public void writeByte(int address, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(absoluteAddress(address)));
    }

    @Override
    public void writeWord(int offset, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(absoluteAddress(offset)));
    }

    @Override
    public void writeLong(int offset, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(absoluteAddress(offset)));
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    private int offsetFor(int address, int width) {
        long start = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(base);
        long endExclusive = start + width;
        if (start < 0 || endExclusive > bytes.length) {
            throw new IndexOutOfBoundsException(
                "ROM access out of bounds at address 0x" + Integer.toHexString(address));
        }
        return (int) start;
    }

    private void ensureHasResetVectors() {
        if (bytes.length < RESET_VECTOR_BYTES) {
            throw new IllegalStateException("ROM must contain at least 8 bytes for reset vectors");
        }
    }

    private int absoluteAddress(int offset) {
        return this.base + offset;
    }
}
