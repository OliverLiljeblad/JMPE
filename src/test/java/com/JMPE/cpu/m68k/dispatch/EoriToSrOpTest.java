package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EoriToSrOpTest {
    @Test
    void executesEoriToSrAndUpdatesTheFullStatusRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new EoriToSrOp().execute(cpu, null, decodedEoriToSr(0x20F0));

        assertEquals(4, cycles);
        assertEquals(0x870F, cpu.statusRegister().rawValue());
    }

    @Test
    void rejectsUserModeExecution() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x071F);

        PrivilegeViolation thrown = assertThrows(
                PrivilegeViolation.class,
                () -> new EoriToSrOp().execute(cpu, null, decodedEoriToSr(0x20F0))
        );

        assertEquals("EORI to SR requires supervisor mode", thrown.getMessage());
        assertEquals(0x071F, cpu.statusRegister().rawValue());
    }

    @Test
    void rejectsWrongOpcodeOrWrongSize() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction wrongOpcode = new DecodedInstruction(
                Opcode.EORI,
                Size.WORD,
                EffectiveAddress.immediate(0x20F0),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );
        DecodedInstruction wrongSize = new DecodedInstruction(
                Opcode.EORI_TO_SR,
                Size.BYTE,
                EffectiveAddress.immediate(0x20F0),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriToSrOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new EoriToSrOp().execute(cpu, null, wrongSize));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(true);
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.EORI_TO_SR,
                Size.WORD,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withWrongDestination = new DecodedInstruction(
                Opcode.EORI_TO_SR,
                Size.WORD,
                EffectiveAddress.immediate(0x20F0),
                EffectiveAddress.ccr(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.EORI_TO_SR,
                Size.WORD,
                EffectiveAddress.immediate(0x20F0),
                EffectiveAddress.sr(),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new EoriToSrOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new EoriToSrOp().execute(cpu, null, withWrongDestination));
        assertThrows(IllegalArgumentException.class, () -> new EoriToSrOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new EoriToSrOp().execute(null, null, decodedEoriToSr(0x20F0)));
        assertThrows(NullPointerException.class, () -> new EoriToSrOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedEoriToSr(int immediate) {
        return new DecodedInstruction(
                Opcode.EORI_TO_SR,
                Size.WORD,
                EffectiveAddress.immediate(immediate),
                EffectiveAddress.sr(),
                0,
                0x0040_0104
        );
    }
}
