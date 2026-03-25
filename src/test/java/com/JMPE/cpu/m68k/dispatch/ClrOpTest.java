package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Clr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClrOpTest {
    @Test
    void executesClrByteOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureClrScenario(cpu, 0x1234_5678);

        int cycles = new ClrOp().execute(cpu, decodedClr(Size.BYTE, EffectiveAddress.dataReg(0)));

        assertEquals(Clr.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_5600, cpu.registers().data(0));
        assertClrFlags(cpu);
    }

    @Test
    void executesClrWordOnDataRegisterAndPreservesUpperBits() {
        M68kCpu cpu = new M68kCpu();
        configureClrScenario(cpu, 0x1234_5678);

        int cycles = new ClrOp().execute(cpu, decodedClr(Size.WORD, EffectiveAddress.dataReg(0)));

        assertEquals(Clr.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_0000, cpu.registers().data(0));
        assertClrFlags(cpu);
    }

    @Test
    void executesClrLongOnDataRegisterAndClearsTheWholeRegister() {
        M68kCpu cpu = new M68kCpu();
        configureClrScenario(cpu, 0x1234_5678);

        int cycles = new ClrOp().execute(cpu, decodedClr(Size.LONG, EffectiveAddress.dataReg(0)));

        assertEquals(Clr.EXECUTION_CYCLES, cycles);
        assertEquals(0x0000_0000, cpu.registers().data(0));
        assertClrFlags(cpu);
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
                Opcode.CLR,
                Size.UNSIZED,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withSource = new DecodedInstruction(
                Opcode.CLR,
                Size.BYTE,
                EffectiveAddress.immediate(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0102
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.CLR,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.none(),
                0,
                0x0040_0102
        );
        DecodedInstruction withMemoryDestination = new DecodedInstruction(
                Opcode.CLR,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.addrRegInd(0),
                0,
                0x0040_0102
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.CLR,
                Size.BYTE,
                EffectiveAddress.none(),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, withSource));
        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, withMemoryDestination));
        assertThrows(IllegalArgumentException.class, () -> new ClrOp().execute(cpu, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new ClrOp().execute(null, decodedClr(Size.BYTE, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new ClrOp().execute(new M68kCpu(), null));
    }

    private static void assertClrFlags(M68kCpu cpu) {
        assertFalse(cpu.statusRegister().isNegativeSet());
        assertTrue(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
        assertFalse(cpu.statusRegister().isCarrySet());
        assertTrue(cpu.statusRegister().isExtendSet());
    }

    private static void configureClrScenario(M68kCpu cpu, int initialValue) {
        cpu.registers().setData(0, initialValue);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setZero(false);
    }

    private static DecodedInstruction decodedClr(Size size, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.CLR,
                size,
                EffectiveAddress.none(),
                destination,
                0,
                0x0040_0102
        );
    }
}
