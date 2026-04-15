package com.JMPE.cpu.m68k.exceptions;

/**
 * Marker for 68000 bus/address faults that must enter through the group-0 exception frame path.
 */
public interface Group0Fault {
    ExceptionVector exceptionVector();

    int faultAddress();

    FaultAccessType accessType();
}
