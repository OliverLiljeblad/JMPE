package com.JMPE.cpu.m68k.exceptions;

/**
 * Marker for CPU exceptions that currently enter through the 68000 simple six-byte frame path.
 *
 * <p>Group-0 bus/address faults are intentionally excluded because they need a distinct frame format.</p>
 */
public interface SimpleVectoredException {
    ExceptionVector exceptionVector();
}
