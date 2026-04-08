package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
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
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        return Cmp.execute(
                decoded.size(),
                () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
                () -> OperandResolver.read(decoded.dst(), cpu, bus, decoded.size()),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.CMPI) {
            throw new IllegalArgumentException("CmpiOp requires opcode CMPI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("CMPI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate)) {
            throw new IllegalArgumentException(
                    "CMPI requires immediate source but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("CMPI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("CMPI must not carry an extension payload");
        }
    }
}
