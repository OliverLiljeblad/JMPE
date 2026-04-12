package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Or;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriOpTest {
    @Test
    void executesOriByteImmediateOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureOriScenario(cpu, 0x1234_5600);

        int cycles = new OriOp().execute(cpu, null, decodedOri(Size.BYTE, 0x80, EffectiveAddress.dataReg(0)));

        assertEquals(Or.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_5680, cpu.registers().data(0));
        assertOriNegativeFlags(cpu);
    }

    @Test
    void executesOriWordImmediateOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureOriScenario(cpu, 0x1234_0000);

        int cycles = new OriOp().execute(cpu, null, decodedOri(Size.WORD, 0x8001, EffectiveAddress.dataReg(0)));

        assertEquals(Or.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_8001, cpu.registers().data(0));
        assertOriNegativeFlags(cpu);
    }

    @Test
    void executesOriLongImmediateOnDataRegisterAndUpdatesTheWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        configureOriScenario(cpu, 0x0234_5678);

        int cycles = new OriOp().execute(cpu, null, decodedOri(Size.LONG, 0x8000_0000, EffectiveAddress.dataReg(0)));

        assertEquals(Or.EXECUTION_CYCLES, cycles);
        assertEquals(0x8234_5678, cpu.registers().data(0));
        assertOriNegativeFlags(cpu);
    }

    @Test
    void rejectsWrongOpcodeOrUnsizedDecode() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.NOP,
                Size.BYTE,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );
        DecodedInstruction unsized = new DecodedInstruction(
                Opcode.ORI,
                Size.UNSIZED,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new OriOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new OriOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.ORI,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.ORI,
                Size.BYTE,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.none(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.ORI,
                Size.BYTE,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new OriOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new OriOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new OriOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new OriOp().execute(null, null, decodedOri(Size.BYTE, 0x80, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new OriOp().execute(new M68kCpu(), null, null));
    }

    private static void assertOriNegativeFlags(M68kCpu cpu) {
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    private static void configureOriScenario(M68kCpu cpu, int initialValue) {
        cpu.registers().setData(0, initialValue);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static DecodedInstruction decodedOri(Size size, int immediate, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.ORI,
                size,
                EffectiveAddress.immediate(immediate),
                destination,
                0,
                0x0040_0104
        );
    }
}
