package com.JMPE.bus;

import java.util.Objects;

/**
 * Generic byte-addressable MMIO region backed by byte-level callbacks.
 *
 * <p>The Mac Plus peripheral windows are sparse and heavily mirrored, so the
 * simplest useful abstraction is "given a local byte offset, read or write one
 * byte". Word and long accesses are then composed in big-endian order by the
 * region itself.</p>
 */
public final class Mmio implements MemoryRegion {
    @FunctionalInterface
    public interface ByteReader {
        int readByte(int offset);
    }

    @FunctionalInterface
    public interface ByteWriter {
        void writeByte(int offset, int value);
    }

    private static final ByteWriter IGNORE_WRITES = (offset, value) -> {
    };

    private final int base;
    private final int size;
    private final ByteReader reader;
    private final ByteWriter writer;

    public Mmio(int base, int size, ByteReader reader, ByteWriter writer) {
        if (size < 2) {
            throw new IllegalArgumentException("MMIO region size must be at least 2 bytes");
        }
        this.base = base;
        this.size = size;
        this.reader = Objects.requireNonNull(reader, "reader must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
    }

    public static Mmio readOnly(int base, int size, ByteReader reader) {
        return new Mmio(base, size, reader, IGNORE_WRITES);
    }

    public static Mmio readWrite(int base, int size, ByteReader reader, ByteWriter writer) {
        return new Mmio(base, size, reader, writer);
    }

    /**
     * Deterministic open-bus placeholder used until a device window is modeled.
     *
     * <p>Returning zero avoids spuriously matching ROM probe signatures while
     * still letting the boot code complete the bus cycles it expects.</p>
     */
    public static Mmio openBus(int base, int size) {
        return readOnly(base, size, offset -> 0);
    }

    @Override
    public int base() {
        return base;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int readByte(int offset) {
        return reader.readByte(offset) & 0xFF;
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
        writer.writeByte(offset, value & 0xFF);
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
}
