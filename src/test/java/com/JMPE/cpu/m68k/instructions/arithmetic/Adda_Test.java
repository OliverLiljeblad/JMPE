package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Adda_Test {
    @Test
    void executeAddsSignExtendedWordSourceToAddressRegister() {
        AtomicInteger writtenValue = new AtomicInteger();

        int cycles = Adda.execute(Size.WORD, () -> 0xFFFF, () -> 0x0000_1000, writtenValue::set);

        assertAll(
            () -> assertEquals(Adda.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_0FFF, writtenValue.get())
        );
    }

    @Test
    void executeAddsFullLongSourceToAddressRegister() {
        AtomicInteger writtenValue = new AtomicInteger();

        Adda.execute(Size.LONG, () -> 0x0001_0000, () -> 0x0002_0000, writtenValue::set);

        assertEquals(0x0003_0000, writtenValue.get());
    }

    @Test
    void executeRejectsUnsizedAndByteOperations() {
        assertAll(
            () -> assertEquals(
                "ADDA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Adda.execute(Size.BYTE, () -> 0, () -> 0, value -> { })
                ).getMessage()
            ),
            () -> assertEquals(
                "ADDA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Adda.execute(Size.UNSIZED, () -> 0, () -> 0, value -> { })
                ).getMessage()
            )
        );
    }
}
