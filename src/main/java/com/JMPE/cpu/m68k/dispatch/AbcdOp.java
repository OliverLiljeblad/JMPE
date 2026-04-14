package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Abcd;

import java.util.Objects;

public final class AbcdOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);

        int sourceValue = DispatchSupport.readSource(decoded, cpu, bus);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Abcd.execute(
            decoded.size(),
            () -> sourceValue,
            destination::read,
            destination::write,
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().abcdConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.ABCD, "ABCD");
        DispatchSupport.requireSize(decoded, Size.BYTE, "ABCD");
        DispatchSupport.requireSource(decoded, "ABCD");
        DispatchSupport.requireDestination(decoded, "ABCD");
        DispatchSupport.requireNoExtension(decoded, "ABCD");

        boolean dataRegisterForm = decoded.src() instanceof EffectiveAddress.DataReg
            && decoded.dst() instanceof EffectiveAddress.DataReg;
        boolean predecrementForm = decoded.src() instanceof EffectiveAddress.AddrRegIndPreDec
            && decoded.dst() instanceof EffectiveAddress.AddrRegIndPreDec;
        if (!dataRegisterForm && !predecrementForm) {
            throw new IllegalArgumentException("ABCD requires matching data-register or predecrement operands");
        }
    }
}
