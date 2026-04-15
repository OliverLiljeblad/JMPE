package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Suba;

import java.util.Objects;

public final class SubaOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.SUBA, "SUBA");
        DispatchSupport.requireSource(decoded, "SUBA");
        DispatchSupport.requireDestination(decoded, "SUBA");
        DispatchSupport.requireNoExtension(decoded, "SUBA");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("SUBA must be decoded as WORD or LONG");
        }

        int destinationRegister = DispatchSupport.requireAddressRegister(decoded.dst(), "destination", "SUBA");
        return Suba.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> cpu.registers().address(destinationRegister),
            value -> cpu.registers().setAddress(destinationRegister, value)
        );
    }
}
