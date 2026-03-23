package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Nop;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code NOP} instructions.
 *
 * <p>
 * The plain {@link Nop} helper only models the raw instruction semantics:
 * "do nothing and cost 4 cycles". {@code NopOp} exists one layer above that.
 * It is the runtime adapter that says "this decoded instruction is a NOP, so
 * the CPU pipeline should execute the NOP helper here".
 * </p>
 *
 * <p>
 * Keeping this adapter separate preserves the current project structure:
 * </p>
 * <ul>
 *   <li>{@code Decoder} identifies the instruction and builds a {@link DecodedInstruction}</li>
 *   <li>{@link DispatchTable} selects the matching {@link Op}</li>
 *   <li>{@code NopOp} validates the decoded shape and bridges to {@link Nop}</li>
 *   <li>{@link Nop} stays a small, reusable instruction-semantics helper</li>
 * </ul>
 */
public final class NopOp implements Op {
    @Override
    public int execute(M68kCpu cpu, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        return Nop.execute();
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.NOP) {
            throw new IllegalArgumentException("NopOp requires opcode NOP but was " + decoded.opcode());
        }
        if (!decoded.isUnsized()) {
            throw new IllegalArgumentException("NOP must be decoded as UNSIZED");
        }
        if (!decoded.hasNoSource()) {
            throw new IllegalArgumentException("NOP must not have a source operand");
        }
        if (!decoded.hasNoDestination()) {
            throw new IllegalArgumentException("NOP must not have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("NOP must not carry an extension payload");
        }
    }
}
