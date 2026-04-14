package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class RteOp implements Op {
    public static final int EXECUTION_CYCLES = 20;

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.RTE, "RTE");
        DispatchSupport.requireUnsized(decoded, "RTE");
        DispatchSupport.requireNoSource(decoded, "RTE");
        DispatchSupport.requireNoDestination(decoded, "RTE");
        DispatchSupport.requireNoExtension(decoded, "RTE");
        DispatchSupport.requireSupervisor(cpu, "RTE");

        int supervisorStackPointer = cpu.registers().supervisorStackPointer();
        int restoredStatusRegister = bus.readWord(supervisorStackPointer);
        int restoredProgramCounter = bus.readLong(supervisorStackPointer + 2);
        cpu.registers().setSupervisorStackPointer(supervisorStackPointer + 6);
        cpu.statusRegister().setRawValue(restoredStatusRegister);
        cpu.registers().setProgramCounter(restoredProgramCounter);
        return EXECUTION_CYCLES;
    }
}
