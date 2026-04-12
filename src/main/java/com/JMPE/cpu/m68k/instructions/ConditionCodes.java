package com.JMPE.cpu.m68k.instructions;

/**
 * Shared CCR mutator surface for instruction helpers that update N, Z, V, and C.
 *
 * <p>
 * This contract intentionally excludes X (Extend). Helpers that also need to update X can layer a
 * specialized sub-interface on top of this one.
 * </p>
 */
public interface ConditionCodes {
    void setNegative(boolean value);
    void setZero(boolean value);
    void setOverflow(boolean value);
    void setCarry(boolean value);
}
