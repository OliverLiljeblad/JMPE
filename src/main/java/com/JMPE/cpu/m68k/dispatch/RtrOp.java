package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class RtrOp implements Op {
    public static final int EXECUTION_CYCLES = 20;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.RTR, "RTR");
        DispatchSupport.requireUnsized(decoded, "RTR");
        DispatchSupport.requireNoSource(decoded, "RTR");
        DispatchSupport.requireNoDestination(decoded, "RTR");
        DispatchSupport.requireNoExtension(decoded, "RTR");

        int stackPointer = cpu.registers().stackPointer();
        int restoredConditionCodes = bus.readWord(stackPointer);
        int restoredProgramCounter = bus.readLong(stackPointer + 2);
        cpu.registers().setStackPointer(stackPointer + 6);
        cpu.statusRegister().setConditionCodeRegister(restoredConditionCodes);
        cpu.registers().setProgramCounter(restoredProgramCounter);
        return EXECUTION_CYCLES;
    }
}
