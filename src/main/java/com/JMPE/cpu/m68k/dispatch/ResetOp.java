package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Reset;

import java.util.Objects;

public final class ResetOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.RESET, "RESET");
        DispatchSupport.requireUnsized(decoded, "RESET");
        DispatchSupport.requireNoSource(decoded, "RESET");
        DispatchSupport.requireNoDestination(decoded, "RESET");
        DispatchSupport.requireNoExtension(decoded, "RESET");
        DispatchSupport.requireSupervisor(cpu, "RESET");

        return Reset.execute();
    }
}
