package com.JMPE.bus;

import java.util.Arrays;

/**
 * Read-only memory region mapped into the emulator address space.
 * <p>
 * ROM bytes are immutable after construction so accidental writes fail fast.
 */
public final class Rom {
    private final int baseAddress;
    private final byte[] bytes;

    public Rom(int baseAddress, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("ROM bytes must not be null or empty");
        }
        this.baseAddress = baseAddress;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public int baseAddress() {
        return baseAddress;
    }

    public int size() {
        return bytes.length;
    }

    public boolean contains(int address) {
        long offset = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(baseAddress);
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

    public long readLong(int address) {
        int offset = offsetFor(address, 4);
        return ((long) Byte.toUnsignedInt(bytes[offset]) << 24)
            | ((long) Byte.toUnsignedInt(bytes[offset + 1]) << 16)
            | ((long) Byte.toUnsignedInt(bytes[offset + 2]) << 8)
            | Byte.toUnsignedInt(bytes[offset + 3]);
    }

    /**
     * ROM is immutable by design; writes should be routed to RAM/MMIO regions instead.
     */
    public void writeByte(int address, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(address));
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    private int offsetFor(int address, int width) {
        long start = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(baseAddress);
        long endExclusive = start + width;
        if (start < 0 || endExclusive > bytes.length) {
            throw new IndexOutOfBoundsException(
                "ROM access out of bounds at address 0x" + Integer.toHexString(address));
        }
        return (int) start;
    }
}
