package com.JMPE.cpu.m68k.dispatch;

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
import com.JMPE.cpu.m68k.instructions.bit.Bset;
import org.junit.jupiter.api.Test;

class BsetOpTest {
    @Test
    void executesRegisterBsetAndOnlyUpdatesZeroFlag() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 33);
        cpu.registers().setData(1, 0x0000_0000);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setZero(false);

        int cycles = new BsetOp().execute(cpu, null, decoded(Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1)));

        assertEquals(Bset.EXECUTION_CYCLES, cycles);
        assertEquals(33, cpu.registers().data(0));
        assertEquals(0x0000_0002, cpu.registers().data(1));
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void executesMemoryBsetAndWritesUpdatedByte() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 64));
        bus.writeByte(0x1004, 0x02);

        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(true);

        int cycles = new BsetOp().execute(
            cpu,
            bus,
            decoded(Size.BYTE, EffectiveAddress.immediate(0), EffectiveAddress.absoluteLong(0x1004))
        );

        assertEquals(Bset.EXECUTION_CYCLES, cycles);
        assertEquals(0x03, bus.readByte(0x1004));
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
    }

    @Test
    void clearsZeroWhenBitWasAlreadySet() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 64));
        bus.writeByte(0x1004, 0x02);

        M68kCpu cpu = new M68kCpu();

        new BsetOp().execute(
            cpu,
            bus,
            decoded(Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.absoluteLong(0x1004))
        );

        assertFalse(cpu.statusRegister().isZeroSet());
        assertEquals(0x02, bus.readByte(0x1004));
    }

    @Test
    void rejectsWrongOpcodeInvalidSizeMissingOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new BsetOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.TST, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1), 0, 0x00C0_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BsetOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.BSET, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1), 0, 0x00C0_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BsetOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.BSET, Size.LONG, EffectiveAddress.none(), EffectiveAddress.dataReg(1), 0, 0x00C0_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BsetOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.BSET, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.none(), 0, 0x00C0_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BsetOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.BSET, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1), 1, 0x00C0_0102)
        ));
    }

    @Test
    void rejectsNullCpuOrDecodedInstruction() {
        assertThrows(NullPointerException.class, () -> new BsetOp().execute(null, null, decoded(Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1))));
        assertThrows(NullPointerException.class, () -> new BsetOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.BSET, size, src, dst, 0, 0x00C0_0102);
    }
}
