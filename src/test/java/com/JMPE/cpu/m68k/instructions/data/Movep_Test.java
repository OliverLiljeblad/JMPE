package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Movep_Test {
    @Test
    void moveMemoryToRegisterReadsSpacedWordBytes() {
        AtomicInteger writtenValue = new AtomicInteger(-1);
        Map<Integer, Integer> memory = Map.of(0x2000, 0x12, 0x2002, 0x34);

        int cycles = Movep.moveMemoryToRegister(Size.WORD, 0x2000, address -> memory.getOrDefault(address, 0), writtenValue::set);

        assertAll(
            () -> assertEquals(Movep.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234, writtenValue.get())
        );
    }

    @Test
    void moveRegisterToMemoryWritesSpacedLongBytes() {
        Map<Integer, Integer> memory = new HashMap<>();

        int cycles = Movep.moveRegisterToMemory(Size.LONG, () -> 0x1234_5678, 0x2000, memory::put);

        assertAll(
            () -> assertEquals(Movep.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x12, memory.get(0x2000)),
            () -> assertEquals(0x34, memory.get(0x2002)),
            () -> assertEquals(0x56, memory.get(0x2004)),
            () -> assertEquals(0x78, memory.get(0x2006))
        );
    }

    @Test
    void rejectInvalidInputs() {
        assertAll(
            () -> assertEquals(
                "MOVEP supports only WORD or LONG size",
                assertThrows(IllegalArgumentException.class,
                    () -> Movep.moveMemoryToRegister(Size.BYTE, 0x2000, address -> 0, value -> { })
                ).getMessage()
            ),
            () -> assertThrows(NullPointerException.class, () -> Movep.moveMemoryToRegister(null, 0x2000, address -> 0, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Movep.moveMemoryToRegister(Size.WORD, 0x2000, null, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Movep.moveMemoryToRegister(Size.WORD, 0x2000, address -> 0, null)),
            () -> assertThrows(NullPointerException.class, () -> Movep.moveRegisterToMemory(Size.WORD, null, 0x2000, (address, value) -> { })),
            () -> assertThrows(NullPointerException.class, () -> Movep.moveRegisterToMemory(Size.WORD, () -> 0, 0x2000, null))
        );
    }
}
