package com.JMPE.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * The central address bus.
 *
 * Routes every CPU read/write to the correct MemoryRegion based on
 * the Mac Plus memory map.  Regions are checked in registration order,
 * so register more-specific regions before catch-all ones.
 *
 * Mac Plus memory map (simplified):
 *
 *   0x000000 - 0x0FFFFF   RAM (1MB), mirrored from ROM at cold reset
 *   0x400000 - 0x41FFFF   ROM (128KB)
 *   0x800000 - 0x9FFFFF   SCC read region
 *   0xA00000 - 0xBFFFFF   SCC write region
 *   0xC00000 - 0xDFFFFF   IWM
 *   0xE80000 - 0xEFFFFF   VIA
 *
 * The ROM overlay at reset (ROM mirrored at 0x000000) is handled here
 * via a simple flag; once the CPU has booted far enough the overlay
 * is removed and RAM becomes visible at 0x000000 normally.
 */
public class Bus {
    private final List<MemoryRegion> regions = new ArrayList<>();

    // ROM overlay: at cold reset the ROM is visible at 0x000000 so the
    // 68000 can fetch its reset vectors.  The VIA clears this flag once
    // the system has initialised.
    private boolean romOverlayActive = true;
    private ROM rom;   // kept separately so we can apply the overlay


    public void register(MemoryRegion region) {
        if (region instanceof ROM) {
            this.rom = (ROM)region;
        }
        regions.add(region);
    }

    /**
     * Called by the VIA when it clears the overlay bit (shortly after reset).
     * After this, address 0x000000 routes to RAM, not ROM.
     */
    public void setRomOverlayActive(boolean active) {
        this.romOverlayActive = active;
    }

    public boolean isRomOverlayActive() {
        return romOverlayActive;
    }

    public int read8(int address) {
        return resolve(address).read8(address);
    }

    public int read16(int address) {
        checkAlignment(address, 2);
        return resolve(address).read8(address);
    }

    public int read32(int address) {
        checkAlignment(address, 4);
        return resolve(address).read8(address);
    }

    public void write8(int address, int value) {
        try {
            resolve(address).write8(address, value);
        } catch (ROM.ROMWriteException e) {}
    }

    public void write16(int address, int value) {
        checkAlignment(address, 2);
        try {
            resolve(address).write16(address, value);
        } catch (ROM.ROMWriteException e) {}
    }

    public void write32(int address, int value) {
        checkAlignment(address, 4);
        try {
            resolve(address).write32(address, value);
        } catch (ROM.ROMWriteException e) {}
    }

    /**
     * Find the region that owns this address.
     * Applies the ROM overlay: if active, any access to 0x000000 - 0x01FFFF
     * (the size of the ROM) is forwarded to the ROM regardless of what is
     * normally mapped there.
     */
    private MemoryRegion resolve(int address) {
        if (romOverlayActive && rom != null && address < ROM.SIZE) {
            return new OverlayAdapter(rom, address);
        }

        for (MemoryRegion region : regions) {
            if (region.contains(address)) {
                return region;
            }
        }

        throw new BusException(address, "unmapped address");
    }

    private static void checkAlignment(int address, int size) {
        if ((address & (size - 1)) != 0) {
            throw new BusException(address, "misaligned " + (size * 8) + "-bit access");
        }
    }


    /**
     * Wraps the ROM so overlay accesses at low addresses are translated
     * into the ROM's own address space without mutating the address that
     * gets passed into ROM's read methods.
     *
     * We add ROM.BASE so ROM's own offset() subtraction cancels it out.
     */
    private static class OverlayAdapter implements MemoryRegion {
        private final ROM rom;
        private final int translatedAddress;

        public OverlayAdapter(ROM rom, int address) {
            this.rom = rom;
            this.translatedAddress = ROM.BASE + address;
        }

        @Override public int getBaseAddress() { return 0; }
        @Override public int getEndAddress()  { return ROM.SIZE - 1; }

        @Override
        public int offset_to_within(int address) {
            return this.translatedAddress;
        }

        @Override public int readbit(int bit) { return -1; }

        @Override public int read8(int address)  { return rom.read8(translatedAddress); }
        @Override public int read16(int address) { return rom.read16(translatedAddress); }
        @Override public int read32(int address) { return rom.read32(translatedAddress); }

        @Override public int read(int value) { return -1; }

        @Override
        public void writebit(int address, int bit) {
            throw new UnsupportedOperationException(
                    String.format("ROM<overlay>: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, bit)
            );
        }

        @Override
        public void write(int address, int value) {
            throw new UnsupportedOperationException(
                String.format("ROM<overlay>: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
            );
        }

        @Override public void write8(int address, int value) {
            throw new UnsupportedOperationException(
                String.format("ROM<overlay>: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
            );
        }
    }

    public static class BusException extends RuntimeException {
        public final int address;
        BusException(int address, String reason) {
            super(String.format("Bus[0x%06X]: %s", address, reason));
            this.address = address;
        }
    }
}
