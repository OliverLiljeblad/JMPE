package com.JMPE.bus;

/**
 * A contiguous, mappable region of the Mac Plus address space.
 *
 * <p>Instances are registered with {@link AddressSpace}, which dispatches
 * every bus read and write to whichever region owns the target address.
 * The three concrete implementations are {@link Ram}, {@link Rom}, and
 * {@link Mmio}; device drivers (VIA, IWM, SCC …) implement {@code Mmio}.
 *
 * <h2>Address contract</h2>
 * Every address passed to {@code read*} / {@code write*} is a <em>local</em>
 * offset from {@link #base()} — i.e. already subtracted by the caller
 * ({@link AddressSpace}).  Implementations must not subtract the base again.
 *
 * <h2>Size and alignment</h2>
 * {@link #size()} must be a power of two and must be ≥ 2.
 * The 68000 requires word and long accesses to be word-aligned; the bus
 * enforces this before any region is called, so regions themselves need
 * not re-check alignment.
 */
public interface MemoryRegion {

    /**
     * The lowest bus address owned by this region (inclusive), after the
     * 24-bit mask has been applied.
     */
    int base();

    /**
     * The number of addressable bytes in this region.  The region owns
     * addresses {@code [base(), base() + size() - 1]}.
     */
    int size();

    // -------------------------------------------------------------------------
    // Reads — offset is relative to base()
    // -------------------------------------------------------------------------

    /**
     * Reads one byte at {@code offset} from this region's base.
     * Returns the value zero-extended into bits [7:0] of the result.
     */
    int readByte(int offset);

    /**
     * Reads one big-endian word (2 bytes) at {@code offset}.
     * Returns the value zero-extended into bits [15:0].
     * {@code offset} is guaranteed to be even by the bus.
     */
    int readWord(int offset);

    /**
     * Reads one big-endian long word (4 bytes) at {@code offset}.
     * {@code offset} is guaranteed to be even by the bus.
     */
    int readLong(int offset);

    // -------------------------------------------------------------------------
    // Writes — offset is relative to base()
    // -------------------------------------------------------------------------

    /**
     * Writes the low byte of {@code value} to {@code offset}.
     */
    void writeByte(int offset, int value);

    /**
     * Writes the low 16 bits of {@code value} as a big-endian word to
     * {@code offset}.  {@code offset} is guaranteed to be even.
     */
    void writeWord(int offset, int value);

    /**
     * Writes all 32 bits of {@code value} as a big-endian long word to
     * {@code offset}.  {@code offset} is guaranteed to be even.
     */
    void writeLong(int offset, int value);
}
