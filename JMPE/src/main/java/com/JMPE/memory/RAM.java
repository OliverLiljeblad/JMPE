package com.JMPE.memory;

/**
 * 1MB RAM for the Mac Plus, mapped at 0x000000 - 0x0FFFFF.
 *
 * The real machine had either 1MB or 4MB installed, but 1MB
 * is the baseline. Extend SIZE later if needed.
 *
 * Note: the 68000 reset vector is fetched from the FIRST
 * four bytes of the address space (0x000000). On the Mac Plus
 * the ROM is *also* mirrored at 0x000000 at startup by hardware,
 * so the Bus handles that — RAM itself doesn't need to know.
 */
public class RAM implements MemoryRegion {

    public static final int BASE = 0x000000;
    public static final int SIZE = 1 * Unit.MB.the();
    public static final int END = BASE + SIZE - 1;

    private final byte[] data;

    public RAM() {
        data = new byte[SIZE];
    }

    @Override public int getBaseAddress() { return BASE; }
    @Override public int getEndAddress()  { return END;  }

    @Override
    public int readbit(int address) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return (read16(offset) << 16) | read16(offset + 2);
    }

    @Override
    public int read(int address) {
        return 0;
    }

    @Override
    public void writebit(int address, int bit) throws ROM.ROMWriteException {
        assert (bit == 0 || bit == 1);
        int offset = offset_to_within(address);
        data[offset] = (byte) (bit);
    }

    @Override
    public void write8(int address, int value) throws ROM.ROMWriteException {
        int offset = offset_to_within(address);
        data[offset] = (byte) (value & 0xFF);
    }

    @Override
    public void write16(int address, int value) throws ROM.ROMWriteException {
        int offset = offset_to_within(address);
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    @Override
    public void write32(int address, int value) throws ROM.ROMWriteException {
        int  offset = offset_to_within(address);
        data[offset] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    @Override
    public void write(int address, int value) throws ROM.ROMWriteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
