package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.And;

import java.util.Objects;

public final class AndOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.AND, "AND");
        DispatchSupport.requireSized(decoded, "AND");
        DispatchSupport.requireSource(decoded, "AND");
        DispatchSupport.requireDestination(decoded, "AND");
        DispatchSupport.requireNoExtension(decoded, "AND");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return And.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            destination::read,
            destination::write,
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
