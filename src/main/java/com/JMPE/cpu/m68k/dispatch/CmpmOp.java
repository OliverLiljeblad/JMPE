package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmp;

import java.util.Objects;

public final class CmpmOp implements Op {
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
        DispatchSupport.requireOpcode(decoded, Opcode.CMPM, "CMPM");
        DispatchSupport.requireSized(decoded, "CMPM");
        DispatchSupport.requireSource(decoded, "CMPM");
        DispatchSupport.requireDestination(decoded, "CMPM");
        DispatchSupport.requireNoExtension(decoded, "CMPM");
        if (!(decoded.src() instanceof EffectiveAddress.AddrRegIndPostInc)) {
            throw new IllegalArgumentException("CMPM requires postincrement source but was " + decoded.src());
        }
        if (!(decoded.dst() instanceof EffectiveAddress.AddrRegIndPostInc)) {
            throw new IllegalArgumentException("CMPM requires postincrement destination but was " + decoded.dst());
        }
    }
}
