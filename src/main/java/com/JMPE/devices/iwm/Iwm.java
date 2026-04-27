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
    private boolean sel;

    public void setSel(boolean sel) {
        this.sel = sel;
    }

    /**
     * Diagnostic hook fired after every memory-mapped IWM access. The state
     * fields reflect the values latched by THIS access (i.e. accessed-after).
     * {@code value} is the byte returned to the bus on a read, or the byte
     * written by the CPU on a write.
     */
    @FunctionalInterface
    public interface AccessListener {
        void onAccess(int offset, boolean isWrite, int value,
                      boolean sel, boolean ca0, boolean ca1, boolean ca2,
                      boolean lstrb, boolean q6, boolean q7, boolean enabled);
    }

    private AccessListener accessListener;

    public void setAccessListener(AccessListener listener) {
        this.accessListener = listener;
    }

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
            //   bit 7    : SENSE — value of the drive line addressed by
            //              CA2:CA1:CA0 plus the externally-driven SEL on
            //              VIA PA4 (which we don't yet route through here).
            //   bit 6    : 0 (MZ)
            //   bit 5    : ENABLE state
            //   bits 4-0 : mode register
            //
            // We don't model an actual attached drive, but we DO need the
            // Sony driver's drive-enumeration probe to find at least one
            // "drive installed" so it adds an entry to DrvQHdr at $308.
            // Without that, the boot at PC $004006E8 spins forever waiting
            // for DrvQHdr.qHead ($030A) to become non-null.
            //
            // The Sony /DRVIN status (drive installed, active-low; 0=present)
            // is the selector with CA2:CA1:CA0=1:1:1. For every other CA
            // selector — /CSTIN (no disk inserted), /TKO (not at track 0),
            // /WRTPRT (not write-protected), etc. — we return SENSE=1
            // ("negated"), which the driver maps to "no/empty/idle".
            //
            // This gives the driver a coherent picture of "one drive,
            // no disk" — exactly the state needed to fall through to the
            // flashing-? boot icon.
            int sense = (ca2 && ca1 && ca0) ? 0 : 0x80;
            return sense | (enabled ? 0x20 : 0) | normalize(mode);
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
        int value = read();
        if (accessListener != null) {
            accessListener.onAccess(offset, false, value,
                sel, ca0, ca1, ca2, lstrb, q6, q7, enabled);
        }
        return value;
    }

    // Called by Mmio ByteWriter — latch the addressed line, then write data
    public void accessAndWrite(int offset, int value) {
        int line = iwmLine(offset);
        accessLine(line >>> 1, (line & 1) != 0);
        write(value);
        if (accessListener != null) {
            accessListener.onAccess(offset, true, value,
                sel, ca0, ca1, ca2, lstrb, q6, q7, enabled);
        }
    }

    private static int iwmLine(int offset) {
        return (offset >>> 9) & 0xF;   // A12:A9
    }

    private static int normalize(int register) {
        return register & 0x1F;
    }
}
