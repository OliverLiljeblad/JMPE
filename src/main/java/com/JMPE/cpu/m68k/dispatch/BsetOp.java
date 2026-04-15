package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.bit.Bset;

import java.util.Objects;

public final class BsetOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Bset.execute(
            decoded.size(),
            () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
            destination::read,
            destination::write,
            cpu.statusRegister()::setZero
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.BSET, "BSET");
        DispatchSupport.requireSized(decoded, "BSET");
        DispatchSupport.requireSource(decoded, "BSET");
        DispatchSupport.requireDestination(decoded, "BSET");
        DispatchSupport.requireNoExtension(decoded, "BSET");
        if (decoded.size() != Size.BYTE && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("BSET must be decoded as BYTE or LONG");
        }
    }
}
