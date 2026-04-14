package com.JMPE.cpu.m68k.exceptions;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;

import java.util.Objects;

public final class ExceptionDispatcher {
    private ExceptionDispatcher() {
    }

    public static void dispatchSimpleVector(M68kCpu cpu, Bus bus, ExceptionVector vector) {
        Objects.requireNonNull(vector, "vector must not be null");
        dispatchSimpleVector(cpu, bus, vector.vectorNumber());
    }

    public static void dispatchSimpleVector(M68kCpu cpu, Bus bus, int vectorNumber) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (vectorNumber < 0) {
            throw new IllegalArgumentException("vectorNumber must not be negative");
        }

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

    public static boolean dispatchIfSupported(M68kCpu cpu, Bus bus, Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");

        if (exception instanceof IllegalInstructionException) {
            dispatchSimpleVector(cpu, bus, ExceptionVector.ILLEGAL_INSTRUCTION);
            return true;
        }
        if (exception instanceof PrivilegeViolation privilegeViolation) {
            dispatchSimpleVector(cpu, bus, privilegeViolation.vector());
            return true;
        }
        if (exception instanceof DivideByZeroException divideByZeroException) {
            dispatchSimpleVector(cpu, bus, divideByZeroException.vector());
            return true;
        }
        if (exception instanceof ChkException chkException) {
            dispatchSimpleVector(cpu, bus, chkException.vector());
            return true;
        }
        return false;
    }
}
