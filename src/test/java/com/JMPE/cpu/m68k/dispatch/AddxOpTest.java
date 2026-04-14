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
import com.JMPE.cpu.m68k.instructions.arithmetic.Addx;
import org.junit.jupiter.api.Test;

class AddxOpTest {
    @Test
    void addsDataRegisterOperandsWithExtendAndPreservesClearedZero() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(4, 0x0000_0000);
        cpu.registers().setData(3, 0x0000_00FF);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setZero(false);

        int cycles = new AddxOp().execute(cpu, null, decoded(Size.BYTE,
            EffectiveAddress.dataReg(4), EffectiveAddress.dataReg(3)));

        assertAll(
            () -> assertEquals(Addx.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_0000, cpu.registers().data(3) & 0xFF),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void addsPredecrementMemoryOperandsInSourceThenDestinationOrder() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 0x100));
        bus.writeByte(0x1002, 0x01);
        bus.writeByte(0x1003, 0x02);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(0, 0x1004);

        int cycles = new AddxOp().execute(cpu, bus, decoded(Size.BYTE,
            EffectiveAddress.addrRegIndPreDec(0), EffectiveAddress.addrRegIndPreDec(0)));

        assertAll(
            () -> assertEquals(Addx.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1002, cpu.registers().address(0)),
            () -> assertEquals(0x03, bus.readByte(0x1002)),
            () -> assertEquals(0x02, bus.readByte(0x1003))
        );
    }

    @Test
    void rejectsMixedOperandShapes() {
        M68kCpu cpu = new M68kCpu();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new AddxOp().execute(
            cpu,
            null,
            decoded(Size.BYTE, EffectiveAddress.dataReg(0), EffectiveAddress.addrRegIndPreDec(0))
        ));

        assertEquals("ADDX requires matching data-register or predecrement operands", thrown.getMessage());
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.ADDX, size, src, dst, 0, 0x1002);
    }
}
