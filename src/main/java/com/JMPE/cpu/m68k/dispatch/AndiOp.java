package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.And;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code ANDI} instructions.
 *
 * <p>
 * The raw {@link And} helper only knows how to bitwise-AND a sized source and
 * destination operand, write the result, and update CCR flags. {@code AndiOp}
 * is the runtime bridge that validates the decoded shape and resolves the
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
public final class AndiOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        Operands operands = validate(decoded);
        return And.execute(
                decoded.size(),
                operands.source()::value,
                () -> cpu.registers().data(operands.destination().reg()),
                value -> DataRegisterWriter.write(cpu, operands.destination().reg(), decoded.size(), value),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static Operands validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.ANDI) {
            throw new IllegalArgumentException("AndiOp requires opcode ANDI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("ANDI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate immediate)) {
            throw new IllegalArgumentException(
                    "ANDI runtime currently supports immediate source only but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("ANDI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("ANDI must not carry an extension payload");
        }
        if (!(decoded.dst() instanceof EffectiveAddress.DataReg dataRegister)) {
            throw new IllegalArgumentException(
                    "ANDI runtime currently supports data-register-direct destination only but was " + decoded.dst()
            );
        }
        return new Operands(immediate, dataRegister);
    }

    private record Operands(EffectiveAddress.Immediate source,
                            EffectiveAddress.DataReg destination) {
    }
}
