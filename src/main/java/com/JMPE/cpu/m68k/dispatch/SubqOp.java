package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subq;

import java.util.Objects;

public final class SubqOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.SUBQ, "SUBQ");
        DispatchSupport.requireSized(decoded, "SUBQ");
        DispatchSupport.requireDestination(decoded, "SUBQ");
        DispatchSupport.requireNoExtension(decoded, "SUBQ");

        int quickValue = DispatchSupport.quickValue(decoded, "SUBQ");
        if (decoded.dst() instanceof EffectiveAddress.AddrReg addrReg) {
            if (decoded.size() == Size.BYTE) {
                throw new IllegalArgumentException("SUBQ to address register must not be decoded as BYTE");
            }
            cpu.registers().setAddress(addrReg.reg(), cpu.registers().address(addrReg.reg()) - quickValue);
            return Subq.EXECUTION_CYCLES;
        }

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Subq.execute(
            decoded.size(),
            quickValue,
            destination::read,
            destination::write,
            cpu.statusRegister().subqConditionCodes()
        );
    }
}
