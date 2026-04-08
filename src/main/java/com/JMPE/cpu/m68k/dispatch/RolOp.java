package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Rol;

import java.util.Objects;

public final class RolOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ROL, "ROL");
        DispatchSupport.requireSized(decoded, "ROL");
        DispatchSupport.requireDestination(decoded, "ROL");
        DispatchSupport.requireNoExtension(decoded, "ROL");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Rol.execute(
            decoded.size(),
            DispatchSupport.shiftCount(cpu, bus, decoded),
            destination::read,
            destination::write,
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
