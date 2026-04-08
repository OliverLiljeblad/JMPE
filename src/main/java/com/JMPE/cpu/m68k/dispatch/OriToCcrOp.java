package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class OriToCcrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ORI_TO_CCR, "ORI_TO_CCR");
        DispatchSupport.requireSize(decoded, Size.BYTE, "ORI_TO_CCR");
        EffectiveAddress.Immediate immediate = DispatchSupport.requireImmediateSource(decoded, "ORI_TO_CCR");
        DispatchSupport.requireNoExtension(decoded, "ORI_TO_CCR");
        DispatchSupport.requireCcrOperand(decoded.dst(), "destination", "ORI_TO_CCR");

        int result = Size.BYTE.mask(cpu.statusRegister().conditionCodeRegister() | immediate.value());
        cpu.statusRegister().setConditionCodeRegister(result);
        return EXECUTION_CYCLES;
    }
}
