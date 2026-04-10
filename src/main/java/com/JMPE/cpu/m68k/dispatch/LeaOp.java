package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Lea;

import java.util.Objects;

public final class LeaOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.LEA, "LEA");
        DispatchSupport.requireSize(decoded, Size.LONG, "LEA");
        DispatchSupport.requireSource(decoded, "LEA");
        DispatchSupport.requireDestination(decoded, "LEA");
        DispatchSupport.requireNoExtension(decoded, "LEA");

        int destinationRegister = DispatchSupport.requireAddressRegister(decoded.dst(), "destination", "LEA");
        int effectiveAddress = DispatchSupport.computeAddress(decoded.src(), cpu);
        return Lea.execute(destinationRegister, effectiveAddress, cpu.registers()::setAddress);
    }
}
