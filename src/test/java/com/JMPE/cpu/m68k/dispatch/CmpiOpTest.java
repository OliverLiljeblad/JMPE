package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Cmp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmpiOpTest {
    @Test
    void executesCmpiOnDataRegisterWithoutModifyingTheRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5600);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(false);
        cpu.statusRegister().setOverflow(true);
        cpu.statusRegister().setZero(true);

        int cycles = new CmpiOp().execute(cpu, null, decodedCmpi(Size.BYTE, 0x01, EffectiveAddress.dataReg(0)));

        assertEquals(Cmp.EXECUTION_CYCLES, cycles);
        assertEquals(0x1234_5600, cpu.registers().data(0));
        assertTrue(cpu.statusRegister().isNegativeSet());
        assertFalse(cpu.statusRegister().isZeroSet());
        assertFalse(cpu.statusRegister().isOverflowSet());
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
                0x0040_0104
        );
        DecodedInstruction unsized = new DecodedInstruction(
                Opcode.CMPI,
                Size.UNSIZED,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new CmpiOp().execute(cpu, null, wrongOpcode));
        assertThrows(IllegalArgumentException.class, () -> new CmpiOp().execute(cpu, null, unsized));
    }

    @Test
    void rejectsUnsupportedOperandsOrExtensionPayload() {
        M68kCpu cpu = new M68kCpu();
        DecodedInstruction withRegisterSource = new DecodedInstruction(
                Opcode.CMPI,
                Size.BYTE,
                EffectiveAddress.dataReg(1),
                EffectiveAddress.dataReg(0),
                0,
                0x0040_0104
        );
        DecodedInstruction withNoDestination = new DecodedInstruction(
                Opcode.CMPI,
                Size.BYTE,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.none(),
                0,
                0x0040_0104
        );
        DecodedInstruction withExtension = new DecodedInstruction(
                Opcode.CMPI,
                Size.BYTE,
                EffectiveAddress.immediate(0x01),
                EffectiveAddress.dataReg(0),
                1,
                0x0040_0104
        );

        assertThrows(IllegalArgumentException.class, () -> new CmpiOp().execute(cpu, null, withRegisterSource));
        assertThrows(IllegalArgumentException.class, () -> new CmpiOp().execute(cpu, null, withNoDestination));
        assertThrows(IllegalArgumentException.class, () -> new CmpiOp().execute(cpu, null, withExtension));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new CmpiOp().execute(null, null, decodedCmpi(Size.BYTE, 0x01, EffectiveAddress.dataReg(0))));
        assertThrows(NullPointerException.class, () -> new CmpiOp().execute(new M68kCpu(), null, null));
    }

    private static DecodedInstruction decodedCmpi(Size size, int immediate, EffectiveAddress destination) {
        return new DecodedInstruction(
                Opcode.CMPI,
                size,
                EffectiveAddress.immediate(immediate),
                destination,
                0,
                0x0040_0104
        );
    }
}
