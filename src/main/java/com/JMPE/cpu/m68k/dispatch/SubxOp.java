package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subx;

import java.util.Objects;

public final class SubxOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);

        // Memory-form SUBX is source predecrement followed by destination predecrement.
        int sourceValue = DispatchSupport.readSource(decoded, cpu, bus);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Subx.execute(
            decoded.size(),
            () -> sourceValue,
            destination::read,
            destination::write,
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().isZeroSet(),
            cpu.statusRegister().subConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.SUBX, "SUBX");
        DispatchSupport.requireSized(decoded, "SUBX");
        DispatchSupport.requireSource(decoded, "SUBX");
        DispatchSupport.requireDestination(decoded, "SUBX");
        DispatchSupport.requireNoExtension(decoded, "SUBX");

        boolean dataRegisterForm = decoded.src() instanceof EffectiveAddress.DataReg
            && decoded.dst() instanceof EffectiveAddress.DataReg;
        boolean predecrementForm = decoded.src() instanceof EffectiveAddress.AddrRegIndPreDec
            && decoded.dst() instanceof EffectiveAddress.AddrRegIndPreDec;
        if (!dataRegisterForm && !predecrementForm) {
            throw new IllegalArgumentException("SUBX requires matching data-register or predecrement operands");
        }
    }
}
