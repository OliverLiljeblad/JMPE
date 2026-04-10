package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Ror;

import java.util.Objects;

public final class RorOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ROR, "ROR");
        DispatchSupport.requireSized(decoded, "ROR");
        DispatchSupport.requireDestination(decoded, "ROR");
        DispatchSupport.requireNoExtension(decoded, "ROR");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Ror.execute(
            decoded.size(),
            DispatchSupport.shiftCount(cpu, bus, decoded),
            destination::read,
            destination::write,
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
