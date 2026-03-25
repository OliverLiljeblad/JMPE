package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Clr;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code CLR} instructions.
 *
 * <p>
 * This implementation validates that the decoded instruction is a proper
 * {@link Opcode#CLR} with a sized operand, no source, a destination operand,
 * and no extension payload, and then executes the operation.
 * </p>
 *
 * <p>
 * Runtime execution is currently limited to a data-register-direct destination
 * ({@link EffectiveAddress.DataReg}). Other addressing modes should be
 * rejected by the validation logic above until explicit support is added.
 * As additional CLR addressing modes are wired into the dispatch layer, this
 * Javadoc should be kept in sync with the supported behavior and limitations.
 * </p>
 */
public final class ClrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        EffectiveAddress.DataReg destination = validate(decoded);
        return Clr.execute(
                decoded.size(),
                value -> writeDataRegister(cpu, destination.reg(), decoded.size(), value),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static EffectiveAddress.DataReg validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.CLR) {
            throw new IllegalArgumentException("ClrOp requires opcode CLR but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("CLR must be decoded with a sized operand");
        }
        if (!decoded.hasNoSource()) {
            throw new IllegalArgumentException("CLR must not have a source operand");
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("CLR must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("CLR must not carry an extension payload");
        }
        if (!(decoded.dst() instanceof EffectiveAddress.DataReg dataRegister)) {
            throw new IllegalArgumentException(
                    "CLR runtime currently supports data-register-direct destination only but was " + decoded.dst()
            );
        }
        return dataRegister;
    }

    private static void writeDataRegister(M68kCpu cpu, int register, Size size, int value) {
        int currentValue = cpu.registers().data(register);
        int maskedValue = size.mask(value);
        int nextValue = switch (size) {
            case BYTE -> (currentValue & 0xFFFF_FF00) | maskedValue;
            case WORD -> (currentValue & 0xFFFF_0000) | maskedValue;
            case LONG -> maskedValue;
            case UNSIZED -> throw new IllegalArgumentException("CLR data-register writes require a sized operation");
        };
        cpu.registers().setData(register, nextValue);
    }
}
