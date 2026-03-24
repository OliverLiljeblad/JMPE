package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.Size;
import java.util.Objects;

/**
 * Implements the execution semantics of the Motorola 68000 MOVEQ instruction.
 * MOVEQ sign-extends an 8-bit immediate value to a 32-bit long and writes it to
 * a data register, then updates the CCR flags using MOVE-family rules.
 * Instruction decoding and register selection are handled by the CPU core.
 */
public final class MoveQ {
    public static final int EXECUTION_CYCLES = 4;
    private static final int MIN_REGISTER = 0;
    private static final int MAX_REGISTER = 7;

    private MoveQ() {
    }

    /**
     * Writes a 32-bit result to a destination data register Dn.
     */
    @FunctionalInterface
    public interface DataRegisterWriter {
        void writeLong(int registerIndex, int value);
    }

    /**
     * Executes MOVEQ with decoded operands.
     *
     * <p>
     * MOVEQ sign-extends an 8-bit immediate to a 32-bit long result, writes that value to Dn,
     * then applies MOVE-family CCR behavior (N/Z from long result, V/C cleared, X unchanged).
     * </p>
     */
    public static int execute(
            int destinationRegister,
            int immediate8,
            DataRegisterWriter registerWriter,
            Move.ConditionCodes conditionCodes
    ) {
        Objects.requireNonNull(registerWriter, "registerWriter must not be null");
        Objects.requireNonNull(conditionCodes, "conditionCodes must not be null");

        validateDestinationRegister(destinationRegister);
        int longResult = signExtend8(immediate8);
        registerWriter.writeLong(destinationRegister, longResult);
        Move.updateConditionCodes(longResult, Size.LONG, conditionCodes);
        return EXECUTION_CYCLES;
    }

    /**
     * Sign-extends the low 8 bits of {@code immediate8} to a 32-bit signed value.
     */
    public static int signExtend8(int immediate8) {
        return (byte) (immediate8 & 0xFF);
    }

    private static void validateDestinationRegister(int destinationRegister) {
        if (destinationRegister < MIN_REGISTER || destinationRegister > MAX_REGISTER) {
            throw new IllegalArgumentException("destinationRegister must be in range 0..7");
        }
    }
}
