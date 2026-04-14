package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Negx;
import org.junit.jupiter.api.Test;

class NegxOpTest {
    @Test
    void negatesDestinationWithExtendAndPreservesClearedZero() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_00FF);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setZero(false);

        int cycles = new NegxOp().execute(cpu, null, decoded(Size.BYTE, EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Negx.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_0000, cpu.registers().data(0) & 0xFF),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void rejectsWrongOpcodeOrUnexpectedSource() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new NegxOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.NOP, Size.BYTE, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0, 0x1002)
        ));
        assertThrows(IllegalArgumentException.class, () -> new NegxOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.NEGX, Size.BYTE, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(0), 0, 0x1002)
        ));
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.NEGX, size, EffectiveAddress.none(), dst, 0, 0x1002);
    }
}
