package com.JMPE.cpu.m68k.exceptions;

import java.util.Objects;

/**
 * Thrown when a bus cycle targets an unmapped address.
 *
 * <p>On real Mac Plus hardware this would assert the 68000's /BERR pin,
 * causing the CPU to take a Bus Error exception (vector 2) through the
 * 68000 group-0 frame format.
 *
 * <p>This is an unchecked exception because bus errors are unrecoverable
 * from the emulated program's perspective and should propagate up to the
 * CPU loop without requiring every intermediate caller to declare it.
 */
public class BusErrorException extends RuntimeException implements Group0Fault {

    private final int address;
    private final FaultAccessType accessType;

    public BusErrorException(int address) {
        this(address, FaultAccessType.READ);
    }

    public BusErrorException(int address, FaultAccessType accessType) {
        super(String.format(
            "<[BusErrorException]> unmapped %s bus address 0x%08X",
            Objects.requireNonNull(accessType, "accessType must not be null").name().toLowerCase(),
            address & 0x00FF_FFFF
        ));
        this.address = address;
        this.accessType = accessType;
    }

    /** The bus address that triggered the error (24-bit, unmasked). */
    public int address() {
        return address;
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.BUS_ERROR;
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
