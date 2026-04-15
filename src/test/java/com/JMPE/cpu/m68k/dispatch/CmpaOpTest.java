package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmpa;
import org.junit.jupiter.api.Test;

class CmpaOpTest {
    @Test
    void comparesSignExtendedWordSourceAgainstAddressRegisterWithoutModifyingIt() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_FFFF);
        cpu.registers().setAddress(1, 0xFFFF_FFFF);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(false);

        int cycles = new CmpaOp().execute(cpu, null, decoded(Size.WORD,
            EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(1)));

        assertEquals(Cmpa.EXECUTION_CYCLES, cycles);
        assertEquals(0xFFFF_FFFF, cpu.registers().address(1));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void rejectsWrongOpcodeOrDestinationShape() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new CmpaOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.NOP, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(1), 0, 0x1002)
        ));
        assertThrows(IllegalArgumentException.class, () -> new CmpaOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.CMPA, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1), 0, 0x1002)
        ));
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.CMPA, size, src, dst, 0, 0x1002);
    }
}
