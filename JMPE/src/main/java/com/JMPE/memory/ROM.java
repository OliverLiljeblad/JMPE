package com.JMPE.memory;

/**
 * 128KB Mac Plus ROM, mapped at 0x400000 - 0x41FFFF.
 *
 * ROM is read-only; any write attempt throws immediately
 * so bad CPU behaviour surfaces loudly during development.
 */
public final class ROM implements MemoryRegion {

    public static final int BASE = 0x400000;
    public static final int SIZE = 128 * Unit.KB.the();
    public static final int END  = BASE + SIZE - 1;

    private final byte[] data;

    public ROM(byte[] ROMdata) {
        assert ROMdata != null;
        if  (ROMdata.length != SIZE) {
            throw new IllegalArgumentException("ROM data length must be equal to " + SIZE);
        }

        //NOTE: Defensive clone to make sure it cant be modified later
        this.data = ROMdata.clone();
    }


    @Override public int getBaseAddress() {
        return BASE;
    }
    @Override public int getEndAddress() {
        return END;
    }

    @Override
    public int offset_to_within(int address) {
        assert address >= BASE && address <= END;
        return address - BASE;
    }

//    @Override public boolean is_writeable() { return false; }

    @Override
    public int readbit(int address) {
        //TODO: which bit are we meant to be reading?
        //      Update this implementation to more accurately read bits
        return data[offset_to_within(address)] & 1;
    }

    @Override
    public int read8(int address) {
        return data[offset_to_within(address)] & 0xFF;
    }

    @Override
    public int read16(int address) {
        int offset = offset_to_within(address);
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    @Override
    public int read32(int address) {
        int offset = offset_to_within(address);
        return MemoryRegion.super.read32(offset);
    }

    /**
     * @param address
     * @throws "NotImplementedException"{to be implemented}
     */
    @Override
    public int read(int address) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void writebit(int address, int bit) throws ROMWriteException {
        throw new ROMWriteException(
                String.format("ROM: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, bit)
        );
    }

    @Override
    public void write8(int address, int value) throws ROMWriteException {
        throw new ROMWriteException(
                String.format("ROM: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
        );
    }

    @Override
    public void write16(int address, int value) throws ROMWriteException {
        throw new ROMWriteException(
                String.format("ROM: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
        );
    }

    @Override
    public void write32(int address, int value) throws ROMWriteException {
        throw new ROMWriteException(
                String.format("ROM: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
        );
    }

    @Override
    public void write(int address, int value) throws ROMWriteException {
        throw new ROMWriteException(
                String.format("ROM: write operation to address [0x%06X]:> {value: 0x%02X} not allowed", address, value)
        );
    }

    static class ROMWriteException extends Exception {
        public ROMWriteException(String message) {
            super(message);
        }
    }

}