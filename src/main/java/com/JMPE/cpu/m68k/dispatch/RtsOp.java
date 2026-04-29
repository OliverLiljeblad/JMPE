package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Rts;

import java.util.Objects;

public final class RtsOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.RTS, "RTS");
        DispatchSupport.requireUnsized(decoded, "RTS");
        DispatchSupport.requireNoSource(decoded, "RTS");
        DispatchSupport.requireNoDestination(decoded, "RTS");
        DispatchSupport.requireNoExtension(decoded, "RTS");

        return Rts.execute(
            () -> DispatchSupport.popLong(cpu, bus),
            DispatchSupport.controlTransferPcWriter(cpu)
        );
    }
}
