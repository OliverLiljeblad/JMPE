package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Not;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code NOT} instructions.
 *
 * <p>
 * The raw {@link Not} helper only knows how to complement a sized destination
 * operand and update CCR flags. {@code NotOp} is the runtime bridge that
 * validates the decoded shape and resolves the currently supported
 * data-register-direct destination from live CPU state.
 * </p>
 *
 * <p>
 * Like the current {@link ClrOp} and {@link TstOp} runtime milestones, this
 * adapter intentionally supports only data-register-direct destinations until a
 * shared bus-backed effective-address read/write layer exists.
 * </p>
 */
public final class NotOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        EffectiveAddress.DataReg destination = validate(decoded);
        return Not.execute(
                decoded.size(),
                () -> cpu.registers().data(destination.reg()),
                value -> DataRegisterWriter.write(cpu, destination.reg(), decoded.size(), value),
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static EffectiveAddress.DataReg validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.NOT) {
            throw new IllegalArgumentException("NotOp requires opcode NOT but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("NOT must be decoded with a sized operand");
        }
        if (!decoded.hasNoSource()) {
            throw new IllegalArgumentException("NOT must not have a source operand");
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("NOT must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("NOT must not carry an extension payload");
        }
        if (!(decoded.dst() instanceof EffectiveAddress.DataReg dataRegister)) {
            throw new IllegalArgumentException(
                    "NOT runtime currently supports data-register-direct destination only but was " + decoded.dst()
            );
        }
        return dataRegister;
    }

}
