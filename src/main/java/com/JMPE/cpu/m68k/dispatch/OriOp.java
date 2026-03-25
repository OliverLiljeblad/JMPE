package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Or;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code ORI} instructions.
 *
 * <p>
 * The raw {@link Or} helper only knows how to bitwise-OR a sized source and
 * destination operand, write the result, and update CCR flags. {@code OriOp} is
 * the runtime bridge that validates the decoded shape and resolves the
 * currently supported immediate-source/data-register-destination form from live
 * CPU state.
 * </p>
 *
 * <p>
 * Like the other current runtime-backed instruction adapters, this first
 * milestone intentionally supports only the data-register-direct destination
 * form. Broader effective-address support can follow once a shared bus-backed
 * operand access layer exists in the dispatch path.
 * </p>
 */
public final class OriOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        Operands operands = validate(decoded);
        return Or.execute(
                decoded.size(),
                operands.source()::value,
                () -> cpu.registers().data(operands.destination().reg()),
                value -> writeDataRegister(cpu, operands.destination().reg(), decoded.size(), value),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static Operands validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.ORI) {
            throw new IllegalArgumentException("OriOp requires opcode ORI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("ORI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate immediate)) {
            throw new IllegalArgumentException(
                    "ORI runtime currently supports immediate source only but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("ORI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("ORI must not carry an extension payload");
        }
        if (!(decoded.dst() instanceof EffectiveAddress.DataReg dataRegister)) {
            throw new IllegalArgumentException(
                    "ORI runtime currently supports data-register-direct destination only but was " + decoded.dst()
            );
        }
        return new Operands(immediate, dataRegister);
    }

    private static void writeDataRegister(M68kCpu cpu, int register, Size size, int value) {
        int currentValue = cpu.registers().data(register);
        int maskedValue = size.mask(value);
        int nextValue = switch (size) {
            case BYTE -> (currentValue & 0xFFFF_FF00) | maskedValue;
            case WORD -> (currentValue & 0xFFFF_0000) | maskedValue;
            case LONG -> maskedValue;
            case UNSIZED -> throw new IllegalArgumentException("ORI data-register writes require a sized operation");
        };
        cpu.registers().setData(register, nextValue);
    }

    private record Operands(EffectiveAddress.Immediate source,
                            EffectiveAddress.DataReg destination) {
    }
}
