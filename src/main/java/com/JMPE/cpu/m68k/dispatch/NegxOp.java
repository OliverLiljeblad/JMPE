package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Negx;

import java.util.Objects;

public final class NegxOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Negx.execute(
            decoded.size(),
            destination::read,
            destination::write,
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().isZeroSet(),
            cpu.statusRegister().subConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.NEGX, "NEGX");
        DispatchSupport.requireSized(decoded, "NEGX");
        DispatchSupport.requireNoSource(decoded, "NEGX");
        DispatchSupport.requireDestination(decoded, "NEGX");
        DispatchSupport.requireNoExtension(decoded, "NEGX");
    }
}
