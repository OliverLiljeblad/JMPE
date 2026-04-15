package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Nbcd;

import java.util.Objects;

public final class NbcdOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Nbcd.execute(
            decoded.size(),
            destination::read,
            destination::write,
            cpu.statusRegister().isExtendSet(),
            cpu.statusRegister().nbcdConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.NBCD, "NBCD");
        DispatchSupport.requireSize(decoded, Size.BYTE, "NBCD");
        DispatchSupport.requireNoSource(decoded, "NBCD");
        DispatchSupport.requireDestination(decoded, "NBCD");
        DispatchSupport.requireNoExtension(decoded, "NBCD");

        if (!isDataAlterable(decoded.dst())) {
            throw new IllegalArgumentException("NBCD requires a data-alterable destination but was " + decoded.dst());
        }
    }

    private static boolean isDataAlterable(EffectiveAddress operand) {
        return switch (operand) {
            case EffectiveAddress.DataReg ignored -> true;
            case EffectiveAddress.AddrRegInd ignored -> true;
            case EffectiveAddress.AddrRegIndPostInc ignored -> true;
            case EffectiveAddress.AddrRegIndPreDec ignored -> true;
            case EffectiveAddress.AddrRegIndDisp ignored -> true;
            case EffectiveAddress.AddrRegIndIndex ignored -> true;
            case EffectiveAddress.AbsoluteShort ignored -> true;
            case EffectiveAddress.AbsoluteLong ignored -> true;
            default -> false;
        };
    }
}
