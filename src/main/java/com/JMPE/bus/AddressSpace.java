package com.JMPE.bus;

/**
 * Memory-mapped address space interface.
 *
 * HOT-PATH SAFE: Implementations must not allocate.
 */
public interface AddressSpace {

    /**
     * Read 8-bit value from address.
     * @throws AddressError if alignment or access violation
     */
    byte readByte(int address);

    /**
     * Read 16-bit value from address (big-endian).
     * @throws AddressError if address is odd
     */
    int readWord(int address);

    /**
     * Read 32-bit value from address (big-endian).
     * @throws AddressError if address is odd
     */
    int readLong(int address);

    /**
     * Write 8-bit value to address.
     */
    void writeByte(int address, int value);

    /**
     * Write 16-bit value to address (big-endian).
     */
    void writeWord(int address, int value);

    /**
     * Write 32-bit value to address (big-endian).
     */
    void writeLong(int address, int value);

    /**
     * Check if address is valid for word/long access.
     * @return true if address is even (68k requires even alignment)
     */
    static boolean isAligned(int address) {
        return (address & 1) == 0;
    }
}
