package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Move;

import java.util.Objects;

public final class MoveFromSrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_FROM_SR, "MOVE_FROM_SR");
        DispatchSupport.requireSize(decoded, Size.WORD, "MOVE_FROM_SR");
        DispatchSupport.requireDestination(decoded, "MOVE_FROM_SR");
        DispatchSupport.requireNoExtension(decoded, "MOVE_FROM_SR");
        DispatchSupport.requireSrOperand(decoded.src(), "source", "MOVE_FROM_SR");

        return Move.execute(
            decoded.size(),
            cpu.statusRegister().rawValue(),
            value -> DispatchSupport.writeDestination(decoded, cpu, bus, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
