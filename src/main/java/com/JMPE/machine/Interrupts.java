package com.JMPE.machine;

/**
 * CPU-facing interrupt source contract.
 */
@FunctionalInterface
public interface Interrupts {
    Interrupts NONE = () -> 0;

    /**
     * Returns the highest currently pending interrupt level, or 0 when no interrupt is pending.
     */
    int highestPendingLevel();

    static Interrupts none() {
        return NONE;
    }
}
