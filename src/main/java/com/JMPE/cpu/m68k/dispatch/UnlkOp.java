package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Unlk;

import java.util.Objects;

public final class UnlkOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.UNLK, "UNLK");
        DispatchSupport.requireUnsized(decoded, "UNLK");
        DispatchSupport.requireSource(decoded, "UNLK");
        DispatchSupport.requireNoDestination(decoded, "UNLK");
        DispatchSupport.requireNoExtension(decoded, "UNLK");

        int register = DispatchSupport.requireAddressRegister(decoded.src(), "source", "UNLK");
        int addressRegisterValue = cpu.registers().address(register);
        return Unlk.execute(
            addressRegisterValue,
            cpu.registers()::setStackPointer,
            bus::readLong,
            value -> cpu.registers().setAddress(register, value)
        );
    }
}
