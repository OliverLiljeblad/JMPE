package com.JMPE.devices.iwm;

public class Iwm {
    // The mode register. 5 bits.
    private int mode;

    boolean enabled;
    boolean driveSelect;   // line 5: DRVSEL — drive 1 (false) vs drive 2 (true)
    boolean q6, q7;

    // Drive-control lines latched by the Sony driver to address a status
    // register. CA2:CA1:CA0 (and externally-driven SEL on VIA PA4) form a
    // 4-bit selector. LSTRB pulses to load step/seek commands.
    boolean ca0, ca1, ca2, lstrb;

    void accessLine(int line, boolean set) {
        switch (normalize(line)) {
            case 0 -> ca0 = set;
            case 1 -> ca1 = set;
            case 2 -> ca2 = set;
            case 3 -> lstrb = set;
            case 4 -> enabled = set;
            case 5 -> driveSelect = set;
            case 6 -> q6 = set;
            case 7 -> q7 = set;
            default -> throw new IllegalArgumentException("Invalid IWM line: " + line);
        }
    }

    //TODO: Complete implementation
    public int read() {
        String status = (q6 ? "6" : "_") + (q7 ? "7" : "_");
        return switch (status) {
            case "6_" -> (enabled ? 0x20 : 0) | normalize(mode);
            default -> 0;
        };
    }

    //TODO: Complete implementation
    public void write(int value) {
        if (q6 && q7) {
            mode = normalize(value);
        }
    }

    //NOTE: Called by Mmio ByteReader — latch the addressed line, then return data
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
