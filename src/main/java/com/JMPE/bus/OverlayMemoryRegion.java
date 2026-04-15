package com.JMPE.bus;

import java.util.Objects;

/**
 * Low-memory aperture that switches between boot ROM and writable backing RAM.
 *
 * <p>Classic compact Macs start with overlay enabled so low memory reads come
 * from ROM. Once the VIA clears the overlay bit, the same address range
 * exposes RAM instead.</p>
 */
public final class OverlayMemoryRegion implements MemoryRegion {
    private final int base;
    private final int size;
    private final Rom rom;
    private final MemoryRegion backing;

    private boolean overlayEnabled;

    public OverlayMemoryRegion(int base, int size, Rom rom, MemoryRegion backing, boolean overlayEnabled) {
        if (size < 2) {
            throw new IllegalArgumentException("Overlay aperture must be at least 2 bytes");
        }
        this.base = base;
        this.size = size;
        this.rom = Objects.requireNonNull(rom, "rom must not be null");
        this.backing = Objects.requireNonNull(backing, "backing must not be null");
        if (backing.base() != 0) {
            throw new IllegalArgumentException("Overlay backing region must start at address 0");
        }
        this.overlayEnabled = overlayEnabled;
    }

    @Override
    public int base() {
        return base;
    }

    @Override
    public int size() {
        return size;
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        this.overlayEnabled = overlayEnabled;
    }

    @Override
    public int readByte(int offset) {
        if (overlayEnabled) {
            return rom.readByte(romAddress(offset));
        }
        return backing.readByte(backingOffset(offset));
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
        if (!overlayEnabled) {
            backing.writeByte(backingOffset(offset), value);
        }
    }

    @Override
    public void writeWord(int offset, int value) {
        writeByte(offset, value >>> 8);
        writeByte(offset + 1, value);
    }

    @Override
    public void writeLong(int offset, int value) {
        writeByte(offset, value >>> 24);
        writeByte(offset + 1, value >>> 16);
        writeByte(offset + 2, value >>> 8);
        writeByte(offset + 3, value);
    }

    private int romAddress(int offset) {
        return rom.baseAddress() + wrap(offset, rom.size());
    }

    private int backingOffset(int offset) {
        return wrap(offset, backing.size());
    }

    private static int wrap(int offset, int size) {
        return offset % size;
    }
}
