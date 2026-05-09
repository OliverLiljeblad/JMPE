package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Movem_Test {
    @Test
    void executeRegistersToMemory_writesSelectedRegistersInForwardOrder() {
        int[] registers = new int[16];
        registers[0] = 0x1111;  // D0 selected by bit 0
        registers[1] = 0x2222;  // D1 selected by bit 1

        List<Integer> writtenAddresses = new ArrayList<>();
        List<Integer> writtenValues = new ArrayList<>();

        // mask = 0x0003: bits 0 and 1 → D0 then D1, in forward order.
        int cycles = Movem.executeRegistersToMemory(
                Size.WORD,
                Movem.AddressingMode.CONTROL,
                0x0003,
                0x1000,
                -1,
                index -> registers[index],
                (address, size, value) -> {
                    writtenAddresses.add(address);
                    writtenValues.add(value);
                }
        );

        assertAll(
                () -> assertEquals(Movem.EXECUTION_CYCLES, cycles),
                () -> assertEquals(List.of(0x1000, 0x1002), writtenAddresses),
                () -> assertEquals(List.of(0x1111, 0x2222), writtenValues)
        );
    }

    @Test
    void executeRegistersToMemory_storesA6ThenA7InPredecrementMemoryOrder() {
        int[] registers = new int[16];
        registers[15] = 0xAAAA;  // A7
        registers[14] = 0xBBBB;  // A6

        List<Integer> writtenValues = new ArrayList<>();

        // mask = 0x0003 (predecrement encoding): bits 0,1 set → A7 and A6 included.
        // On the 68000 the stored block is laid out D0..D7,A0..A7 from low to high
        // address regardless of mode, so A6 (lower-numbered) is written first
        // (lowest address), then A7.
        Movem.executeRegistersToMemory(
                Size.WORD,
                Movem.AddressingMode.PREDECREMENT,
                0x0003,
                0x1000,
                0,
                index -> registers[index],
                (address, size, value) -> writtenValues.add(value)
        );

        assertEquals(List.of(0xBBBB, 0xAAAA), writtenValues);
    }

    @Test
    void executeRegistersToMemory_rejectsPostincrementMode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Movem.executeRegistersToMemory(
                        Size.WORD,
                        Movem.AddressingMode.POSTINCREMENT,
                        0x00FF,
                        0x1000,
                        -1,
                        index -> 0,
                        (address, size, value) -> {}
                )
        );

        assertEquals("POSTINCREMENT is not valid for register-to-memory MOVEM", exception.getMessage());
    }

    @Test
    void executeRegistersToMemory_rejectsByteSize() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Movem.executeRegistersToMemory(
                        Size.BYTE,
                        Movem.AddressingMode.CONTROL,
                        0x00FF,
                        0x1000,
                        -1,
                        index -> 0,
                        (address, size, value) -> {}
                )
        );

        assertEquals("MOVEM only supports WORD and LONG sizes", exception.getMessage());
    }

    @Test
    void executeMemoryToRegisters_writesLongValuesIntoSelectedRegisters() {
        // memory[0x1000]=0xABCD1234, memory[0x1004]=0x56789ABC
        int[] values = { 0xABCD1234, 0x56789ABC };

        int[] registers = new int[16];

        // mask = 0x0003: bits 0 and 1 → D0 (register 0) and D1 (register 1).
        int cycles = Movem.executeMemoryToRegisters(
                Size.LONG,
                Movem.AddressingMode.POSTINCREMENT,
                0x0003,
                0x1000,
                -1,
                (address, size) -> values[(address - 0x1000) / size.bytes()],
                (index, value) -> registers[index] = value
        );

        assertAll(
                () -> assertEquals(Movem.EXECUTION_CYCLES, cycles),
                () -> assertEquals(0xABCD1234, registers[0]),
                () -> assertEquals(0x56789ABC, registers[1])
        );
    }

    @Test
    void executeMemoryToRegisters_signExtendsWordValues() {
        int[] registers = new int[16];

        // mask = 0x0001: bit 0 → D0. Reading 0x8000 as WORD sign-extends to 0xFFFF8000.
        Movem.executeMemoryToRegisters(
                Size.WORD,
                Movem.AddressingMode.POSTINCREMENT,
                0x0001,
                0x1000,
                -1,
                (address, size) -> 0x8000,
                (index, value) -> registers[index] = value
        );

        assertEquals((int) 0xFFFF8000, registers[0]);
    }

    @Test
    void executeMemoryToRegisters_rejectsPredecrementMode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Movem.executeMemoryToRegisters(
                        Size.WORD,
                        Movem.AddressingMode.PREDECREMENT,
                        0x00FF,
                        0x1000,
                        -1,
                        (address, size) -> 0,
                        (index, value) -> {}
                )
        );

        assertEquals("PREDECREMENT is not valid for memory-to-register MOVEM", exception.getMessage());
    }

    @Test
    void executeMemoryToRegisters_rejectsByteSize() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Movem.executeMemoryToRegisters(
                        Size.BYTE,
                        Movem.AddressingMode.POSTINCREMENT,
                        0x00FF,
                        0x1000,
                        -1,
                        (address, size) -> 0,
                        (index, value) -> {}
                )
        );

        assertEquals("MOVEM only supports WORD and LONG sizes", exception.getMessage());
    }
}
