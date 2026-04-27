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

    public int read() {
        // IWM register selection by Q6/Q7:
        //   Q7=0,Q6=0 -> data register (read disk byte)
        //   Q7=0,Q6=1 -> status register
        //   Q7=1,Q6=0 -> write-handshake register
        //   Q7=1,Q6=1 -> mode register read (returns mode in low 5 bits)
        if (q6 && !q7) {
            // Status register:
            //   bit 7    : SENSE — state of the drive status line addressed
            //              by CA2:CA1:CA0:SEL. We don't model an attached
            //              drive, so always return 1 ("negated"). For every
            //              Sony status line this means "no" — no disk in
            //              place, no track zero, no drive installed, motor
            //              off, idle, etc. The Sony driver interprets this
            //              as "no drive / no disk" and posts a completion
            //              with the appropriate error so its IOParam is no
            //              longer left in-progress.
            //   bit 6    : 0 (MZ)
            //   bit 5    : ENABLE state
            //   bits 4-0 : mode register
            return 0x80 | (enabled ? 0x20 : 0) | normalize(mode);
        }
        if (q6 && q7) {
            // Mode register read.
            return normalize(mode);
        }
        if (!q6 && q7) {
            // Write-handshake register: bit 7 = ready (1 = ready to accept
            // another byte), bit 6 = underrun (0 = no underrun).
            return 0xC0;
        }
        // Data register read: no drive, no disk -> return 0.
        return 0;
    }

    public void write(int value) {
        if (q6 && q7) {
            mode = normalize(value);
        }
        // Other write paths (data register, etc.) are no-ops while no drive
        // is attached.
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
