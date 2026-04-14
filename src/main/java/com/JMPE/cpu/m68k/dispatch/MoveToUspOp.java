package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveToUspOp implements Op {
    private static final int EXECUTION_CYCLES = 4;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_TO_USP, "MOVE_TO_USP");
        DispatchSupport.requireSize(decoded, Size.LONG, "MOVE_TO_USP");
        DispatchSupport.requireSource(decoded, "MOVE_TO_USP");
        DispatchSupport.requireNoDestination(decoded, "MOVE_TO_USP");
        DispatchSupport.requireNoExtension(decoded, "MOVE_TO_USP");
        DispatchSupport.requireSupervisor(cpu, "MOVE to USP");

        int sourceRegister = DispatchSupport.requireAddressRegister(decoded.src(), "source", "MOVE_TO_USP");
        cpu.registers().setUserStackPointer(cpu.registers().address(sourceRegister));
        return EXECUTION_CYCLES;
    }
}
