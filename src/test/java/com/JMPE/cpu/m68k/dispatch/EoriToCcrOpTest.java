package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EoriToCcrOpTest {
    @Test
    void executesEoriToCcrAndPreservesUpperStatusBits() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x251F);

        int cycles = new EoriToCcrOp().execute(cpu, null, decodedEoriToCcr(0x0015));

        assertEquals(4, cycles);
        assertEquals(0x250A, cpu.statusRegister().rawValue());
        assertEquals(0x0A, cpu.statusRegister().conditionCodeRegister());
    }

    @Test
    void rejectsWrongOpcodeOrWrongSize() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.EORI,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
        DecodedInstruction wrongSize = new DecodedInstruction(
                Opcode.EORI_TO_CCR,
                Size.WORD,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriToCcrOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new EoriToCcrOp().execute(cpu, null, wrongSize));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.EORI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withWrongDestination = new DecodedInstruction(
                Opcode.EORI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.EORI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriToCcrOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new EoriToCcrOp().execute(cpu, null, withWrongDestination));
        assertThrows(IllegalArgumentException.class, () -> new EoriToCcrOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new EoriToCcrOp().execute(null, null, decodedEoriToCcr(0x15)));
        assertThrows(NullPointerException.class, () -> new EoriToCcrOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedEoriToCcr(int immediate) {
        return new DecodedInstruction(
                Opcode.EORI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(immediate),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
    }
}
