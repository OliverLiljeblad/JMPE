package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Reset;
import org.junit.jupiter.api.Test;

class ResetOpTest {
    @Test
    void executesResetWithoutChangingCpuState() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0xA71F);
        cpu.registers().setProgramCounter(0x0040_0100);
        cpu.registers().setData(0, 0x1234_5678);

        int cycles = new ResetOp().execute(cpu, null, decodedReset());

        assertEquals(Reset.EXECUTION_CYCLES, cycles);
        assertEquals(0xA71F, cpu.statusRegister().rawValue());
        assertEquals(0x0040_0100, cpu.registers().programCounter());
        assertEquals(0x1234_5678, cpu.registers().data(0));
    }

    @Test
    void rejectsUserModeExecution() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x071F);

        PrivilegeViolation thrown = assertThrows(
            PrivilegeViolation.class,
            () -> new ResetOp().execute(cpu, null, decodedReset())
        );

        assertEquals("RESET requires supervisor mode", thrown.getMessage());
        assertEquals(0x071F, cpu.statusRegister().rawValue());
    }

    @Test
    void rejectsWrongOpcodeOrDecodedShape() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(true);

        DecodedInstruction wrongOpcode = new DecodedInstruction(
            Opcode.NOP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );
        DecodedInstruction withSource = new DecodedInstruction(
            Opcode.RESET,
            Size.UNSIZED,
            EffectiveAddress.immediate(1),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );

        assertThrows(IllegalArgumentException.class, () -> new ResetOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new ResetOp().execute(cpu, null, withSource));
    }

    private static DecodedInstruction decodedReset() {
        return new DecodedInstruction(
            Opcode.RESET,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            0x0040_0102
        );
    }
}
