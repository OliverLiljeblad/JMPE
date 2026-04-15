package com.JMPE.bus;

import java.util.Objects;

/**
 * {@link MemoryRegion} adapter that maps a {@link Rom} into an
 * {@link AddressSpace}.
 *
 * <p>
 * {@link Rom} keeps its existing absolute-address API because that is already
 * used directly in reset-vector and ROM-loader tests. {@code RomRegion}
 * provides the offset-based {@link MemoryRegion} view that the bus needs,
 * without forcing a wider refactor of {@code Rom}.
 * </p>
 */
public final class RomRegion implements MemoryRegion {
    private final Rom rom;
    private final int mappedBase;
    private final int mappedSize;

    public RomRegion(Rom rom) {
        this(rom, Objects.requireNonNull(rom, "rom must not be null").baseAddress(), rom.size());
    }

    public RomRegion(Rom rom, int mappedBase) {
        this(rom, mappedBase, Objects.requireNonNull(rom, "rom must not be null").size());
    }

    public RomRegion(Rom rom, int mappedBase, int mappedSize) {
        this.rom = Objects.requireNonNull(rom, "rom must not be null");
        if (mappedSize < rom.size()) {
            throw new IllegalArgumentException("mappedSize must cover at least the backing ROM bytes");
        }
        this.mappedBase = mappedBase;
        this.mappedSize = mappedSize;
    }

    @Override
    public int base() {
        return mappedBase;
    }

    @Override
    public int size() {
        return mappedSize;
    }

    @Override
    public int readByte(int offset) {
        return rom.readByte(backingAddress(offset));
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

    @Override
    public void writeByte(int offset, int value) {
        rom.writeByte(mappedAddress(offset), value);
    }

    @Override
    public void writeWord(int offset, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(mappedAddress(offset)));
    }

    @Override
    public void writeLong(int offset, int value) {
        throw new UnsupportedOperationException(
            "Cannot write to ROM at address 0x" + Integer.toHexString(mappedAddress(offset)));
    }

<<<<<<< HEAD
    private int absoluteAddress(int offset) {
        return rom.base() + offset;
=======
    private int backingAddress(int offset) {
        return rom.baseAddress() + (offset % rom.size());
    }

    private int mappedAddress(int offset) {
        return mappedBase + offset;
>>>>>>> origin/main
    }
}
