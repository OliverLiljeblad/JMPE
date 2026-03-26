package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Clr;

import java.util.Objects;

/**
 * Dispatch-layer executor shell for decoded {@code CLR} instructions.
 *
 * <p>
 * This class is intentionally being wired in incremental steps. At this stage,
 * it only establishes the runtime adapter shape used by {@link DispatchTable}:
 * an {@link Op} implementation that receives the live CPU plus the decoded
 * instruction.
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
                value -> DataRegisterWriter.write(cpu, destination.reg(), decoded.size(), value),
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

}
