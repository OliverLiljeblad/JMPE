package com.JMPE.bus;

import AddressError;

public class Bus implements AddressSpace {
    private final MemoryRegion[] = new MemoryRegion[256];

    public Bus() {
        //TODO: initialize regions
    }

    @Override
    public byte readByte(int address) {
        MemoryRegion region = toRegion(address);
        return region.readByte(address & 0xFFFF);
    }

    @Override
    public int readWord(int address) {
        return 0;
    }

    @Override
    public int readLong(int address) {
        return 0;
    }

    @Override
    public void writeByte(int address, int value) {

    }

    @Override
    public void writeWord(int address, int value) {

    }

    @Override
    public void writeLong(int address, int value) {

    }
}
