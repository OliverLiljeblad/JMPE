package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Dbcc;
import com.JMPE.cpu.m68k.instructions.control.Scc;

import java.util.Objects;

public final class SccOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.Scc, "SCC");
        DispatchSupport.requireSize(decoded, Size.BYTE, "SCC");
        DispatchSupport.requireNoSource(decoded, "SCC");
        DispatchSupport.requireDestination(decoded, "SCC");

        return Scc.execute(
            Dbcc.isConditionTrue(decoded.extension(), DispatchSupport.conditionCodesReader(cpu)),
            value -> DispatchSupport.writeDestination(decoded, cpu, bus, value)
        );
    }
}
