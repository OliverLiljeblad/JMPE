package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Add;

import java.util.Objects;

public final class AddOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ADD, "ADD");
        DispatchSupport.requireSized(decoded, "ADD");
        DispatchSupport.requireSource(decoded, "ADD");
        DispatchSupport.requireDestination(decoded, "ADD");
        DispatchSupport.requireNoExtension(decoded, "ADD");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Add.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            destination::read,
            destination::write,
            cpu.statusRegister().addConditionCodes()
        );
    }
}
