package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Scc_Test {
    @Test
    void executeWritesAllOnesWhenConditionIsTrue() {
        AtomicInteger destination = new AtomicInteger(Integer.MIN_VALUE);

        int cycles = Scc.execute(true, destination::set);

        assertAll(
            () -> assertEquals(Scc.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFF, destination.get())
        );
    }

    @Test
    void executeWritesZeroWhenConditionIsFalse() {
        AtomicInteger destination = new AtomicInteger(Integer.MIN_VALUE);

        Scc.execute(false, destination::set);

        assertEquals(0x00, destination.get());
    }

    @Test
    void executeRejectsNullDestinationWriter() {
        assertThrows(NullPointerException.class, () -> Scc.execute(true, null));
    }
}
