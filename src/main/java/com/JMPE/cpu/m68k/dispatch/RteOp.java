package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.ExceptionFrameKind;
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
        ExceptionFrameKind frameKind = cpu.consumeExceptionFrameOrDefault(ExceptionFrameKind.SIX_BYTE_SIMPLE);
        int restoredStatusRegister;
        int restoredProgramCounter;
        int restoredSupervisorStackPointer;

        if (frameKind == ExceptionFrameKind.GROUP_0) {
            restoredStatusRegister = bus.readWord(supervisorStackPointer + 8);
            restoredProgramCounter = bus.readLong(supervisorStackPointer + 10);
            restoredSupervisorStackPointer = supervisorStackPointer + 14;
        } else {
            restoredStatusRegister = bus.readWord(supervisorStackPointer);
            restoredProgramCounter = bus.readLong(supervisorStackPointer + 2);
            restoredSupervisorStackPointer = supervisorStackPointer + 6;
        }

        cpu.registers().setSupervisorStackPointer(restoredSupervisorStackPointer);
        cpu.statusRegister().setRawValue(restoredStatusRegister);
        cpu.registers().setProgramCounter(restoredProgramCounter);
        return EXECUTION_CYCLES;
    }
}
