package com.JMPE.cpu.m68k.instructions.arithmetic;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Suba_Test {
    @Test
    void executeSubtractsSignExtendedWordSourceFromAddressRegister() {
        AtomicInteger writtenValue = new AtomicInteger();

        int cycles = Suba.execute(Size.WORD, () -> 0xFFFF, () -> 0x0000_1000, writtenValue::set);

        assertAll(
            () -> assertEquals(Suba.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_1001, writtenValue.get())
        );
    }

    @Test
    void executeSubtractsFullLongSourceFromAddressRegister() {
        AtomicInteger writtenValue = new AtomicInteger();

        Suba.execute(Size.LONG, () -> 0x0001_0000, () -> 0x0002_0000, writtenValue::set);

        assertEquals(0x0001_0000, writtenValue.get());
    }

    @Test
    void executeRejectsUnsizedAndByteOperations() {
        assertAll(
            () -> assertEquals(
                "SUBA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Suba.execute(Size.BYTE, () -> 0, () -> 0, value -> { })
                ).getMessage()
            ),
            () -> assertEquals(
                "SUBA must use WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Suba.execute(Size.UNSIZED, () -> 0, () -> 0, value -> { })
                ).getMessage()
            )
        );
    }
}
