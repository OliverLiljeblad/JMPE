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
import com.JMPE.cpu.m68k.instructions.data.Ext;
import org.junit.jupiter.api.Test;

class ExtOpTest {
    @Test
    void extWordSignExtendsLowByteAndPreservesUpperWord() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5680);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);

        int cycles = new ExtOp().execute(cpu, null, decoded(Size.WORD, EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Ext.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_FF80, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void extLongSignExtendsLowWordIntoWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x1234_8001);

        int cycles = new ExtOp().execute(cpu, null, decoded(Size.LONG, EffectiveAddress.dataReg(1)));

        assertAll(
            () -> assertEquals(Ext.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFFFF_8001, cpu.registers().data(1)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void rejectsWrongOpcodeUnsupportedSizeWrongOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();

        assertThrows(IllegalArgumentException.class, () -> new ExtOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.TST, Size.WORD, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0, 0x0040_0104)
        ));
        assertThrows(IllegalArgumentException.class, () -> new ExtOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.EXT, Size.BYTE, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 0, 0x0040_0104)
        ));
        assertThrows(IllegalArgumentException.class, () -> new ExtOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.EXT, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(0), 0, 0x0040_0104)
        ));
        assertThrows(IllegalArgumentException.class, () -> new ExtOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.EXT, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrReg(0), 0, 0x0040_0104)
        ));
        assertThrows(IllegalArgumentException.class, () -> new ExtOp().execute(
            cpu,
            null,
            new DecodedInstruction(Opcode.EXT, Size.WORD, EffectiveAddress.none(), EffectiveAddress.dataReg(0), 1, 0x0040_0104)
        ));
    }

    @Test
    void rejectsNullCpuOrDecodedInstruction() {
        assertThrows(NullPointerException.class, () -> new ExtOp().execute(null, null, decoded(Size.WORD, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new ExtOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.EXT, size, EffectiveAddress.none(), dst, 0, 0x0040_0104);
    }
}
