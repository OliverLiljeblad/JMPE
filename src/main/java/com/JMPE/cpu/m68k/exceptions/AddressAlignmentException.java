package com.JMPE.cpu.m68k.exceptions;

/**
 * Thrown when a word or long access targets an odd (misaligned) address.
 *
 * <p>The 68000 requires all word and long word bus cycles to use even
 * addresses.  An odd address causes the CPU to take an Address Error
 * exception (vector 3) <em>before</em> the bus cycle completes.  The CPU
 * loop catches this and routes it through
 * {@link com.JMPE.cpu.m68k.exceptions.ExceptionDispatcher}.
 *
 * <p>Unchecked for the same reason as {@link BusErrorException}.
 */
public class AddressAlignmentException extends Exception {

    private final int address;

    public AddressAlignmentException(int address) {
        super(String.format("<[AddressAlignmentException]> address{0x%08X} is not aligned", address & 0x00FF_FFFF));
        this.address = address;
    }

    /** The raw (pre-mask) address that triggered the error. */
    public int address() { return address; }
}
