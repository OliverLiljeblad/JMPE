package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.exceptions.ExceptionDispatcher;
import com.JMPE.cpu.m68k.exceptions.ExceptionVector;

import java.util.Objects;

public final class TrapvOp implements Op {
    public static final int EXECUTION_CYCLES_TAKEN = 34;
    public static final int EXECUTION_CYCLES_NOT_TAKEN = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.TRAPV, "TRAPV");
        DispatchSupport.requireUnsized(decoded, "TRAPV");
        DispatchSupport.requireNoSource(decoded, "TRAPV");
        DispatchSupport.requireNoDestination(decoded, "TRAPV");
        DispatchSupport.requireNoExtension(decoded, "TRAPV");

        if (!cpu.statusRegister().isOverflowSet()) {
            return EXECUTION_CYCLES_NOT_TAKEN;
        }

        ExceptionDispatcher.dispatchSimpleVector(cpu, bus, ExceptionVector.TRAPV);
        return EXECUTION_CYCLES_TAKEN;
    }
}
