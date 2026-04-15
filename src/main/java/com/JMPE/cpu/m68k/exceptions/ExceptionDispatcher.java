package com.JMPE.cpu.m68k.exceptions;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;

import java.util.Objects;

public final class ExceptionDispatcher {
    private static final int GROUP0_INSTRUCTION_ACCESS = 0;
    private static final int GROUP0_DATA_ACCESS = 0x08;
    private static final int GROUP0_READ = 0x10;
    private static final int GROUP0_WRITE = 0;
    private static final int USER_DATA_FUNCTION_CODE = 0x01;
    private static final int USER_PROGRAM_FUNCTION_CODE = 0x02;
    private static final int SUPERVISOR_DATA_FUNCTION_CODE = 0x05;
    private static final int SUPERVISOR_PROGRAM_FUNCTION_CODE = 0x06;

    private ExceptionDispatcher() {
    }

    public static void dispatchSimpleVector(M68kCpu cpu, Bus bus, ExceptionVector vector) {
        Objects.requireNonNull(vector, "vector must not be null");
        if (!vector.usesSimpleFrame()) {
            throw new IllegalArgumentException(vector + " does not use the simple six-byte exception frame");
        }
        dispatchSimpleVectorNumber(cpu, bus, vector.vectorNumber());
    }

    /**
     * Dispatches a simple six-byte exception frame for dynamic vectors such as TRAP #n.
     */
    public static void dispatchSimpleVectorNumber(M68kCpu cpu, Bus bus, int vectorNumber) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (vectorNumber < 0) {
            throw new IllegalArgumentException("vectorNumber must not be negative");
        }

        dispatchSimpleFrame(cpu, bus, vectorNumber);
    }

    public static boolean dispatchIfSupported(M68kCpu cpu, Bus bus, Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");

        if (exception instanceof SimpleVectoredException vectoredException) {
            dispatchSimpleVector(cpu, bus, vectoredException.exceptionVector());
            return true;
        }
        return false;
    }

    public static void dispatchGroup0Fault(M68kCpu cpu,
                                           Bus bus,
                                           Group0Fault fault,
                                           int savedProgramCounter,
                                           int instructionRegister,
                                           boolean instructionAccess,
                                           boolean supervisorAccess) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(fault, "fault must not be null");
        if (fault.exceptionVector().frameKind() != ExceptionFrameKind.GROUP_0) {
            throw new IllegalArgumentException(fault.exceptionVector() + " does not use the group-0 exception frame");
        }

        int savedStatusRegister = cpu.statusRegister().rawValue();

        cpu.clearStopped();
        cpu.statusRegister().setSupervisor(true);
        cpu.statusRegister().setTrace(false);

        int stackPointer = cpu.registers().stackPointer();
        stackPointer -= Integer.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeLong(stackPointer, savedProgramCounter);

        stackPointer -= Short.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeWord(stackPointer, savedStatusRegister);

        stackPointer -= Short.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeWord(stackPointer, instructionRegister);

        stackPointer -= Integer.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeLong(stackPointer, fault.faultAddress());

        stackPointer -= Short.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeWord(
            stackPointer,
            buildGroup0StatusWord(fault.accessType(), instructionAccess, supervisorAccess)
        );

        cpu.registers().setProgramCounter(bus.readLong(fault.exceptionVector().vectorAddress()));
    }

    public static void dispatchInterruptAutovector(M68kCpu cpu, Bus bus, int interruptLevel) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (interruptLevel < 1 || interruptLevel > 7) {
            throw new IllegalArgumentException("interrupt level must be in range 1..7");
        }

        int savedProgramCounter = cpu.registers().programCounter();
        int savedStatusRegister = cpu.statusRegister().rawValue();

        cpu.clearStopped();
        cpu.statusRegister().setSupervisor(true);
        cpu.statusRegister().setTrace(false);
        cpu.statusRegister().setInterruptMask(interruptLevel);

        int stackPointer = cpu.registers().stackPointer();
        stackPointer -= Integer.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeLong(stackPointer, savedProgramCounter);

        stackPointer -= Short.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeWord(stackPointer, savedStatusRegister);

        cpu.registers().setProgramCounter(bus.readLong(ExceptionVector.interruptAutovectorNumber(interruptLevel) * 4));
    }

    private static void dispatchSimpleFrame(M68kCpu cpu, Bus bus, int vectorNumber) {
        int savedProgramCounter = cpu.registers().programCounter();
        int savedStatusRegister = cpu.statusRegister().rawValue();

        cpu.clearStopped();
        cpu.statusRegister().setSupervisor(true);
        cpu.statusRegister().setTrace(false);

        int stackPointer = cpu.registers().stackPointer();
        stackPointer -= Integer.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeLong(stackPointer, savedProgramCounter);

        stackPointer -= Short.BYTES;
        cpu.registers().setStackPointer(stackPointer);
        bus.writeWord(stackPointer, savedStatusRegister);

        cpu.registers().setProgramCounter(bus.readLong(vectorNumber * 4));
    }

    private static int buildGroup0StatusWord(FaultAccessType accessType,
                                             boolean instructionAccess,
                                             boolean supervisorAccess) {
        int readWrite = accessType == FaultAccessType.READ ? GROUP0_READ : GROUP0_WRITE;
        int instructionNot = instructionAccess ? GROUP0_INSTRUCTION_ACCESS : GROUP0_DATA_ACCESS;
        int functionCode;
        if (supervisorAccess) {
            functionCode = instructionAccess ? SUPERVISOR_PROGRAM_FUNCTION_CODE : SUPERVISOR_DATA_FUNCTION_CODE;
        } else {
            functionCode = instructionAccess ? USER_PROGRAM_FUNCTION_CODE : USER_DATA_FUNCTION_CODE;
        }
        return readWrite | instructionNot | functionCode;
    }
}
