package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Sbcd;

import java.util.Objects;

public final class SbcdOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);

        int sourceValue = DispatchSupport.readSource(decoded, cpu, bus);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Sbcd.execute(
            decoded.size(),
            () -> sourceValue,
            destination::read,
            destination::write,
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().sbcdConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.SBCD, "SBCD");
        DispatchSupport.requireSize(decoded, Size.BYTE, "SBCD");
        DispatchSupport.requireSource(decoded, "SBCD");
        DispatchSupport.requireDestination(decoded, "SBCD");
        DispatchSupport.requireNoExtension(decoded, "SBCD");

        boolean dataRegisterForm = decoded.src() instanceof EffectiveAddress.DataReg
            && decoded.dst() instanceof EffectiveAddress.DataReg;
        boolean predecrementForm = decoded.src() instanceof EffectiveAddress.AddrRegIndPreDec
            && decoded.dst() instanceof EffectiveAddress.AddrRegIndPreDec;
        if (!dataRegisterForm && !predecrementForm) {
            throw new IllegalArgumentException("SBCD requires matching data-register or predecrement operands");
        }
    }
}
