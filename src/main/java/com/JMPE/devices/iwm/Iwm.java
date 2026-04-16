package com.JMPE.devices.iwm;

public class Iwm {
    // The mode register. 5 bits.
    private int mode;

    boolean enabled;
    boolean q6, q7;

    void accessLine(int line, boolean set) {
        switch (normalize(line)) {
            case 0, 1, 2, 3 -> { } // no-op for CA0, CA1, CA2, LSTRB
            case 4 -> enabled = set;
            case 5 -> throw new UnsupportedOperationException();
            case 6 -> q6 = set;
            case 7 -> q7 = set;
            default -> throw new IllegalArgumentException("Invalid IWM line: " + line);
        }
    }

    public int read() {
        String status = (q6 ? "6" : "_") + (q7 ? "7" : "_");
        return switch (status) {
            case "6_" -> (enabled ? 0x20 : 0) | normalize(mode);
            default -> 0;
        };
    }

    public void write(int value) {
        if (q6 && q7) {
            mode = normalize(value);
        }
    }

    // Called by Mmio ByteReader — latch the addressed line, then return data
    public int access(int offset) {
        int line = iwmLine(offset);
        accessLine(line >>> 1, (line & 1) != 0);
        return read();
    }

    // Called by Mmio ByteWriter — latch the addressed line, then write data
    public void accessAndWrite(int offset, int value) {
        int line = iwmLine(offset);
        accessLine(line >>> 1, (line & 1) != 0);
        write(value);
    }

    private static int iwmLine(int offset) {
        return (offset >>> 9) & 0xF;   // A12:A9
    }

    private static int normalize(int register) {
        return register & 0x1F;
    }
}
