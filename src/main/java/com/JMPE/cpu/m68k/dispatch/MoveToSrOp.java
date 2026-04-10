package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveToSrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_TO_SR, "MOVE_TO_SR");
        DispatchSupport.requireSize(decoded, Size.WORD, "MOVE_TO_SR");
        DispatchSupport.requireSource(decoded, "MOVE_TO_SR");
        DispatchSupport.requireNoExtension(decoded, "MOVE_TO_SR");
        DispatchSupport.requireSrOperand(decoded.dst(), "destination", "MOVE_TO_SR");
        DispatchSupport.requireSupervisor(cpu, "MOVE to SR");

        cpu.statusRegister().setRawValue(Size.WORD.mask(DispatchSupport.readSource(decoded, cpu, bus)));
        return EXECUTION_CYCLES;
    }
}
