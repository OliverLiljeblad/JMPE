package com.JMPE.cpu.m68k.instructions.data;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Lea_Test {
    @Test
    void executeWritesEffectiveAddressToSelectedAddressRegister() {
        AtomicInteger registerIndex = new AtomicInteger(-1);
        AtomicInteger writtenValue = new AtomicInteger();

        int cycles = Lea.execute(5, 0x1234_5678, (register, value) -> {
            registerIndex.set(register);
            writtenValue.set(value);
        });

        assertAll(
                () -> assertEquals(Lea.EXECUTION_CYCLES, cycles),
                () -> assertEquals(5, registerIndex.get()),
                () -> assertEquals(0x1234_5678, writtenValue.get())
        );
    }

    @Test
    void executeRejectsNullWriter() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> Lea.execute(0, 0x1000, null)
        );

        assertEquals("writer must not be null", exception.getMessage());
    }
}
