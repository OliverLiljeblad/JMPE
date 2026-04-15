package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Roxl;

import java.util.Objects;

public final class RoxlOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ROXL, "ROXL");
        DispatchSupport.requireSized(decoded, "ROXL");
        DispatchSupport.requireDestination(decoded, "ROXL");
        DispatchSupport.requireNoExtension(decoded, "ROXL");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Roxl.execute(
            decoded.size(),
            DispatchSupport.shiftCount(cpu, bus, decoded),
            cpu.statusRegister().isExtendSet(),
            destination::read,
            destination::write,
            cpu.statusRegister().roxlConditionCodes()
        );
    }
}
