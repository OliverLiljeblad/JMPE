package com.JMPE.cpu.m68k.exceptions;

/**
 * Thrown when a bus cycle targets an unmapped address.
 *
 * <p>On real Mac Plus hardware this would assert the 68000's /BERR pin,
 * causing the CPU to take a Bus Error exception (vector 2).  That uses the
 * 68000 group-0 frame format, so it is intentionally kept separate from the
 * current simple six-byte exception-entry path.
 *
 * <p>This is an unchecked exception because bus errors are unrecoverable
 * from the emulated program's perspective and should propagate up to the
 * CPU loop without requiring every intermediate caller to declare it.
 */
public class BusErrorException extends RuntimeException {

    private final int address;

    public BusErrorException(int address) {
        super(String.format("<[BusErrorException]> unmapped bus address 0x%08X", address & 0x00FF_FFFF));
        this.address = address;
    }

    /** The bus address that triggered the error (24-bit, unmasked). */
    public int address() { return address; }
}
