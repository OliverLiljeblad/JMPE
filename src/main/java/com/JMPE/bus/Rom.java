package com.JMPE.bus;

/**
 * A read-only memory region holding the Mac Plus ROM image.
 *
 * <h2>Write behaviour</h2>
 * On real Mac Plus hardware, writing to the ROM address range hits open bus —
 * nothing happens and no error is generated.  This implementation matches
 * that behaviour: all {@code write*} methods are silent no-ops.  If the ROM
 * were instead to throw on write, boot-time self-test code that probes the
 * address space would incorrectly trigger Bus Error exceptions.
 *
 * <h2>Loading</h2>
 * The backing array is supplied at construction time.  The caller
 * ({@link com.JMPE.util.RomLoader}) is responsible for reading the image
 * from disk and verifying its checksum before handing it over.
 *
 * <h2>Mirroring</h2>
 * The Mac Plus ROM appears at multiple address ranges (e.g. 0x400000 and
 * 0xFC0000).  Rather than duplicating the array, {@link AddressSpace} should
 * register two separate {@code Rom} instances pointing at the same
 * {@code byte[]} but with different {@code base} values.  The constructor
 * accepts the array by reference for exactly this reason.
 */
public final class Rom implements MemoryRegion {

    private final int    base;
    private final byte[] data;

    /**
     * Creates a ROM region backed by the supplied data array.
     *
     * @param base the lowest bus address owned by this region
     * @param data the ROM bytes; the array is used directly (not copied)
     */
    public Rom(int base, byte[] data) {
        this.base = base;
        this.data = data;
    }

    @Override public int base() { return base; }
    @Override public int size() { return data.length; }

    // -------------------------------------------------------------------------
    // Reads — identical big-endian layout to Ram
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
    // Writes — silent no-ops (open bus behaviour)
    // -------------------------------------------------------------------------

    @Override public void writeByte(int offset, int value) { /* open bus */ }
    @Override public void writeWord(int offset, int value) { /* open bus */ }
    @Override public void writeLong(int offset, int value) { /* open bus */ }
}
