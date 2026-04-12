package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Eor;

import java.util.Objects;

public final class EorOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.EOR, "EOR");
        DispatchSupport.requireSized(decoded, "EOR");
        DispatchSupport.requireSource(decoded, "EOR");
        DispatchSupport.requireDestination(decoded, "EOR");
        DispatchSupport.requireNoExtension(decoded, "EOR");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Eor.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            destination::read,
            destination::write,
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
