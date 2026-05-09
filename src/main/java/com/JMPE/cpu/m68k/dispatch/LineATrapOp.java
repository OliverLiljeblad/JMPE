package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.ExceptionDispatcher;
import com.JMPE.cpu.m68k.exceptions.ExceptionVector;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class LineATrapOp implements Op {
    public static final int EXECUTION_CYCLES = 34;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.LINE_A_TRAP, "LINE_A_TRAP");
        DispatchSupport.requireUnsized(decoded, "LINE_A_TRAP");
        DispatchSupport.requireNoSource(decoded, "LINE_A_TRAP");
        DispatchSupport.requireNoDestination(decoded, "LINE_A_TRAP");

        // Per M68000 PRM §6.3.6, the saved PC for unimplemented-instruction
        // exceptions (Line A / Line F) must be the address of the trapping
        // opword itself, not the next instruction. M68kCpu.step() advances PC
        // past the opword before dispatch, so rewind by 2 here.
        cpu.registers().setProgramCounter(cpu.registers().programCounter() - 2);
        ExceptionDispatcher.dispatchSimpleVector(cpu, bus, ExceptionVector.LINE_A_TRAP);
        return EXECUTION_CYCLES;
    }
}
