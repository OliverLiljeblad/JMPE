package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
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
 * Validates the decoded shape (immediate source, alterable destination)
 * and bridges into {@link And#execute} via {@link OperandResolver}.
 * Supports all alterable addressing modes for the destination.
 * </p>
 */
public final class AndiOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location dst = OperandResolver.resolveLocation(decoded.dst(), cpu, bus, decoded.size());
        return And.execute(
                decoded.size(),
                () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
                dst::read,
                dst::write,
                cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.ANDI) {
            throw new IllegalArgumentException("AndiOp requires opcode ANDI but was " + decoded.opcode());
        }
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException("ANDI must be decoded with a sized operand");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate)) {
            throw new IllegalArgumentException(
                    "ANDI requires immediate source but was " + decoded.src()
            );
        }
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException("ANDI must have a destination operand");
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("ANDI must not carry an extension payload");
        }
    }
}
