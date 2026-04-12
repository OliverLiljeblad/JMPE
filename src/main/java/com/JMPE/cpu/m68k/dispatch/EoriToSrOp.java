package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code EORI #imm,SR}.
 */
public final class EoriToSrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        EffectiveAddress.Immediate immediate = validate(decoded);
        if (!cpu.statusRegister().isSupervisorSet()) {
            throw new PrivilegeViolation("EORI to SR");
        }
        int result = Size.WORD.mask(cpu.statusRegister().rawValue() ^ immediate.value());
        cpu.statusRegister().setRawValue(result);
        return EXECUTION_CYCLES;
    }

    private static EffectiveAddress.Immediate validate(DecodedInstruction decoded) {
        if (decoded.opcode() != Opcode.EORI_TO_SR) {
            throw new IllegalArgumentException("EoriToSrOp requires opcode EORI_TO_SR but was " + decoded.opcode());
        }
        if (decoded.size() != Size.WORD) {
            throw new IllegalArgumentException("EORI_TO_SR must be decoded as a WORD operation");
        }
        if (!(decoded.src() instanceof EffectiveAddress.Immediate immediate)) {
            throw new IllegalArgumentException(
                    "EORI_TO_SR runtime currently supports immediate source only but was " + decoded.src()
            );
        }
        if (!(decoded.dst() instanceof EffectiveAddress.Sr)) {
            throw new IllegalArgumentException(
                    "EORI_TO_SR requires SR as the destination but was " + decoded.dst()
            );
        }
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException("EORI_TO_SR must not carry an extension payload");
        }
        return immediate;
    }
}
