package com.JMPE.cpu.m68k.exceptions;

import java.util.Objects;

/**
 * Thrown when a word or long access targets an odd (misaligned) address.
 *
 * <p>The 68000 requires all word and long word bus cycles to use even
 * addresses.  An odd address causes the CPU to take an Address Error
 * exception (vector 3) <em>before</em> the bus cycle completes through the
 * 68000 group-0 frame format.
 *
 * <p>Unchecked for the same reason as {@link BusErrorException}.
 */
public class AddressErrorException extends RuntimeException implements Group0Fault {

    private final int address;
    private final FaultAccessType accessType;

    public AddressErrorException(int address) {
        this(address, FaultAccessType.READ);
    }

    public AddressErrorException(int address, FaultAccessType accessType) {
        super(String.format(
            "<[AddressErrorException]> %s address 0x%08X is misaligned (odd address) for word/long access",
            Objects.requireNonNull(accessType, "accessType must not be null").name().toLowerCase(),
            address & 0x00FF_FFFF
        ));
        this.address = address;
        this.accessType = accessType;
    }

    /** The raw (pre-mask) address that triggered the error. */
    public int address() {
        return address;
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.ADDRESS_ERROR;
    }

    @Override
    public int faultAddress() {
        return address;
    }

    @Override
    public FaultAccessType accessType() {
        return accessType;
    }
}
