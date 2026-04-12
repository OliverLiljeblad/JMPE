package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Pea;

import java.util.Objects;

public final class PeaOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.PEA, "PEA");
        DispatchSupport.requireSize(decoded, Size.LONG, "PEA");
        DispatchSupport.requireSource(decoded, "PEA");
        DispatchSupport.requireNoDestination(decoded, "PEA");
        DispatchSupport.requireNoExtension(decoded, "PEA");

        int effectiveAddress = DispatchSupport.computeAddress(decoded.src(), cpu);
        return Pea.execute(effectiveAddress, value -> DispatchSupport.pushLong(cpu, bus, value));
    }
}
