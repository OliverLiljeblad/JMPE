package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addq;

import java.util.Objects;

public final class AddqOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ADDQ, "ADDQ");
        DispatchSupport.requireSized(decoded, "ADDQ");
        DispatchSupport.requireDestination(decoded, "ADDQ");
        DispatchSupport.requireNoExtension(decoded, "ADDQ");

        int quickValue = DispatchSupport.quickValue(decoded, "ADDQ");
        if (decoded.dst() instanceof EffectiveAddress.AddrReg addrReg) {
            if (decoded.size() == Size.BYTE) {
                throw new IllegalArgumentException("ADDQ to address register must not be decoded as BYTE");
            }
            cpu.registers().setAddress(addrReg.reg(), cpu.registers().address(addrReg.reg()) + quickValue);
            return Addq.EXECUTION_CYCLES;
        }

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);
        return Addq.execute(
            decoded.size(),
            quickValue,
            destination::read,
            destination::write,
            cpu.statusRegister().addqConditionCodes()
        );
    }
}
