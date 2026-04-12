package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

public final class MoveOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE, "MOVE");
        DispatchSupport.requireSized(decoded, "MOVE");
        DispatchSupport.requireSource(decoded, "MOVE");
        DispatchSupport.requireDestination(decoded, "MOVE");
        DispatchSupport.requireNoExtension(decoded, "MOVE");

        return Move.execute(
            decoded.size(),
            DispatchSupport.readSource(decoded, cpu, bus),
            value -> DispatchSupport.writeDestination(decoded, cpu, bus, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
