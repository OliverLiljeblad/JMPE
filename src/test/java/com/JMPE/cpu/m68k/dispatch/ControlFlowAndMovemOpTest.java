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
import com.JMPE.cpu.m68k.exceptions.ChkException;
import com.JMPE.cpu.m68k.exceptions.ProgramCounterAddressErrorException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Link;
import com.JMPE.cpu.m68k.instructions.control.Unlk;
import org.junit.jupiter.api.Test;

class ControlFlowAndMovemOpTest {
    @Test
    void leaOpComputesEffectiveAddress() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(1, 0x1000);

        int cycles = new LeaOp().execute(cpu, null, decoded(Opcode.LEA, Size.LONG,
            EffectiveAddress.addrRegIndDisp(1, 0x20), EffectiveAddress.addrReg(0), 0));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x1020, cpu.registers().address(0))
        );
    }

    @Test
    void linkOpPushesOldFrameRegisterAndAllocatesLocalStackSpace() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setStackPointer(0x1100);
        cpu.registers().setAddress(6, 0x2222_0000);

        int cycles = new LinkOp().execute(cpu, bus, decoded(Opcode.LINK, Size.UNSIZED,
            EffectiveAddress.addrReg(6), EffectiveAddress.none(), -8));

        assertAll(
            () -> assertEquals(Link.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x10F4, cpu.registers().stackPointer()),
            () -> assertEquals(0x10FC, cpu.registers().address(6)),
            () -> assertEquals(0x2222_0000, bus.readLong(0x10FC))
        );
    }

    @Test
    void unlkOpRestoresFrameRegisterAndReleasesStackFrame() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setStackPointer(0x10F4);
        cpu.registers().setAddress(6, 0x10FC);
        bus.writeLong(0x10FC, 0x2222_0000);

        int cycles = new UnlkOp().execute(cpu, bus, decoded(Opcode.UNLK, Size.UNSIZED,
            EffectiveAddress.addrReg(6), EffectiveAddress.none(), 0));

        assertAll(
            () -> assertEquals(Unlk.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1100, cpu.registers().stackPointer()),
            () -> assertEquals(0x2222_0000, cpu.registers().address(6))
        );
    }

    @Test
    void peaJsrAndRtsUseSharedStackHelpers() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setStackPointer(0x1100);
        cpu.registers().setProgramCounter(0x2004);

        int peaCycles = new PeaOp().execute(cpu, bus, decoded(Opcode.PEA, Size.LONG,
            EffectiveAddress.absoluteLong(0x3344_5566), EffectiveAddress.none(), 0));
        int jsrCycles = new JsrOp().execute(cpu, bus, decoded(Opcode.JSR, Size.UNSIZED,
            EffectiveAddress.absoluteLong(0x0000_3000), EffectiveAddress.none(), 0));
        int rtsCycles = new RtsOp().execute(cpu, bus, decoded(Opcode.RTS, Size.UNSIZED,
            EffectiveAddress.none(), EffectiveAddress.none(), 0));

        assertAll(
            () -> assertEquals(12, peaCycles),
            () -> assertEquals(16, jsrCycles),
            () -> assertEquals(16, rtsCycles),
            () -> assertEquals(0x10FC, cpu.registers().stackPointer()),
            () -> assertEquals(0x2004, cpu.registers().programCounter()),
            () -> assertEquals(0x3344_5566, bus.readLong(0x10FC)),
            () -> assertEquals(0x0000_2004, bus.readLong(0x10F8))
        );
    }

    @Test
    void jmpOpWritesProgramCounter() {
        M68kCpu cpu = new M68kCpu();

        int cycles = new JmpOp().execute(cpu, null, decoded(Opcode.JMP, Size.UNSIZED,
            EffectiveAddress.absoluteLong(0x0000_4000), EffectiveAddress.none(), 0));

        assertAll(
            () -> assertEquals(8, cycles),
            () -> assertEquals(0x0000_4000, cpu.registers().programCounter())
        );
    }

    @Test
    void braOpBranchesRelativeToCurrentProgramCounter() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x2002);

        int cycles = new BraOp().execute(cpu, null, decoded(Opcode.BRA, Size.BYTE,
            EffectiveAddress.immediate(6), EffectiveAddress.none(), 0));

        assertAll(
            () -> assertEquals(10, cycles),
            () -> assertEquals(0x2008, cpu.registers().programCounter())
        );
    }

    @Test
    void braOpRejectsOddTargetAddress() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x2002);

        assertThrows(ProgramCounterAddressErrorException.class, () ->
            new BraOp().execute(cpu, null, decoded(Opcode.BRA, Size.BYTE,
                EffectiveAddress.immediate(-1), EffectiveAddress.none(), 0)));
    }

    @Test
    void bsrOpPushesReturnAddressAndBranchesRelativeToCurrentProgramCounter() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x2004);
        cpu.registers().setStackPointer(0x1100);

        int cycles = new BsrOp().execute(cpu, bus, decoded(Opcode.BSR, Size.WORD,
            EffectiveAddress.immediate(8), EffectiveAddress.none(), 0));

        assertAll(
            () -> assertEquals(18, cycles),
            () -> assertEquals(0x200A, cpu.registers().programCounter()),
            () -> assertEquals(0x10FC, cpu.registers().stackPointer()),
            () -> assertEquals(0x0000_2004, bus.readLong(0x10FC))
        );
    }

    @Test
    void bccOpRespectsConditionCodes() {
        M68kCpu branchingCpu = new M68kCpu();
        branchingCpu.registers().setProgramCounter(0x3002);
        branchingCpu.statusRegister().setZero(false);

        int takenCycles = new BccOp().execute(branchingCpu, null, decoded(Opcode.BCC, Size.BYTE,
            EffectiveAddress.immediate(4), EffectiveAddress.none(), 0x6));

        M68kCpu fallthroughCpu = new M68kCpu();
        fallthroughCpu.registers().setProgramCounter(0x3002);
        fallthroughCpu.statusRegister().setZero(true);

        int fallthroughCycles = new BccOp().execute(fallthroughCpu, null, decoded(Opcode.BCC, Size.BYTE,
            EffectiveAddress.immediate(4), EffectiveAddress.none(), 0x6));

        assertAll(
            () -> assertEquals(10, takenCycles),
            () -> assertEquals(10, fallthroughCycles),
            () -> assertEquals(0x3006, branchingCpu.registers().programCounter()),
            () -> assertEquals(0x3002, fallthroughCpu.registers().programCounter()),
            () -> assertFalse(branchingCpu.statusRegister().isZeroSet()),
            () -> assertTrue(fallthroughCpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void dbccOpDecrementsLowWordAndBranchesWhenConditionIsFalse() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x3004);
        cpu.registers().setData(0, 0x1234_0001);

        int cycles = new DbccOp().execute(cpu, null, decoded(Opcode.DBcc, Size.UNSIZED,
            EffectiveAddress.immediate(-4), EffectiveAddress.dataReg(0), 0x1));

        assertAll(
            () -> assertEquals(10, cycles),
            () -> assertEquals(0x1234_0000, cpu.registers().data(0)),
            () -> assertEquals(0x2FFE, cpu.registers().programCounter())
        );
    }

    @Test
    void dbccOpFallsThroughWithoutTouchingFlagsWhenConditionIsTrue() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x3004);
        cpu.registers().setData(0, 0x0000_0001);
        cpu.statusRegister().setZero(false);
        cpu.statusRegister().setNegative(true);

        int cycles = new DbccOp().execute(cpu, null, decoded(Opcode.DBcc, Size.UNSIZED,
            EffectiveAddress.immediate(-4), EffectiveAddress.dataReg(0), 0x6));

        assertAll(
            () -> assertEquals(10, cycles),
            () -> assertEquals(0x0000_0001, cpu.registers().data(0)),
            () -> assertEquals(0x3004, cpu.registers().programCounter()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isNegativeSet())
        );
    }

    @Test
    void sccOpWritesConditionalByteWithoutTouchingFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5600);
        cpu.statusRegister().setZero(false);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setCarry(true);

        int cycles = new SccOp().execute(cpu, null, decoded(Opcode.Scc, Size.BYTE,
            EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0x6));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x1234_56FF, cpu.registers().data(0)),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet())
        );
    }

    @Test
    void movemMemToRegOpLoadsRegistersAndAppliesPostincrementWriteback() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(2, 0x1200);
        bus.writeLong(0x1200, 0xABCD_1234);
        bus.writeLong(0x1204, 0x5678_9ABC);

        int cycles = new MovemMemToRegOp().execute(cpu, bus, decoded(Opcode.MOVEM_MEM_TO_REG, Size.LONG,
            EffectiveAddress.addrRegIndPostInc(2), EffectiveAddress.none(), 0x0401));

        assertAll(
            () -> assertEquals(8, cycles),
            () -> assertEquals(0xABCD_1234, cpu.registers().data(0)),
            () -> assertEquals(0x1208, cpu.registers().address(2))
        );
    }

    @Test
    void movemRegToMemOpStoresRegistersSequentially() {
        AddressSpace bus = stackAndCodeBus();
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1111_2222);
        cpu.registers().setAddress(1, 0x3333_4444);

        int cycles = new MovemRegToMemOp().execute(cpu, bus, decoded(Opcode.MOVEM_REG_TO_MEM, Size.LONG,
            EffectiveAddress.none(), EffectiveAddress.absoluteLong(0x1300), 0x0201));

        assertAll(
            () -> assertEquals(8, cycles),
            () -> assertEquals(0x1111_2222, bus.readLong(0x1300)),
            () -> assertEquals(0x3333_4444, bus.readLong(0x1304))
        );
    }

    @Test
    void chkOpThrowsChkExceptionWhenRegisterIsNegative() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0xFFFF_FFFF);
        cpu.registers().setData(1, 0x0000_0010);

        ChkException thrown = assertThrows(
            ChkException.class,
            () -> new ChkOp().execute(cpu, null, decoded(Opcode.CHK, Size.WORD,
                EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(0), 0))
        );

        assertAll(
            () -> assertEquals(6, thrown.vector()),
            () -> assertEquals("CHK triggered exception vector 6", thrown.getMessage())
        );
    }

    private static AddressSpace stackAndCodeBus() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 0x1000));
        return bus;
    }

    private static DecodedInstruction decoded(Opcode opcode, Size size, EffectiveAddress src, EffectiveAddress dst, int extension) {
        return new DecodedInstruction(opcode, size, src, dst, extension, 0x0040_0104);
    }
}
