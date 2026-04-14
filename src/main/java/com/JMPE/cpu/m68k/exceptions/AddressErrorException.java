package com.JMPE.cpu.m68k.exceptions;

/**
 * Thrown when a word or long access targets an odd (misaligned) address.
 *
 * <p>The 68000 requires all word and long word bus cycles to use even
 * addresses.  An odd address causes the CPU to take an Address Error
 * exception (vector 3) <em>before</em> the bus cycle completes.  That uses
 * the 68000 group-0 frame format, so it is intentionally modeled separately
 * from the current simple six-byte exception-entry path.
 *
 * <p>Unchecked for the same reason as {@link BusErrorException}.
 */
public class AddressErrorException extends RuntimeException {

    private final int address;

    public AddressErrorException(int address) {
        super(String.format("<[AddressErrorException]> address{0x%08X} is misaligned (odd address) for word/long access", address & 0x00FF_FFFF));
        this.address = address;
    }

    /** The raw (pre-mask) address that triggered the error. */
    public int address() { return address; }
}
