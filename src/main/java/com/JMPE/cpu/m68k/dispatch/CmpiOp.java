package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmp;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code CMPI} instructions.
 */
public final class CmpiOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        Operands operands = validate(decoded);
        return Cmp.execute(
                decoded.size(),
                operands.source()::value,
                () -> cpu.registers().data(operands.destination().reg()),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static Operands validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.CMPI) {
            throw new IllegalArgumentException("CmpiOp requires opcode CMPI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("CMPI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate immediate)) {
            throw new IllegalArgumentException(
                    "CMPI runtime currently supports immediate source only but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("CMPI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("CMPI must not carry an extension payload");
        }
        if (!(decoded.dst() instanceof EffectiveAddress.DataReg dataRegister)) {
            throw new IllegalArgumentException(
                    "CMPI runtime currently supports data-register-direct destination only but was " + decoded.dst()
            );
        }
        return new Operands(immediate, dataRegister);
    }

    private record Operands(EffectiveAddress.Immediate source,
                            EffectiveAddress.DataReg destination) {
    }
}
