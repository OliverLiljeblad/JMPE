package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Tas;

import java.util.Objects;

public final class TasOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.TAS, "TAS");
        DispatchSupport.requireSize(decoded, Size.BYTE, "TAS");
        DispatchSupport.requireNoSource(decoded, "TAS");
        DispatchSupport.requireDestination(decoded, "TAS");
        DispatchSupport.requireNoExtension(decoded, "TAS");
        DispatchSupport.requireDataAlterable(decoded.dst(), "destination", "TAS");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Tas.execute(destination::read, destination::write, cpu.statusRegister().moveConditionCodes());
    }
}
