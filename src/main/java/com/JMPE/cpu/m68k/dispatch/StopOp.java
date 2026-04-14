package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class StopOp implements Op {
    public static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.STOP, "STOP");
        DispatchSupport.requireUnsized(decoded, "STOP");
        DispatchSupport.requireNoSource(decoded, "STOP");
        DispatchSupport.requireNoDestination(decoded, "STOP");
        DispatchSupport.requireSupervisor(cpu, "STOP");

        cpu.statusRegister().setRawValue(decoded.extension());
        cpu.stop();
        return EXECUTION_CYCLES;
    }
}
