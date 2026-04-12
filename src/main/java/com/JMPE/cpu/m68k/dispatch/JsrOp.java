package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Jsr;

import java.util.Objects;

public final class JsrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.JSR, "JSR");
        DispatchSupport.requireUnsized(decoded, "JSR");
        DispatchSupport.requireSource(decoded, "JSR");
        DispatchSupport.requireNoDestination(decoded, "JSR");
        DispatchSupport.requireNoExtension(decoded, "JSR");

        int returnAddress = cpu.registers().programCounter();
        int targetAddress = DispatchSupport.computeAddress(decoded.src(), cpu);
        return Jsr.execute(
            returnAddress,
            targetAddress,
            value -> DispatchSupport.pushLong(cpu, bus, value),
            cpu.registers()::setProgramCounter
        );
    }
}
