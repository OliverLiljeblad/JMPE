package com.JMPE.cpu.m68k.instructions.data;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Exg_Test {
    @Test
    void executeSwapsRegisterValues() {
        AtomicInteger writtenSource = new AtomicInteger(-1);
        AtomicInteger writtenDestination = new AtomicInteger(-1);

        int cycles = Exg.execute(
            () -> 0x1111_2222,
            () -> 0x3333_4444,
            writtenSource::set,
            writtenDestination::set
        );

        assertAll(
            () -> assertEquals(Exg.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x3333_4444, writtenSource.get()),
            () -> assertEquals(0x1111_2222, writtenDestination.get())
        );
    }

    @Test
    void executeRejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> Exg.execute(null, () -> 0, value -> { }, value -> { }));
        assertThrows(NullPointerException.class, () -> Exg.execute(() -> 0, null, value -> { }, value -> { }));
        assertThrows(NullPointerException.class, () -> Exg.execute(() -> 0, () -> 0, null, value -> { }));
        assertThrows(NullPointerException.class, () -> Exg.execute(() -> 0, () -> 0, value -> { }, null));
    }
}
