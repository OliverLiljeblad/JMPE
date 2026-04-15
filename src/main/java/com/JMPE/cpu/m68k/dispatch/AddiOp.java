package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addi;

import java.util.Objects;

public final class AddiOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        EffectiveAddress.Immediate immediate = DispatchSupport.requireImmediateSource(decoded, "ADDI");
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Addi.execute(
            decoded.size(),
            immediate::value,
            destination::read,
            destination::write,
            cpu.statusRegister().addConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.ADDI, "ADDI");
        DispatchSupport.requireSized(decoded, "ADDI");
        DispatchSupport.requireDestination(decoded, "ADDI");
        DispatchSupport.requireNoExtension(decoded, "ADDI");
        DispatchSupport.requireImmediateSource(decoded, "ADDI");
    }
}
