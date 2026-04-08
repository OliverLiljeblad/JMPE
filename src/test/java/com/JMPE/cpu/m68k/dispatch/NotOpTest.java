package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.logic.Not;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotOpTest {
    @Test
    void executesNotByteOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureNotScenario(cpu, 0x1234_5678);

        int cycles = new NotOp().execute(cpu, null, decodedNot(Size.BYTE, EffectiveAddress.dataReg(0)));

        assertEquals(Not.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_5687, cpu.registers().data(0));
        assertNotFlags(cpu);
    }

    @Test
    void executesNotWordOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureNotScenario(cpu, 0x1234_5678);

        int cycles = new NotOp().execute(cpu, null, decodedNot(Size.WORD, EffectiveAddress.dataReg(0)));

        assertEquals(Not.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_A987, cpu.registers().data(0));
        assertNotFlags(cpu);
    }

    @Test
    void executesNotLongOnDataRegisterAndUpdatesTheWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        configureNotScenario(cpu, 0x1234_5678);

        int cycles = new NotOp().execute(cpu, null, decodedNot(Size.LONG, EffectiveAddress.dataReg(0)));

        assertEquals(Not.EXECUTION_CYCLES, cycles);
        assertEquals(0xEDCB_A987, cpu.registers().data(0));
        assertNotFlags(cpu);
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
                Opcode.NOT,
                Size.UNSIZED,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new NotOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new NotOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withSource = new DecodedInstruction(
                Opcode.NOT,
                Size.BYTE,
                EffectiveAddress.immediate(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.NOT,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.none(),
                0,
                0x0040_0102
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.NOT,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new NotOp().execute(cpu, null, withSource));
        assertThrows(IllegalArgumentException.class, () -> new NotOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new NotOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new NotOp().execute(null, null, decodedNot(Size.BYTE, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new NotOp().execute(new M68kCpu(), null, null));
    }

    private static void assertNotFlags(M68kCpu cpu) {
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    private static void configureNotScenario(M68kCpu cpu, int initialValue) {
        cpu.registers().setData(0, initialValue);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(false);
        cpu.statusRegister().setZero(true);
    }

    private static DecodedInstruction decodedNot(Size size, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.NOT,
                size,
                EffectiveAddress.none(),
                destination,
                0,
                0x0040_0102
        );
    }
}
