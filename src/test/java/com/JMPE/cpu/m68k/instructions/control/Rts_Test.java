package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Rts_Test {
    @Test
    void executePopsReturnAddressBeforeUpdatingProgramCounter() {
        List<String> events = new ArrayList<>();
        AtomicInteger writtenPc = new AtomicInteger();

        int cycles = Rts.execute(
                () -> {
                    events.add("pop");
                    return 0x00AB_CDEF;
                },
                value -> {
                    events.add("pc");
                    writtenPc.set(value);
                }
        );

        assertAll(
                () -> assertEquals(Rts.EXECUTION_CYCLES, cycles),
                () -> assertEquals(List.of("pop", "pc"), events),
                () -> assertEquals(0x00AB_CDEF, writtenPc.get())
        );
    }
}
