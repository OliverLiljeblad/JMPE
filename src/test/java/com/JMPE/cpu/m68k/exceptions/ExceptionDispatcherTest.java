package com.JMPE.cpu.m68k.exceptions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.M68kCpu;
import org.junit.jupiter.api.Test;

class ExceptionDispatcherTest {
    @Test
    void dispatchSimpleVectorPushesSimpleFrameOnSupervisorStackAndLoadsHandler() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        bus.writeLong(ExceptionVector.LINE_A_TRAP.vectorAddress(), 0x0000_1234);
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.registers().setProgramCounter(0x0000_1002);
        cpu.statusRegister().setRawValue(0x8005);

        ExceptionDispatcher.dispatchSimpleVector(cpu, bus, ExceptionVector.LINE_A_TRAP);

        assertAll(
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0000_3000, cpu.registers().userStackPointer()),
            () -> assertEquals(0x0000_1FFA, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x2005, cpu.statusRegister().rawValue()),
            () -> assertFalse(cpu.statusRegister().isTraceSet()),
            () -> assertTrue(cpu.statusRegister().isSupervisorSet()),
            () -> assertEquals(0x8005, bus.readWord(0x0000_1FFA)),
            () -> assertEquals(0x0000_1002, bus.readLong(0x0000_1FFC))
        );
    }

    @Test
    void dispatchSimpleVectorRejectsGroupZeroVectors() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> ExceptionDispatcher.dispatchSimpleVector(cpu, bus, ExceptionVector.BUS_ERROR)
        );

        assertEquals("BUS_ERROR does not use the simple six-byte exception frame", thrown.getMessage());
    }

    @Test
    void dispatchGroup0FaultPushesExtendedFrameOnSupervisorStackAndLoadsHandler() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        bus.writeLong(ExceptionVector.ADDRESS_ERROR.vectorAddress(), 0x0000_1234);
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.registers().setProgramCounter(0x0000_1002);
        cpu.statusRegister().setRawValue(0x8005);

        ExceptionDispatcher.dispatchGroup0Fault(
            cpu,
            bus,
            new AddressErrorException(0x0000_1235, FaultAccessType.READ),
            0x0000_1002,
            0x4A50,
            false,
            false
        );

        assertAll(
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0000_3000, cpu.registers().userStackPointer()),
            () -> assertEquals(0x0000_1FF2, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x2005, cpu.statusRegister().rawValue()),
            () -> assertFalse(cpu.statusRegister().isTraceSet()),
            () -> assertTrue(cpu.statusRegister().isSupervisorSet()),
            () -> assertEquals(0x0019, bus.readWord(0x0000_1FF2)),
            () -> assertEquals(0x0000_1235, bus.readLong(0x0000_1FF4)),
            () -> assertEquals(0x4A50, bus.readWord(0x0000_1FF8)),
            () -> assertEquals(0x8005, bus.readWord(0x0000_1FFA)),
            () -> assertEquals(0x0000_1002, bus.readLong(0x0000_1FFC))
        );
    }

    @Test
    void dispatchInterruptAutovectorPushesSimpleFrameRaisesMaskAndClearsStoppedState() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        bus.writeLong(ExceptionVector.interruptAutovectorNumber(3) * 4, 0x0000_1234);
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.registers().setProgramCounter(0x0000_1000);
        cpu.statusRegister().setRawValue(0x0015);
        cpu.stop();

        ExceptionDispatcher.dispatchInterruptAutovector(cpu, bus, 3);

        assertAll(
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0000_3000, cpu.registers().userStackPointer()),
            () -> assertEquals(0x0000_1FFA, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x2315, cpu.statusRegister().rawValue()),
            () -> assertFalse(cpu.statusRegister().isTraceSet()),
            () -> assertTrue(cpu.statusRegister().isSupervisorSet()),
            () -> assertFalse(cpu.isStopped()),
            () -> assertEquals(0x0015, bus.readWord(0x0000_1FFA)),
            () -> assertEquals(0x0000_1000, bus.readLong(0x0000_1FFC))
        );
    }

    @Test
    void dispatchIfSupportedLeavesGroupZeroFaultsUnhandledForNow() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();

        assertFalse(ExceptionDispatcher.dispatchIfSupported(cpu, bus, new BusErrorException(0x0000_1234)));
        assertFalse(ExceptionDispatcher.dispatchIfSupported(cpu, bus, new AddressErrorException(0x0000_1235)));
    }

    private static AddressSpace flatRamBus() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x0000_0000, 0x4000));
        return bus;
    }
}
