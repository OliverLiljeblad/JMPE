package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Neg;

import java.util.Objects;

public final class NegOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Neg.execute(
            decoded.size(),
            destination::read,
            destination::write,
            cpu.statusRegister().subConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.NEG, "NEG");
        DispatchSupport.requireSized(decoded, "NEG");
        DispatchSupport.requireNoSource(decoded, "NEG");
        DispatchSupport.requireDestination(decoded, "NEG");
        DispatchSupport.requireNoExtension(decoded, "NEG");
    }
}
