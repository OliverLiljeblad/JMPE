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

    public RomRegion(Rom rom) {
        this.rom = Objects.requireNonNull(rom, "rom must not be null");
    }

    @Override
    public int base() {
        return rom.baseAddress();
    }

    @Override
    public int size() {
        return rom.size();
    }

    @Override
    public int readByte(int offset) {
        return rom.readByte(absoluteAddress(offset));
    }

    @Override
    public int readWord(int offset) {
        return rom.readWord(absoluteAddress(offset));
    }

    @Override
    public int readLong(int offset) {
        return (int) rom.readLong(absoluteAddress(offset));
    }

    @Override
    public void writeByte(int offset, int value) {
        rom.writeByte(absoluteAddress(offset), value);
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

    private int absoluteAddress(int offset) {
        return rom.baseAddress() + offset;
    }
}
