package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Mulu;

import java.util.Objects;

public final class MuluOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MULU, "MULU");
        DispatchSupport.requireSize(decoded, Size.WORD, "MULU");
        DispatchSupport.requireSource(decoded, "MULU");
        DispatchSupport.requireDestination(decoded, "MULU");
        DispatchSupport.requireNoExtension(decoded, "MULU");

        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "MULU");
        return Mulu.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> cpu.registers().data(destinationRegister),
            value -> cpu.registers().setData(destinationRegister, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
