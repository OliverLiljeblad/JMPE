package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmpa;

import java.util.Objects;

public final class CmpaOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.CMPA, "CMPA");
        DispatchSupport.requireSource(decoded, "CMPA");
        DispatchSupport.requireDestination(decoded, "CMPA");
        DispatchSupport.requireNoExtension(decoded, "CMPA");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("CMPA must be decoded as WORD or LONG");
        }

        int destinationRegister = DispatchSupport.requireAddressRegister(decoded.dst(), "destination", "CMPA");
        return Cmpa.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            () -> cpu.registers().address(destinationRegister),
            cpu.statusRegister().moveConditionCodes()
        );
    }
}
