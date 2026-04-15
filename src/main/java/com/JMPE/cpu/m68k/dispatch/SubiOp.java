package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subi;

import java.util.Objects;

public final class SubiOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        EffectiveAddress.Immediate immediate = DispatchSupport.requireImmediateSource(decoded, "SUBI");
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Subi.execute(
            decoded.size(),
            immediate::value,
            destination::read,
            destination::write,
            cpu.statusRegister().subConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.SUBI, "SUBI");
        DispatchSupport.requireSized(decoded, "SUBI");
        DispatchSupport.requireDestination(decoded, "SUBI");
        DispatchSupport.requireNoExtension(decoded, "SUBI");
        DispatchSupport.requireImmediateSource(decoded, "SUBI");
    }
}
