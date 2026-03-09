package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RegistersTest {
    @Test
    void readsAndWritesDataAddressAndProgramCounter() {
        Registers registers = new Registers();

        registers.setData(0, 0x1234_5678);
        registers.setAddress(3, 0x00FF_1000);
        registers.setProgramCounter(0x0040_0200);
        registers.setStackPointer(0x0000_2000);

        assertEquals(0x1234_5678, registers.data(0));
        assertEquals(0x00FF_1000, registers.address(3));
        assertEquals(0x0000_2000, registers.address(Registers.STACK_POINTER_REGISTER));
        assertEquals(0x0000_2000, registers.stackPointer());
        assertEquals(0x0040_0200, registers.programCounter());
    }

    @Test
    void validatesDataAndAddressIndices() {
        Registers registers = new Registers();

        assertThrows(IllegalArgumentException.class, () -> registers.data(-1));
        assertThrows(IllegalArgumentException.class, () -> registers.data(8));
        assertThrows(IllegalArgumentException.class, () -> registers.address(-1));
        assertThrows(IllegalArgumentException.class, () -> registers.address(8));
    }

    @Test
    void returnsDefensiveCopiesOfRegisterArrays() {
        Registers registers = new Registers();
        registers.setData(1, 0x1111_1111);
        registers.setAddress(1, 0x2222_2222);

        int[] dataCopy = registers.copyDataRegisters();
        int[] addressCopy = registers.copyAddressRegisters();
        dataCopy[1] = 0;
        addressCopy[1] = 0;

        assertEquals(0x1111_1111, registers.data(1));
        assertEquals(0x2222_2222, registers.address(1));
        assertArrayEquals(new int[] {0, 0x1111_1111, 0, 0, 0, 0, 0, 0}, registers.copyDataRegisters());
        assertArrayEquals(new int[] {0, 0x2222_2222, 0, 0, 0, 0, 0, 0}, registers.copyAddressRegisters());
    }
}
