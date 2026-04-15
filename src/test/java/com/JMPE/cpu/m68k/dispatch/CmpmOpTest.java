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
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmp;
import org.junit.jupiter.api.Test;

class CmpmOpTest {
    @Test
    void comparesMemoryOperandsAndPostincrementsBothRegisters() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));
        bus.writeByte(0x2000, 0x01);
        bus.writeByte(0x2002, 0x01);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(5, 0x2000);
        cpu.registers().setAddress(3, 0x2002);
        cpu.statusRegister().setExtend(true);

        int cycles = new CmpmOp().execute(cpu, bus, decoded(Size.BYTE,
            EffectiveAddress.addrRegIndPostInc(5), EffectiveAddress.addrRegIndPostInc(3)));

        assertEquals(Cmp.EXECUTION_CYCLES, cycles);
        assertEquals(0x2001, cpu.registers().address(5));
        assertEquals(0x2003, cpu.registers().address(3));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void rejectsNonPostincrementOperands() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new CmpmOp().execute(
            cpu,
            null,
            decoded(Size.BYTE, EffectiveAddress.dataReg(0), EffectiveAddress.addrRegIndPostInc(3))
        ));
        assertThrows(IllegalArgumentException.class, () -> new CmpmOp().execute(
            cpu,
            null,
            decoded(Size.BYTE, EffectiveAddress.addrRegIndPostInc(5), EffectiveAddress.dataReg(0))
        ));
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.CMPM, size, src, dst, 0, 0x1002);
    }
}
