package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Muls;

import java.util.Objects;

public final class MulsOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MULS, "MULS");
        DispatchSupport.requireSize(decoded, Size.WORD, "MULS");
        DispatchSupport.requireSource(decoded, "MULS");
        DispatchSupport.requireDestination(decoded, "MULS");
        DispatchSupport.requireNoExtension(decoded, "MULS");

        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "MULS");
        return Muls.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> cpu.registers().data(destinationRegister),
            value -> cpu.registers().setData(destinationRegister, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
