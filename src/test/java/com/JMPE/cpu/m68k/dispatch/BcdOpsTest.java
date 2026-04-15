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
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Abcd;
import com.JMPE.cpu.m68k.instructions.arithmetic.Nbcd;
import com.JMPE.cpu.m68k.instructions.arithmetic.Sbcd;
import org.junit.jupiter.api.Test;

class BcdOpsTest {
    @Test
    void abcdOpAddsRegisterOperandsAndPreservesUndefinedFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x0000_0055);
        cpu.registers().setData(2, 0x1234_0045);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);

        int cycles = new AbcdOp().execute(cpu, null, decoded(Opcode.ABCD, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Abcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_0000, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void abcdOpUsesSourceThenDestinationPredecrementForMemoryForm() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));
        bus.writeByte(0x2000, 0x12);
        bus.writeByte(0x2001, 0x34);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(0, 0x2001);
        cpu.registers().setAddress(1, 0x2002);
        cpu.statusRegister().setZero(true);

        int cycles = new AbcdOp().execute(cpu, bus, decoded(Opcode.ABCD,
            EffectiveAddress.addrRegIndPreDec(0), EffectiveAddress.addrRegIndPreDec(1)));

        assertAll(
            () -> assertEquals(Abcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x2000, cpu.registers().address(0)),
            () -> assertEquals(0x2001, cpu.registers().address(1)),
            () -> assertEquals(0x12, bus.readByte(0x2000)),
            () -> assertEquals(0x46, bus.readByte(0x2001)),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertFalse(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void sbcdOpSubtractsRegisterOperandsAndClearsStickyZeroOnNonZeroResult() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x0000_0001);
        cpu.registers().setData(2, 0x1234_0000);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);

        int cycles = new SbcdOp().execute(cpu, null, decoded(Opcode.SBCD, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Sbcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_0099, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void nbcdOpNegatesDataRegisterByteAndRequiresDataAlterableDestination() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_0001);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);

        int cycles = new NbcdOp().execute(cpu, null, decoded(Opcode.NBCD, EffectiveAddress.none(), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Nbcd.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_0099, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new NbcdOp().execute(cpu, null, decoded(Opcode.NBCD, EffectiveAddress.none(), EffectiveAddress.addrReg(0)))
        );
        assertEquals("NBCD requires a data-alterable destination but was AddrReg[reg=0]", thrown.getMessage());
    }

    private static DecodedInstruction decoded(Opcode opcode, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(opcode, Size.BYTE, src, dst, 0, 0x1002);
    }
}
