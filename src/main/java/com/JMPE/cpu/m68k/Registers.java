package com.JMPE.cpu.m68k;

import java.util.Arrays;

/**
 * Motorola 68000 programmer-visible register file.
 * <p>
 * The 68000 exposes eight data registers (D0-D7), eight address registers (A0-A7),
 * and a 32-bit program counter. This model provides explicit typed accessors with
 * defensive index checks so decoder/execution code can fail fast on invalid register ids.
 * </p>
 */
public final class Registers {
    public static final int DATA_REGISTER_COUNT = 8;
    public static final int ADDRESS_REGISTER_COUNT = 8;
    public static final int STACK_POINTER_REGISTER = 7;

    private final int[] dataRegisters = new int[DATA_REGISTER_COUNT];
    private final int[] addressRegisters = new int[ADDRESS_REGISTER_COUNT];
    private int programCounter;

    public int data(int index) {
        return dataRegisters[validateDataIndex(index)];
    }

    public void setData(int index, int value) {
        dataRegisters[validateDataIndex(index)] = value;
    }

    public int address(int index) {
        return addressRegisters[validateAddressIndex(index)];
    }

    public void setAddress(int index, int value) {
        addressRegisters[validateAddressIndex(index)] = value;
    }

    public int stackPointer() {
        return addressRegisters[STACK_POINTER_REGISTER];
    }

    public void setStackPointer(int value) {
        addressRegisters[STACK_POINTER_REGISTER] = value;
    }

    public int programCounter() {
        return programCounter;
    }

    public void setProgramCounter(int value) {
        programCounter = value;
    }

    public int[] copyDataRegisters() {
        return Arrays.copyOf(dataRegisters, DATA_REGISTER_COUNT);
    }

    public int[] copyAddressRegisters() {
        return Arrays.copyOf(addressRegisters, ADDRESS_REGISTER_COUNT);
    }

    private int validateDataIndex(int index) {
        if (index < 0 || index >= DATA_REGISTER_COUNT) {
            throw new IllegalArgumentException("Invalid data register index D" + index);
        }
        return index;
    }

    private int validateAddressIndex(int index) {
        if (index < 0 || index >= ADDRESS_REGISTER_COUNT) {
            throw new IllegalArgumentException("Invalid address register index A" + index);
        }
        return index;
    }
}
