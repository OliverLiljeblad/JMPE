package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.MoveQ;

import java.util.Objects;

public final class MoveQOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVEQ, "MOVEQ");
        DispatchSupport.requireSize(decoded, Size.LONG, "MOVEQ");
        EffectiveAddress.Immediate immediate = DispatchSupport.requireImmediateSource(decoded, "MOVEQ");
        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "MOVEQ");
        DispatchSupport.requireNoExtension(decoded, "MOVEQ");

        return MoveQ.execute(
            destinationRegister,
            immediate.value(),
            (register, value) -> cpu.registers().setData(register, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
