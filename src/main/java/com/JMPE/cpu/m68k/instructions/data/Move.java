package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Implements the execution semantics of the Motorola 68000 MOVE instruction family.
 * This helper masks the source value to the instruction size (BYTE, WORD, LONG),
 * writes the result to a provided destination writer, and updates the CCR flags.
 * Instruction decoding, effective-address resolution, and memory/register access
 * are handled elsewhere in the CPU core.
 */
public final class Move {
    public static final int DEFAULT_CYCLES = 4;

    private Move() {
    }

    /**
     * Executes MOVE semantics using a default cycle count.
     *
     * <p>
     * {@code sourceValue} may contain bits wider than {@code size}. This method masks it to the
     * instruction size, writes the masked result, and computes flags from that masked result.
     * </p>
     */
    public static int execute(
            Size size,
            int sourceValue,
            IntConsumer destinationWriter,
            ConditionCodes conditionCodes
    ) {
        return execute(size, sourceValue, destinationWriter, conditionCodes, DEFAULT_CYCLES);
    }

    /**
     * Executes MOVE semantics and returns the supplied cycle count.
     *
     * <p>
     * {@code sourceValue} may contain bits wider than {@code size}. This method masks it to the
     * instruction size before both destination write-back and CCR updates.
     * </p>
     */
    public static int execute(
            Size size,
            int sourceValue,
            IntConsumer destinationWriter,
            ConditionCodes conditionCodes,
            int cycles
    ) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(destinationWriter, "destinationWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");
        if (cycles < 0) {
            throw new IllegalArgumentException("cycles must be >= 0");
        }

        int maskedResult = size.mask(sourceValue);
        updateConditionCodes(maskedResult, size, conditionCodes);
        destinationWriter.accept(maskedResult);
        return cycles;
    }

    /**
     * Applies MOVE/MOVEQ CCR behavior to an already sized result.
     *
     * <p>
     * {@code maskedResult} is expected to already be masked to {@code size}. This method sets N
     * and Z from that sized result, clears V and C, and intentionally leaves X unchanged.
     * </p>
     */
    public static void updateConditionCodes(int maskedResult, Size size, ConditionCodes conditionCodes) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        conditionCodes.setNegative(size.isNegative(maskedResult));
        conditionCodes.setZero(size.isZero(maskedResult));
        conditionCodes.setOverflow(false);
        conditionCodes.setCarry(false);
    }
}
