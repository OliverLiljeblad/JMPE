package com.JMPE.bus;

/**
 * A readable and writable memory region backed by a plain {@code byte[]}.
 *
 * <p>Used for the Mac Plus main RAM (128 KB–4 MB depending on configuration).
 * The backing array is allocated once at construction; no dynamic resizing
 * ever occurs.
 *
 * <h2>Big-endian layout</h2>
 * Bytes are stored in big-endian order matching the 68000 and the physical
 * Mac Plus memory chips: the high byte of a word is at the lower address.
 *
 * <h2>Offset contract</h2>
 * All offsets received here are already relative to {@link #base()};
 * {@link AddressSpace} subtracts the base before calling.
 */
public final class Ram implements MemoryRegion {

    private final int    base;
    private final byte[] data;

    /**
     * Creates a zero-initialised RAM region.
     *
     * @param base the lowest bus address owned by this region
     * @param size the number of bytes; must be a power of two and ≥ 2
     */
    public Ram(int base, int size) {
        this.base = base;
        this.data = new byte[size];
    }

    @Override public int base() { return base; }
    @Override public int size() { return data.length; }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    @Override
    public int readByte(int offset) {
        return data[offset] & 0xFF;
    }

    @Override
    public int readWord(int offset) {
        return ((data[offset]     & 0xFF) << 8)
            |  (data[offset + 1] & 0xFF);
    }

    @Override
    public int readLong(int offset) {
        return ((data[offset]     & 0xFF) << 24)
            | ((data[offset + 1] & 0xFF) << 16)
            | ((data[offset + 2] & 0xFF) <<  8)
            |  (data[offset + 3] & 0xFF);
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    @Override
    public void writeByte(int offset, int value) {
        data[offset] = (byte) value;
    }

    @Override
    public void writeWord(int offset, int value) {
        data[offset]     = (byte) (value >>> 8);
        data[offset + 1] = (byte)  value;
    }

    @Override
    public void writeLong(int offset, int value) {
        data[offset]     = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>>  8);
        data[offset + 3] = (byte)  value;
    }
}
