package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Lsr;

import java.util.Objects;

public final class LsrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.LSR, "LSR");
        DispatchSupport.requireSized(decoded, "LSR");
        DispatchSupport.requireDestination(decoded, "LSR");
        DispatchSupport.requireNoExtension(decoded, "LSR");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Lsr.execute(
            decoded.size(),
            DispatchSupport.shiftCount(cpu, bus, decoded),
            destination::read,
            destination::write,
            cpu.statusRegister().lsrConditionCodes()
        );
    }
}
