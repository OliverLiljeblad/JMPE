package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveFromUspOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_FROM_USP, "MOVE_FROM_USP");
        DispatchSupport.requireSize(decoded, Size.LONG, "MOVE_FROM_USP");
        DispatchSupport.requireNoSource(decoded, "MOVE_FROM_USP");
        DispatchSupport.requireDestination(decoded, "MOVE_FROM_USP");
        DispatchSupport.requireNoExtension(decoded, "MOVE_FROM_USP");
        DispatchSupport.requireSupervisor(cpu, "MOVE from USP");

        int destinationRegister = DispatchSupport.requireAddressRegister(decoded.dst(), "destination", "MOVE_FROM_USP");
        cpu.registers().setAddress(destinationRegister, cpu.registers().userStackPointer());
        return EXECUTION_CYCLES;
    }
}
