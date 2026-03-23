package com.JMPE.cpu.m68k.instructions.data;

import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 MOVEM instruction.
 * This helper transfers an already-decoded register list between registers and consecutive memory locations.
 * Register-mask decoding, effective-address resolution, and address-register writeback are handled by the CPU core.
 */
public final class Movem {
    public static final int EXECUTION_CYCLES = 8;
    private static final int DATA_REGISTER_COUNT = 8;
    private static final int ADDRESS_REGISTER_BASE = 8;
    private static final int VALID_MASK = 0xFFFF;
    private static final int[] FORWARD_ORDER = {
            0, 1, 2, 3, 4, 5, 6, 7,
            8, 9, 10, 11, 12, 13, 14, 15
    };
    private static final int[] PREDECREMENT_ORDER = {
            15, 14, 13, 12, 11, 10, 9, 8,
            7, 6, 5, 4, 3, 2, 1, 0
    };

    private Movem() {
    }

    public enum AddressingMode {
        CONTROL,
        PREDECREMENT,
        POSTINCREMENT
    }

    @FunctionalInterface
    public interface RegisterReader {
        int read(int registerIndex);
    }

    @FunctionalInterface
    public interface RegisterWriter {
        void write(int registerIndex, int value);
    }

    @FunctionalInterface
    public interface MemoryReader {
        int read(int address, Move.Size size);
    }

    @FunctionalInterface
    public interface MemoryWriter {
        void write(int address, Move.Size size, int value);
    }

    public static int executeRegistersToMemory(
            Move.Size size,
            AddressingMode addressingMode,
            int registerMask,
            int startAddress,
            int effectiveAddressRegister,
            RegisterReader registerReader,
            MemoryWriter memoryWriter
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(addressingMode, "addressingMode must not be null");
        Objects.requireNonNull(registerReader, "registerReader must not be null");
        Objects.requireNonNull(memoryWriter, "memoryWriter must not be null");

        validateSize(size);
        validateEffectiveAddressRegister(effectiveAddressRegister);
        if (addressingMode == AddressingMode.POSTINCREMENT) {
            throw new IllegalArgumentException("POSTINCREMENT is not valid for register-to-memory MOVEM");
        }

        int[] registerOrder = addressingMode == AddressingMode.PREDECREMENT ? PREDECREMENT_ORDER : FORWARD_ORDER;
        int normalizedMask = registerMask & VALID_MASK;
        int address = startAddress;
        for (int bitIndex = 0; bitIndex < registerOrder.length; bitIndex++) {
            if (((normalizedMask >>> bitIndex) & 1) == 0) {
                continue;
            }

            int registerIndex = registerOrder[bitIndex];
            int value = size.mask(registerReader.read(registerIndex));
            memoryWriter.write(address, size, value);
            address += size.bytes();
        }

        return EXECUTION_CYCLES;
    }

    public static int executeMemoryToRegisters(
            Move.Size size,
            AddressingMode addressingMode,
            int registerMask,
            int startAddress,
            int effectiveAddressRegister,
            MemoryReader memoryReader,
            RegisterWriter registerWriter
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(addressingMode, "addressingMode must not be null");
        Objects.requireNonNull(memoryReader, "memoryReader must not be null");
        Objects.requireNonNull(registerWriter, "registerWriter must not be null");

        validateSize(size);
        validateEffectiveAddressRegister(effectiveAddressRegister);
        if (addressingMode == AddressingMode.PREDECREMENT) {
            throw new IllegalArgumentException("PREDECREMENT is not valid for memory-to-register MOVEM");
        }

        int normalizedMask = registerMask & VALID_MASK;
        int finalPostincrementAddress = startAddress + (Integer.bitCount(normalizedMask) * size.bytes());
        int address = startAddress;
        for (int bitIndex = 0; bitIndex < FORWARD_ORDER.length; bitIndex++) {
            if (((normalizedMask >>> bitIndex) & 1) == 0) {
                continue;
            }

            int registerIndex = FORWARD_ORDER[bitIndex];
            int memoryValue = memoryReader.read(address, size);
            int registerValue = size == Move.Size.WORD
                    ? signExtendWord(memoryValue)
                    : size.mask(memoryValue);
            if (addressingMode == AddressingMode.POSTINCREMENT
                    && effectiveAddressRegister >= 0
                    && registerIndex == ADDRESS_REGISTER_BASE + effectiveAddressRegister) {
                registerValue = finalPostincrementAddress;
            }

            registerWriter.write(registerIndex, registerValue);
            address += size.bytes();
        }

        return EXECUTION_CYCLES;
    }

    private static void validateSize(Move.Size size) {
        if (size == Move.Size.BYTE) {
            throw new IllegalArgumentException("MOVEM only supports WORD and LONG sizes");
        }
    }

    private static void validateEffectiveAddressRegister(int effectiveAddressRegister) {
        if (effectiveAddressRegister < -1 || effectiveAddressRegister >= DATA_REGISTER_COUNT) {
            throw new IllegalArgumentException("effectiveAddressRegister must be -1 or in range 0..7");
        }
    }

    private static int signExtendWord(int value) {
        return (short) (value & 0xFFFF);
    }
}
