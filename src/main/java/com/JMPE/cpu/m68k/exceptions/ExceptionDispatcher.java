package com.JMPE.cpu.m68k.exceptions;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;

import java.util.Objects;

public final class ExceptionDispatcher {
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
}
