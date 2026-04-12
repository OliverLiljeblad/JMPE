package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Jmp;

import java.util.Objects;

public final class JmpOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.JMP, "JMP");
        DispatchSupport.requireUnsized(decoded, "JMP");
        DispatchSupport.requireSource(decoded, "JMP");
        DispatchSupport.requireNoDestination(decoded, "JMP");
        DispatchSupport.requireNoExtension(decoded, "JMP");

        return Jmp.execute(DispatchSupport.computeAddress(decoded.src(), cpu), cpu.registers()::setProgramCounter);
    }
}
