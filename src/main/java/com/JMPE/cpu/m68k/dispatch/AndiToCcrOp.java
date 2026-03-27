package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code ANDI #imm,CCR}.
 */
public final class AndiToCcrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        EffectiveAddress.Immediate immediate = validate(decoded);
        int result = Size.BYTE.mask(cpu.statusRegister().conditionCodeRegister() & immediate.value());
        cpu.statusRegister().setConditionCodeRegister(result);
        return EXECUTION_CYCLES;
    }

    private static EffectiveAddress.Immediate validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.ANDI_TO_CCR) {
            throw new IllegalArgumentException("AndiToCcrOp requires opcode ANDI_TO_CCR but was " + decoded.opcode());
        }
        if (decoded.size() != Size.BYTE) {
            throw new IllegalArgumentException("ANDI_TO_CCR must be decoded as a BYTE operation");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate immediate)) {
            throw new IllegalArgumentException(
                    "ANDI_TO_CCR runtime currently supports immediate source only but was " + decoded.src()
            );
        }
        if (!(decoded.dst() instanceof EffectiveAddress.Ccr)) {
            throw new IllegalArgumentException(
                    "ANDI_TO_CCR requires CCR as the destination but was " + decoded.dst()
            );
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("ANDI_TO_CCR must not carry an extension payload");
        }
        return immediate;
    }
}
