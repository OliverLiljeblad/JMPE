package com.JMPE.bus;

import com.JMPE.cpu.m68k.exceptions.AddressErrorException;
import com.JMPE.cpu.m68k.exceptions.BusErrorException;
import com.JMPE.machine.MacPlusMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches bus reads and writes to the correct {@link MemoryRegion} based
 * on the 24-bit Mac Plus address map.
 *
 * <h2>Role in the system</h2>
 * {@code AddressSpace} is the single owner of all registered regions.
 * {@link MacPlusMachine} builds one instance, registers the machine's memory
 * regions into it (such as ROM-backed and other addressable regions), then
 * passes it (as a {@link Bus}) to everything that needs bus access: the CPU,
 * the decoder, the DMA controller, etc.
 *
 * <h2>Address masking</h2>
 * The 68000 has 24 address pins, so bit 24 and above are ignored.
 * Every address is masked to {@code 0x00FFFFFF} before dispatch.
 *
 * <h2>Unmapped accesses</h2>
 * A read or write to an address that no region covers fires a
 * {@link BusErrorException}.  On real Mac Plus hardware this would assert the
 * /BERR line, which causes the 68000 to take a Bus Error exception through
 * the group-0 frame path.  That CPU-side routing is intentionally kept
 * separate from the current simple exception dispatcher.
 *
 * <h2>Region overlap</h2>
 * Overlapping regions are not permitted.  {@link #addRegion} throws
 * {@link IllegalArgumentException} if a new region would overlap any
 * existing one.  This is checked at setup time (machine init) so it cannot
 * silently corrupt a running emulation.
 *
 * <h2>Lookup strategy</h2>
 * Region count for the Mac Plus is small (< 10), so a linear scan over an
 * {@code ArrayList} is fast enough — a region is found in at most ~8
 * comparisons.  A sorted structure or interval tree would only pay off
 * with dozens of regions.
 */
public final class AddressSpace implements Bus {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** The 68000 exposes 24 address pins; bits above this are meaningless. */
    private static final int ADDRESS_MASK = 0x00FF_FFFF;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<MemoryRegion> regions = new ArrayList<>(8);

    // -------------------------------------------------------------------------
    // Region registration
    // -------------------------------------------------------------------------

    /**
     * Registers a memory region.
     *
     * @param region the region to add; must not overlap any existing region
     * @throws IllegalArgumentException if the region overlaps an existing one
     */
    public void addRegion(MemoryRegion region) {
        int newBase = region.base();
        int newEnd  = newBase + region.size() - 1;

        for (MemoryRegion existing : regions) {
            int exBase = existing.base();
            int exEnd  = exBase + existing.size() - 1;
            if (newBase <= exEnd && newEnd >= exBase) {
                throw new IllegalArgumentException(String.format(
                    "Region [0x%06X, 0x%06X] overlaps existing [0x%06X, 0x%06X]",
                    newBase, newEnd, exBase, exEnd));
            }
        }
        regions.add(region);
    }

    // -------------------------------------------------------------------------
    // Bus — reads
    // -------------------------------------------------------------------------

    @Override
    public int readByte(int address) throws BusErrorException {
        int addr = address & ADDRESS_MASK;
        return regionAt(addr).readByte(addr - regionAt(addr).base());
        // Note: two calls to regionAt — inlined below for clarity; a hot JIT
        // will inline and eliminate the double lookup.
    }

    @Override
    public int readWord(int address) throws BusErrorException, AddressErrorException {
        int addr = address & ADDRESS_MASK;
        checkAlignment(addr, address);
        MemoryRegion r = regionAt(addr);
        return r.readWord(addr - r.base());
    }

    @Override
    public int readLong(int address) throws BusErrorException, AddressErrorException {
        int addr = address & ADDRESS_MASK;
        checkAlignment(addr, address);
        MemoryRegion r = regionAt(addr);
        return r.readLong(addr - r.base());
    }

    // -------------------------------------------------------------------------
    // Bus — writes
    // -------------------------------------------------------------------------

    @Override
    public void writeByte(int address, int value) throws BusErrorException {
        int addr = address & ADDRESS_MASK;
        MemoryRegion r = regionAt(addr);
        r.writeByte(addr - r.base(), value);
    }

    @Override
    public void writeWord(int address, int value) throws BusErrorException, AddressErrorException {
        int addr = address & ADDRESS_MASK;
        checkAlignment(addr, address);
        MemoryRegion r = regionAt(addr);
        r.writeWord(addr - r.base(), value);
    }

    @Override
    public void writeLong(int address, int value) throws AddressErrorException, BusErrorException {
        int addr = address & ADDRESS_MASK;
        checkAlignment(addr, address);
        MemoryRegion r = regionAt(addr);
        r.writeLong(addr - r.base(), value);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds the region that owns {@code addr}, or throws {@link BusErrorException}
     * if no region covers it.
     */
    private MemoryRegion regionAt(int addr) throws BusErrorException {
        for (MemoryRegion r : regions) {
            if (addr >= r.base() && addr < r.base() + r.size()) {
                return r;
            }
        }
        throw new BusErrorException(addr);
    }

    /**
     * Checks that {@code addr} is word-aligned (even).  If not, throws
     * {@link AddressErrorException}.
     *
     * @param addr    the 24-bit masked address being accessed
     * @param rawAddr the original address before masking, for the error report
     */
    private static void checkAlignment(int addr, int rawAddr) throws AddressErrorException {
        if ((addr & 1) != 0) {
            throw new AddressErrorException(rawAddr);
        }
    }
}
