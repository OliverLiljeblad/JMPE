package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.bit.Btst;

import java.util.Objects;

public final class BtstOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        return Btst.execute(
            decoded.size(),
            () -> OperandResolver.read(decoded.src(), cpu, bus, decoded.size()),
            () -> OperandResolver.read(decoded.dst(), cpu, bus, decoded.size()),
            cpu.statusRegister()::setZero
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.BTST, "BTST");
        DispatchSupport.requireSized(decoded, "BTST");
        DispatchSupport.requireSource(decoded, "BTST");
        DispatchSupport.requireDestination(decoded, "BTST");
        DispatchSupport.requireNoExtension(decoded, "BTST");
        if (decoded.size() != Size.BYTE && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("BTST must be decoded as BYTE or LONG");
        }
    }
}
