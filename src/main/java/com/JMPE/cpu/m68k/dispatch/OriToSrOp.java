package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class OriToSrOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ORI_TO_SR, "ORI_TO_SR");
        DispatchSupport.requireSize(decoded, Size.WORD, "ORI_TO_SR");
        EffectiveAddress.Immediate immediate = DispatchSupport.requireImmediateSource(decoded, "ORI_TO_SR");
        DispatchSupport.requireNoExtension(decoded, "ORI_TO_SR");
        DispatchSupport.requireSrOperand(decoded.dst(), "destination", "ORI_TO_SR");
        DispatchSupport.requireSupervisor(cpu, "ORI to SR");

        int result = Size.WORD.mask(cpu.statusRegister().rawValue() | immediate.value());
        cpu.statusRegister().setRawValue(result);
        return EXECUTION_CYCLES;
    }
}
