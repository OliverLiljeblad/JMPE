package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddiOpTest {

    @Test
    void executesAddiOnDataRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_0010);

        int cycles = new AddiOp().execute(cpu, null, decodedAddi(Size.BYTE, 0x01, EffectiveAddress.dataReg(0)));

        assertEquals(Addi.EXECUTION_CYCLES_DN, cycles);
        assertEquals(0x0000_0011, cpu.registers().data(0));
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertFalse(cpu.statusRegister().isExtendSet());
    }

    @Test
    void setsZeroFlagWhenResultIsZero() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x0000_00FF);

        new AddiOp().execute(cpu, null, decodedAddi(Size.BYTE, 0x01, EffectiveAddress.dataReg(1)));

        assertEquals(0x0000_0000, cpu.registers().data(1) & 0xFF);
        assertTrue(cpu.statusRegister().isZeroSet());
        assertTrue(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    @Test
    void rejectsWrongOpcodeOrUnsizedDecode() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.NOP,
                Size.BYTE,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.dataReg(0),
                0,
                0x0640_0104
        );
        DecodedInstruction unsized = new DecodedInstruction(
                Opcode.ADDI,
                Size.UNSIZED,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.dataReg(0),
                0,
                0x0640_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new AddiOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new AddiOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.ADDI,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0640_0104
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.ADDI,
                Size.BYTE,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.none(),
                0,
                0x0640_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.ADDI,
                Size.BYTE,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.dataReg(0),
                1,
                0x0640_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new AddiOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new AddiOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new AddiOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new AddiOp().execute(null, null, decodedAddi(Size.BYTE, 0x01, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new AddiOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedAddi(Size size, int immediate, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.ADDI,
                size,
                EffectiveAddress.immediate(immediate),
                destination,
                0,
                0x0640_0104
        );
    }
}
