package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.exceptions.ExceptionDispatcher;
import com.JMPE.cpu.m68k.exceptions.ExceptionVector;

import java.util.Objects;

public final class TrapOp implements Op {
    public static final int EXECUTION_CYCLES = 34;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.TRAP, "TRAP");
        DispatchSupport.requireUnsized(decoded, "TRAP");
        DispatchSupport.requireNoSource(decoded, "TRAP");
        DispatchSupport.requireNoDestination(decoded, "TRAP");

        ExceptionDispatcher.dispatchSimpleVectorNumber(cpu, bus, ExceptionVector.trapVectorNumber(decoded.extension()));
        return EXECUTION_CYCLES;
    }
}
