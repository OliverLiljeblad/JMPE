package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Tst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TstOpTest {
    @Test
    void executesTstOnDataRegisterWithoutModifyingTheRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_0080);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);

        int cycles = new TstOp().execute(cpu, null, decodedTst(Size.BYTE, EffectiveAddress.dataReg(0)));

        assertEquals(Tst.EXECUTION_CYCLES, cycles);
        assertEquals(0x0000_0080, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void rejectsWrongOpcodeOrUnsizedDecode() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.NOP,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );
        DecodedInstruction unsized = new DecodedInstruction(
                Opcode.TST,
                Size.UNSIZED,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new TstOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new TstOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withSource = new DecodedInstruction(
                Opcode.TST,
                Size.BYTE,
                EffectiveAddress.immediate(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.TST,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.none(),
                0,
                0x0040_0102
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.TST,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new TstOp().execute(cpu, null, withSource));
        assertThrows(IllegalArgumentException.class, () -> new TstOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new TstOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new TstOp().execute(null, null, decodedTst(Size.BYTE, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new TstOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedTst(Size size, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.TST,
                size,
                EffectiveAddress.none(),
                destination,
                0,
                0x0040_0102
        );
    }
}
