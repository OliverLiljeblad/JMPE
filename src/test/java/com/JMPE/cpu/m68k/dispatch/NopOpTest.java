package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Nop;
import org.junit.jupiter.api.Test;

class NopOpTest {
    @Test
    void executesNopWithoutChangingCpuState() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0040_0100);
        cpu.registers().setData(0, 0x1234_5678);
        cpu.statusRegister().setCarry(true);

        int cycles = new NopOp().execute(cpu, null, decodedNop());

        assertEquals(Nop.EXECUTION_CYCLES, cycles);
        assertEquals(0x0040_0100, cpu.registers().programCounter());
        assertEquals(0x1234_5678, cpu.registers().data(0));
        assertEquals(true, cpu.statusRegister().isCarrySet());
    }

    @Test
    void rejectsWrongOpcode() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction decoded = new DecodedInstruction(
            Opcode.RTS,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new NopOp().execute(cpu, null, decoded));
    }

    @Test
    void rejectsSizedNop() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction decoded = new DecodedInstruction(
            Opcode.NOP,
            Size.WORD,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new NopOp().execute(cpu, null, decoded));
    }

    @Test
    void rejectsUnexpectedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withSource = new DecodedInstruction(
            Opcode.NOP,
            Size.UNSIZED,
            EffectiveAddress.immediate(1),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );
        DecodedInstruction withDestination = new DecodedInstruction(
            Opcode.NOP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.dataReg(0),
            0,
            0x0040_0102
        );
        DecodedInstruction withExtension = new DecodedInstruction(
            Opcode.NOP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            1,
            0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new NopOp().execute(cpu, null, withSource));
        assertThrows(IllegalArgumentException.class, () -> new NopOp().execute(cpu, null, withDestination));
        assertThrows(IllegalArgumentException.class, () -> new NopOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new NopOp().execute(null, null, decodedNop()));
        assertThrows(NullPointerException.class, () -> new NopOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedNop() {
        return new DecodedInstruction(
            Opcode.NOP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );
    }
}
