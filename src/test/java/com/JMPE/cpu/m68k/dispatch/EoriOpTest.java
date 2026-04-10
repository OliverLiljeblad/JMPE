package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Eor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EoriOpTest {
    @Test
    void executesEoriByteImmediateOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureEoriScenario(cpu, 0x1234_5600);

        int cycles = new EoriOp().execute(cpu, null, decodedEori(Size.BYTE, 0x80, EffectiveAddress.dataReg(0)));

        assertEquals(Eor.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_5680, cpu.registers().data(0));
        assertEoriNegativeFlags(cpu);
    }

    @Test
    void executesEoriWordImmediateOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureEoriScenario(cpu, 0x1234_0000);

        int cycles = new EoriOp().execute(cpu, null, decodedEori(Size.WORD, 0x8001, EffectiveAddress.dataReg(0)));

        assertEquals(Eor.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_8001, cpu.registers().data(0));
        assertEoriNegativeFlags(cpu);
    }

    @Test
    void executesEoriLongImmediateOnDataRegisterAndUpdatesTheWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        configureEoriScenario(cpu, 0x0234_5678);

        int cycles = new EoriOp().execute(cpu, null, decodedEori(Size.LONG, 0x8000_0000, EffectiveAddress.dataReg(0)));

        assertEquals(Eor.EXECUTION_CYCLES, cycles);
        assertEquals(0x8234_5678, cpu.registers().data(0));
        assertEoriNegativeFlags(cpu);
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
                Opcode.EORI,
                Size.UNSIZED,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new EoriOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.EORI,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.EORI,
                Size.BYTE,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.none(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.EORI,
                Size.BYTE,
                EffectiveAddress.immediate(0x80),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new EoriOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new EoriOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new EoriOp().execute(null, null, decodedEori(Size.BYTE, 0x80, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new EoriOp().execute(new M68kCpu(), null, null));
    }

    private static void assertEoriNegativeFlags(M68kCpu cpu) {
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    private static void configureEoriScenario(M68kCpu cpu, int initialValue) {
        cpu.registers().setData(0, initialValue);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static DecodedInstruction decodedEori(Size size, int immediate, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.EORI,
                size,
                EffectiveAddress.immediate(immediate),
                destination,
                0,
                0x0040_0104
        );
    }
}
