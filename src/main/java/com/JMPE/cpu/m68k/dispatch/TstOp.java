package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Tst;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code TST} instructions.
 *
 * <p>
 * The raw {@link Tst} helper only knows how to apply TST semantics to a sized
 * operand reader and CCR sink. {@code TstOp} is the runtime bridge that
 * validates the decoded shape and resolves the currently supported operand
 * form from live CPU state.
 * </p>
 *
 * <p>
 * This first runtime milestone intentionally supports only data-register-direct
 * TST forms. That keeps the branch aligned with the current no-RAM execution
 * goal and avoids widening the runtime before a shared bus-backed
 * effective-address reader exists.
 * </p>
 */
public final class TstOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        return Tst.execute(
                decoded.size(),
                () -> OperandResolver.read(decoded.dst(), cpu, bus, decoded.size()),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.TST) {
            throw new IllegalArgumentException("TstOp requires opcode TST but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("TST must be decoded with a sized operand");
        }
        if (!decoded.hasNoSource()) {
            throw new IllegalArgumentException("TST must not have a source operand");
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("TST must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("TST must not carry an extension payload");
        }
    }
}
