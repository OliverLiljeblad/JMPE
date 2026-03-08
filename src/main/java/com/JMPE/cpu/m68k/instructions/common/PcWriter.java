package com.JMPE.cpu.m68k.instructions.common;

/**
 * Writes a new program counter value.
 * Instruction helpers use this abstraction to avoid direct CPU/register access.
 */
@FunctionalInterface
public interface PcWriter {
    void write(int newPc);
}
