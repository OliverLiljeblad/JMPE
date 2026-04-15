package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Link_Test {
    @Test
    void pushesOldAddressRegisterThenAllocatesStackFrame() {
        AtomicInteger stackPointer = new AtomicInteger(0x0000_1100);
        AtomicInteger writtenAddress = new AtomicInteger(-1);
        AtomicInteger writtenValue = new AtomicInteger(-1);
        AtomicInteger linkedRegister = new AtomicInteger(-1);

        int cycles = Link.execute(
            0x1234_5678,
            -8,
            stackPointer::get,
            stackPointer::set,
            (address, value) -> {
                writtenAddress.set(address);
                writtenValue.set(value);
            },
            linkedRegister::set
        );

        assertAll(
            () -> assertEquals(Link.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_10FC, writtenAddress.get()),
            () -> assertEquals(0x1234_5678, writtenValue.get()),
            () -> assertEquals(0x0000_10FC, linkedRegister.get()),
            () -> assertEquals(0x0000_10F4, stackPointer.get())
        );
    }

    @Test
    void rejectsNullCollaborators() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> Link.execute(0, 0, null, value -> { }, (address, value) -> { }, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Link.execute(0, 0, () -> 0, null, (address, value) -> { }, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Link.execute(0, 0, () -> 0, value -> { }, null, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Link.execute(0, 0, () -> 0, value -> { }, (address, value) -> { }, null))
        );
    }
}
