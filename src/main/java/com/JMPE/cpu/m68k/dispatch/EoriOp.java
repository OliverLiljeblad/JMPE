package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Eor;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code EORI} instructions.
 *
 * <p>
 * The raw {@link Eor} helper only knows how to bitwise-exclusive-OR a sized source and destination
 * operand, write the result, and update CCR flags. {@code EoriOp} is the runtime bridge that
 * validates the decoded shape and resolves the currently supported immediate-source/data-register-
 * destination form from live CPU state.
 * </p>
 */
public final class EoriOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location dst = OperandResolver.resolveLocation(decoded.dst(), cpu, bus, decoded.size());
        return Eor.execute(
                decoded.size(),
                () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
                dst::read,
                dst::write,
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.EORI) {
            throw new IllegalArgumentException("EoriOp requires opcode EORI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("EORI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate)) {
            throw new IllegalArgumentException(
                    "EORI requires immediate source but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("EORI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("EORI must not carry an extension payload");
        }
    }
}
