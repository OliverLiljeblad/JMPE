package com.JMPE.memory;

import java.lang.Exception;

public interface MemoryRegion {
    //DOCS: Inclusive bounds of memory region
    int getBaseAddress();
    int getEndAddress();


    int readbit(int address);
    int read8(int address);

    //DOCS: Reads a 16-bit big-endian 'word'
    // |DEFAULT: two byte reads. should be overridden in specific implementation
    default int read16(int address) {
        return (read8(address) & 0xFF) << 8 | (read8(address + 1) & 0xFF);
    }

    //DOCS: Reads a 32-bit big-endian 'long-word'
    // |DEFAULT: two word reads. should be overridden in specific implementation
    default int read32(int address) {
        return (read16(address) & 0xFF) << 16 | (read16(address + 1) & 0xFFFF);
    }

    int read(int address);


    void writebit(int address, int bit) throws Exception;
    void write8(int address, int value) throws Exception;

    //DOCS: Writes a 16-bit big-endian 'word'
    // |DEFAULT: two byte writes. should be overridden in specific implementation
    default void write16(int address, int value) throws Exception {
        write8(address, (value >> 8) & 0xFF);
        write8(address + 1, (value & 0xFF));
    }

    //DOCS: Writes a 32-bit big-endian 'word'
    // |DEFAULT: two byte writes. should be overridden in specific implementation
    default void write32(int address, int value) throws Exception {
        write8(address, (value >> 16) & 0xFFFF);
        write8(address + 2, (value & 0xFFFF));
    }

    void write(int address, int value) throws Exception;


    default boolean contains(int address) {
        return address >= getBaseAddress() && address <= getEndAddress();
    }
}
