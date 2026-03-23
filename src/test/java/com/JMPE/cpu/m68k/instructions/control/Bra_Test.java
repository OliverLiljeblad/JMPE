package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Bra_Test {
    @Test
    void executeWritesDisplacedProgramCounter() {
        AtomicInteger newPc = new AtomicInteger(Integer.MIN_VALUE);

        int cycles = Bra.execute(0x1000, 0x20, newPc::set);

        assertAll(
                () -> assertEquals(Bra.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0x1020, newPc.get())
        );
    }

    @Test
    void executeRejectsNullPcWriter() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> Bra.execute(0x1000, 0x20, null)
        );

        assertEquals("pcWriter must not be null", exception.getMessage());
    }
}
