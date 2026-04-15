package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.ExceptionFrameKind;
import com.JMPE.cpu.m68k.exceptions.ExceptionVector;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class SystemTrapOpTest {
    @Test
    void trapOpVectorsThroughImmediateTrapNumber() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        bus.writeLong(ExceptionVector.trapVectorNumber(3) * 4, 0x0000_1234);
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.registers().setProgramCounter(0x0000_1002);
        cpu.statusRegister().setRawValue(0x8005);

        int cycles = new TrapOp().execute(cpu, bus, decoded(Opcode.TRAP, 3));

        assertAll(
            () -> assertEquals(TrapOp.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0000_1FFA, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x2005, cpu.statusRegister().rawValue()),
            () -> assertEquals(0x8005, bus.readWord(0x0000_1FFA)),
            () -> assertEquals(0x0000_1002, bus.readLong(0x0000_1FFC))
        );
    }

    @Test
    void trapvOpOnlyVectorsWhenOverflowIsSet() {
        AddressSpace bus = flatRamBus();
        bus.writeLong(ExceptionVector.TRAPV.vectorAddress(), 0x0000_1234);

        M68kCpu takenCpu = new M68kCpu();
        takenCpu.registers().setUserStackPointer(0x0000_3000);
        takenCpu.registers().setSupervisorStackPointer(0x0000_2000);
        takenCpu.registers().setProgramCounter(0x0000_1002);
        takenCpu.statusRegister().setOverflow(true);

        int takenCycles = new TrapvOp().execute(takenCpu, bus, decoded(Opcode.TRAPV, 0));

        M68kCpu notTakenCpu = new M68kCpu();
        notTakenCpu.registers().setProgramCounter(0x0000_1002);

        int notTakenCycles = new TrapvOp().execute(notTakenCpu, bus, decoded(Opcode.TRAPV, 0));

        assertAll(
            () -> assertEquals(TrapvOp.EXECUTION_CYCLES_TAKEN, takenCycles),
            () -> assertEquals(0x0000_1234, takenCpu.registers().programCounter()),
            () -> assertEquals(0x0000_1FFA, takenCpu.registers().supervisorStackPointer()),
            () -> assertEquals(TrapvOp.EXECUTION_CYCLES_NOT_TAKEN, notTakenCycles),
            () -> assertEquals(0x0000_1002, notTakenCpu.registers().programCounter())
        );
    }

    @Test
    void lineTrapOpsUseFixedVectors() {
        AddressSpace bus = flatRamBus();
        bus.writeLong(ExceptionVector.LINE_A_TRAP.vectorAddress(), 0x0000_1234);
        bus.writeLong(ExceptionVector.LINE_F_TRAP.vectorAddress(), 0x0000_2345);

        M68kCpu lineACpu = new M68kCpu();
        lineACpu.registers().setUserStackPointer(0x0000_3000);
        lineACpu.registers().setSupervisorStackPointer(0x0000_2000);
        lineACpu.registers().setProgramCounter(0x0000_1002);

        int lineACycles = new LineATrapOp().execute(lineACpu, bus, decoded(Opcode.LINE_A_TRAP, 0xA123));

        M68kCpu lineFCpu = new M68kCpu();
        lineFCpu.registers().setUserStackPointer(0x0000_3000);
        lineFCpu.registers().setSupervisorStackPointer(0x0000_2000);
        lineFCpu.registers().setProgramCounter(0x0000_1002);

        int lineFCycles = new LineFTrapOp().execute(lineFCpu, bus, decoded(Opcode.LINE_F_TRAP, 0xF234));

        assertAll(
            () -> assertEquals(LineATrapOp.EXECUTION_CYCLES, lineACycles),
            () -> assertEquals(0x0000_1234, lineACpu.registers().programCounter()),
            () -> assertEquals(LineFTrapOp.EXECUTION_CYCLES, lineFCycles),
            () -> assertEquals(0x0000_2345, lineFCpu.registers().programCounter())
        );
    }

    @Test
    void rteOpRestoresFullStatusRegisterAndProgramCounterFromSupervisorStack() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.statusRegister().setRawValue(0x2700);
        bus.writeWord(0x0000_2000, 0x0015);
        bus.writeLong(0x0000_2002, 0x0000_1234);

        int cycles = new RteOp().execute(cpu, bus, decoded(Opcode.RTE, 0));

        assertAll(
            () -> assertEquals(RteOp.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0015, cpu.statusRegister().rawValue()),
            () -> assertEquals(0x0000_2006, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x0000_3000, cpu.registers().stackPointer())
        );
    }

    @Test
    void rteOpRestoresGroup0ExceptionFrameAndReturnsToUserStack() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        cpu.recordExceptionFrame(ExceptionFrameKind.GROUP_0);
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.statusRegister().setRawValue(0x2700);
        bus.writeWord(0x0000_2000, 0x0019);
        bus.writeLong(0x0000_2002, 0x0000_1235);
        bus.writeWord(0x0000_2006, 0x4A50);
        bus.writeWord(0x0000_2008, 0x0015);
        bus.writeLong(0x0000_200A, 0x0000_1234);

        int cycles = new RteOp().execute(cpu, bus, decoded(Opcode.RTE, 0));

        assertAll(
            () -> assertEquals(RteOp.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x0015, cpu.statusRegister().rawValue()),
            () -> assertEquals(0x0000_200E, cpu.registers().supervisorStackPointer()),
            () -> assertEquals(0x0000_3000, cpu.registers().stackPointer())
        );
    }

    @Test
    void rtrOpRestoresConditionCodesAndProgramCounterWithoutChangingSupervisorState() {
        AddressSpace bus = flatRamBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.statusRegister().setRawValue(0x2700);
        bus.writeWord(0x0000_2000, 0x0015);
        bus.writeLong(0x0000_2002, 0x0000_1234);

        int cycles = new RtrOp().execute(cpu, bus, decoded(Opcode.RTR, 0));

        assertAll(
            () -> assertEquals(RtrOp.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_1234, cpu.registers().programCounter()),
            () -> assertEquals(0x2715, cpu.statusRegister().rawValue()),
            () -> assertTrue(cpu.statusRegister().isSupervisorSet()),
            () -> assertEquals(0x0000_2006, cpu.registers().supervisorStackPointer())
        );
    }

    @Test
    void stopOpLoadsStatusRegisterAndSetsStoppedState() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setUserStackPointer(0x0000_3000);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.statusRegister().setRawValue(0x2700);

        int cycles = new StopOp().execute(cpu, null, decoded(Opcode.STOP, 0x0015));

        assertAll(
            () -> assertEquals(StopOp.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0015, cpu.statusRegister().rawValue()),
            () -> assertFalse(cpu.statusRegister().isSupervisorSet()),
            () -> assertTrue(cpu.isStopped()),
            () -> assertEquals(0x0000_3000, cpu.registers().stackPointer())
        );
    }

    @Test
    void stopOpRejectsUserModeExecution() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(false);

        assertThrows(PrivilegeViolation.class, () -> new StopOp().execute(cpu, null, decoded(Opcode.STOP, 0x0015)));
        assertFalse(cpu.isStopped());
    }

    private static DecodedInstruction decoded(Opcode opcode, int extension) {
        return new DecodedInstruction(opcode, Size.UNSIZED, EffectiveAddress.none(), EffectiveAddress.none(), extension, 0);
    }

    private static AddressSpace flatRamBus() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x0000_0000, 0x4000));
        return bus;
    }
}
