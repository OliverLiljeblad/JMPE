package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmp;

import java.util.Objects;

public final class CmpOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.CMP, "CMP");
        DispatchSupport.requireSized(decoded, "CMP");
        DispatchSupport.requireSource(decoded, "CMP");
        DispatchSupport.requireDestination(decoded, "CMP");
        DispatchSupport.requireNoExtension(decoded, "CMP");

        return Cmp.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> OperandResolver.read(decoded.dst(), cpu, bus, decoded.size()),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
