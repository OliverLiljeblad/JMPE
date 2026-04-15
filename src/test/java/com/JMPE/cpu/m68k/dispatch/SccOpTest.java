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
import com.JMPE.cpu.m68k.instructions.control.Scc;
import org.junit.jupiter.api.Test;

class SccOpTest {
    @Test
    void executesSccToDataRegisterAndPreservesFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5600);
        cpu.statusRegister().setZero(false);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setExtend(true);

        int cycles = new SccOp().execute(cpu, null, decoded(EffectiveAddress.dataReg(0), 0x6));

        assertEquals(Scc.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_56FF, cpu.registers().data(0));
        assertFalse(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isOverflowSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void executesFalseConditionAsZeroByteWrite() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 64));
        bus.writeByte(0x1004, 0xAA);

        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setZero(false);

        int cycles = new SccOp().execute(cpu, bus, decoded(EffectiveAddress.absoluteLong(0x1004), 0x7));

        assertEquals(Scc.EXECUTION_CYCLES, cycles);
        assertEquals(0x00, bus.readByte(0x1004));
        assertFalse(cpu.statusRegister().isZeroSet());
    }

    @Test
    void rejectsWrongOpcodeSizeSourceOrMissingDestination() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new SccOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.BCC, Size.BYTE, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0x6, 0x0040_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SccOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.Scc, Size.UNSIZED, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0x6, 0x0040_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SccOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.Scc, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0), 0x6, 0x0040_0102)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SccOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.Scc, Size.BYTE, EffectiveAddress.none(), EffectiveAddress.none(), 0x6, 0x0040_0102)
        ));
    }

    @Test
    void rejectsNullCpuOrDecodedInstruction() {
        assertThrows(NullPointerException.class, () -> new SccOp().execute(null, null, decoded(EffectiveAddress.dataReg(0), 0x6)));
        assertThrows(NullPointerException.class, () -> new SccOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decoded(EffectiveAddress dst, int extension) {
        return new DecodedInstruction(Opcode.Scc, Size.BYTE, EffectiveAddress.none(), dst, extension, 0x0040_0102);
    }
}
