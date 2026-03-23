package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Jsr_Test {
    @Test
    void executePushesReturnAddressBeforeUpdatingProgramCounter() {
        List<String> events = new ArrayList<>();
        AtomicInteger pushedValue = new AtomicInteger();
        AtomicInteger writtenPc = new AtomicInteger();

        int cycles = Jsr.execute(
                0x0200,
                0x1000,
                value -> {
                    events.add("push");
                    pushedValue.set(value);
                },
                value -> {
                    events.add("pc");
                    writtenPc.set(value);
                }
        );

        assertAll(
                () -> assertEquals(Jsr.EXECUTION_CYCLES, cycles),
                () -> assertEquals(List.of("push", "pc"), events),
                () -> assertEquals(0x0200, pushedValue.get()),
                () -> assertEquals(0x1000, writtenPc.get())
        );
    }
}
