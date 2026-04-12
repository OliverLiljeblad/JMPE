package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Asr;

import java.util.Objects;

public final class AsrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ASR, "ASR");
        DispatchSupport.requireSized(decoded, "ASR");
        DispatchSupport.requireDestination(decoded, "ASR");
        DispatchSupport.requireNoExtension(decoded, "ASR");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Asr.execute(
            decoded.size(),
            DispatchSupport.shiftCount(cpu, bus, decoded),
            destination::read,
            destination::write,
            cpu.statusRegister().asrConditionCodes()
        );
    }
}
