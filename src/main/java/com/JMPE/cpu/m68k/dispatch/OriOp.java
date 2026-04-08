package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
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
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location dst = OperandResolver.resolveLocation(decoded.dst(), cpu, bus, decoded.size());
        return Or.execute(
                decoded.size(),
                () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
                dst::read,
                dst::write,
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.ORI) {
            throw new IllegalArgumentException("OriOp requires opcode ORI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("ORI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate)) {
            throw new IllegalArgumentException(
                    "ORI requires immediate source but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("ORI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("ORI must not carry an extension payload");
        }
    }
}
