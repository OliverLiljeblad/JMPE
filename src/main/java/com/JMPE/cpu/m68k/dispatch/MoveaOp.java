package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveaOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVEA, "MOVEA");
        DispatchSupport.requireSource(decoded, "MOVEA");
        DispatchSupport.requireDestination(decoded, "MOVEA");
        DispatchSupport.requireNoExtension(decoded, "MOVEA");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("MOVEA must be decoded as a WORD or LONG operation");
        }

        int destinationRegister = DispatchSupport.requireAddressRegister(decoded.dst(), "destination", "MOVEA");
        int sourceValue = DispatchSupport.readSource(decoded, cpu, bus);
        cpu.registers().setAddress(destinationRegister, decoded.size() == Size.WORD ? (short) sourceValue : sourceValue);
        return DispatchSupport.moveCycles();
    }
}
