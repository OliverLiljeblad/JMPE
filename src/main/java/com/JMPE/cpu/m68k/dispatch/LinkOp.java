package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Link;

import java.util.Objects;

public final class LinkOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.LINK, "LINK");
        DispatchSupport.requireUnsized(decoded, "LINK");
        DispatchSupport.requireSource(decoded, "LINK");
        DispatchSupport.requireNoDestination(decoded, "LINK");

        int register = DispatchSupport.requireAddressRegister(decoded.src(), "source", "LINK");
        int addressRegisterValue = cpu.registers().address(register);
        return Link.execute(
            addressRegisterValue,
            decoded.extension(),
            cpu.registers()::stackPointer,
            cpu.registers()::setStackPointer,
            bus::writeLong,
            value -> cpu.registers().setAddress(register, value)
        );
    }
}
