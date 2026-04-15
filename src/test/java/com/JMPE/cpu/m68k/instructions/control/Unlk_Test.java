package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Unlk_Test {
    @Test
    void restoresFrameRegisterAndPopsStackFrame() {
        AtomicInteger stackPointer = new AtomicInteger(0x0000_10F4);
        AtomicInteger restoredRegister = new AtomicInteger(-1);
        AtomicInteger readAddress = new AtomicInteger(-1);

        int cycles = Unlk.execute(
            0x0000_10FC,
            stackPointer::set,
            address -> {
                readAddress.set(address);
                return 0x1234_5678;
            },
            restoredRegister::set
        );

        assertAll(
            () -> assertEquals(Unlk.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_10FC, readAddress.get()),
            () -> assertEquals(0x1234_5678, restoredRegister.get()),
            () -> assertEquals(0x0000_1100, stackPointer.get())
        );
    }

    @Test
    void rejectsNullCollaborators() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> Unlk.execute(0, null, address -> 0, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Unlk.execute(0, value -> { }, null, value -> { })),
            () -> assertThrows(NullPointerException.class, () -> Unlk.execute(0, value -> { }, address -> 0, null))
        );
    }
}
