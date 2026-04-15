package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Divu;

import java.util.Objects;

public final class DivuOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.DIVU, "DIVU");
        DispatchSupport.requireSize(decoded, Size.WORD, "DIVU");
        DispatchSupport.requireSource(decoded, "DIVU");
        DispatchSupport.requireDestination(decoded, "DIVU");
        DispatchSupport.requireNoExtension(decoded, "DIVU");

        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "DIVU");
        return Divu.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> cpu.registers().data(destinationRegister),
            value -> cpu.registers().setData(destinationRegister, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
