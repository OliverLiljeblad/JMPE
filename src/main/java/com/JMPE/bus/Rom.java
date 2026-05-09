package com.JMPE.bus;

import java.util.Arrays;

/**
 * Read-only memory region mapped into the emulator address space.
 *
 * <p>All read/write methods accept <em>local offsets</em> from {@link #base()},
 * honouring the {@link MemoryRegion} contract.  When the aperture is larger
 * than the backing data (e.g. 128 KB ROM in a 1 MB address window) reads
 * wrap naturally — mirroring the real chip behaviour where only the lower
 * address lines are connected.
 *
 * <p>Writes are silent NOPs, matching real hardware where the ROM chip
 * simply ignores write cycles (no write-enable signal).
 */
public final class Rom implements MemoryRegion {
    private static final int RESET_STACK_POINTER_OFFSET = 0;
    private static final int RESET_PROGRAM_COUNTER_OFFSET = 4;
    private static final int RESET_VECTOR_BYTES = 8;

    private final int base;
    private final byte[] bytes;
    private final int apertureSize;

    public Rom(int base, byte[] bytes) {
        this(base, bytes, bytes == null ? 0 : bytes.length);
    }

    public Rom(int base, byte[] bytes, int apertureSize) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("ROM bytes must not be null or empty");
        }
        if (apertureSize < bytes.length) {
            throw new IllegalArgumentException("aperture must be at least as large as the backing data");
        }
        this.base = base;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.apertureSize = apertureSize;
    }

    @Override
    public int base() {
        return base;
    }

    @Override
    public int size() {
        return apertureSize;
    }

    public int backingSize() {
        return bytes.length;
    }

    public boolean contains(int address) {
        long offset = Integer.toUnsignedLong(address) - Integer.toUnsignedLong(base);
        return offset >= 0 && offset < apertureSize;
    }

    @Override
    public int readByte(int offset) {
        return Byte.toUnsignedInt(bytes[offset % bytes.length]);
    }

    @Override
    public int readWord(int offset) {
        return (readByte(offset) << 8)
            | readByte(offset + 1);
    }

    @Override
    public int readLong(int offset) {
        return (readByte(offset) << 24)
            | (readByte(offset + 1) << 16)
            | (readByte(offset + 2) << 8)
            | readByte(offset + 3);
    }

    /**
     * Reads the initial supervisor stack pointer from vector table offset 0x000000.
     */
    public int initialSupervisorStackPointer() {
        ensureHasResetVectors();
        return readLong(RESET_STACK_POINTER_OFFSET);
    }

    /**
     * Reads the initial program counter from vector table offset 0x000004.
     */
    public int initialProgramCounter() {
        ensureHasResetVectors();
        return readLong(RESET_PROGRAM_COUNTER_OFFSET);
    }

    @Override
    public void writeByte(int offset, int value) { /* ROM ignores writes */ }

    @Override
    public void writeWord(int offset, int value) { /* ROM ignores writes */ }

    @Override
    public void writeLong(int offset, int value) { /* ROM ignores writes */ }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    private void ensureHasResetVectors() {
        if (bytes.length < RESET_VECTOR_BYTES) {
            throw new IllegalStateException("ROM must contain at least 8 bytes for reset vectors");
        }
    }
}
