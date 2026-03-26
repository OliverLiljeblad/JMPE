package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AndiToCcrOpTest {
    @Test
    void executesAndiToCcrAndPreservesUpperStatusBits() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x251F);

        int cycles = new AndiToCcrOp().execute(cpu, decodedAndiToCcr(0x0015));

        assertEquals(4, cycles);
        assertEquals(0x2515, cpu.statusRegister().rawValue());
        assertEquals(0x15, cpu.statusRegister().conditionCodeRegister());
    }

    @Test
    void rejectsWrongOpcodeOrWrongSize() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.ANDI,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
        DecodedInstruction wrongSize = new DecodedInstruction(
                Opcode.ANDI_TO_CCR,
                Size.WORD,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new AndiToCcrOp().execute(cpu, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new AndiToCcrOp().execute(cpu, wrongSize));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.ANDI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withWrongDestination = new DecodedInstruction(
                Opcode.ANDI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.ANDI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(0x15),
                EffectiveAddress.ccr(),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new AndiToCcrOp().execute(cpu, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new AndiToCcrOp().execute(cpu, withWrongDestination));
        assertThrows(IllegalArgumentException.class, () -> new AndiToCcrOp().execute(cpu, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new AndiToCcrOp().execute(null, decodedAndiToCcr(0x15)));
        assertThrows(NullPointerException.class, () -> new AndiToCcrOp().execute(new M68kCpu(), null));
    }

    private static DecodedInstruction decodedAndiToCcr(int immediate) {
        return new DecodedInstruction(
                Opcode.ANDI_TO_CCR,
                Size.BYTE,
                EffectiveAddress.immediate(immediate),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
    }
}
