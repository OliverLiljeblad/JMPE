package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveToCcrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_TO_CCR, "MOVE_TO_CCR");
        DispatchSupport.requireSize(decoded, Size.WORD, "MOVE_TO_CCR");
        DispatchSupport.requireSource(decoded, "MOVE_TO_CCR");
        DispatchSupport.requireNoExtension(decoded, "MOVE_TO_CCR");
        DispatchSupport.requireCcrOperand(decoded.dst(), "destination", "MOVE_TO_CCR");

        int sourceValue = DispatchSupport.readSource(decoded, cpu, bus);
        cpu.statusRegister().setConditionCodeRegister(Size.BYTE.mask(sourceValue));
        return EXECUTION_CYCLES;
    }
}
