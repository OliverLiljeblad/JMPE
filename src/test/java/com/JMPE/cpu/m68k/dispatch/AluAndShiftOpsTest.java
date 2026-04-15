package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addi;
import com.JMPE.cpu.m68k.instructions.arithmetic.Neg;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subi;
import org.junit.jupiter.api.Test;

class AluAndShiftOpsTest {
    @Test
    void addOpWritesResultAndArithmeticFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x7FFF);

        int cycles = new AddOp().execute(cpu, null, decoded(Opcode.ADD, Size.WORD,
            EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_8000, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet())
        );
    }

    @Test
    void subOpWritesResultAndBorrowFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000);

        int cycles = new SubOp().execute(cpu, null, decoded(Opcode.SUB, Size.BYTE,
            EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_00FF, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void addiOpWritesResultAndArithmeticFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x007F);

        int cycles = new AddiOp().execute(cpu, null, decoded(Opcode.ADDI, Size.BYTE,
            EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Addi.EXECUTION_CYCLES_DN, cycles),
            () -> assertEquals(0x0000_0080, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertFalse(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void subiOpWritesResultAndBorrowFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000);

        int cycles = new SubiOp().execute(cpu, null, decoded(Opcode.SUBI, Size.BYTE,
            EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Subi.EXECUTION_CYCLES_DN, cycles),
            () -> assertEquals(0x0000_00FF, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void negOpUsesResolverBackedReadModifyWrite() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_0001);

        int cycles = new NegOp().execute(cpu, null, decoded(Opcode.NEG, Size.BYTE,
            EffectiveAddress.none(), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Neg.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_00FF, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void cmpOpUpdatesFlagsWithoutWritingDestination() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234);

        int cycles = new CmpOp().execute(cpu, null, decoded(Opcode.CMP, Size.WORD,
            EffectiveAddress.immediate(0x1234), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x1234, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isNegativeSet())
        );
    }

    @Test
    void logicalOpsUseResolverBackedReadModifyWrite() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0F0F);
        cpu.registers().setData(1, 0x00FF);
        cpu.registers().setData(2, 0x00F0);

        new AndOp().execute(cpu, null, decoded(Opcode.AND, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1)));
        new OrOp().execute(cpu, null, decoded(Opcode.OR, Size.WORD, EffectiveAddress.immediate(0x0F00), EffectiveAddress.dataReg(1)));
        int cycles = new EorOp().execute(cpu, null, decoded(Opcode.EOR, Size.WORD, EffectiveAddress.dataReg(2), EffectiveAddress.dataReg(1)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_0FFF, cpu.registers().data(1)),
            () -> assertFalse(cpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void addqOpSupportsAddressRegisterDestinationWithoutChangingFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(0, 0x1000);
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new AddqOp().execute(cpu, null, decoded(Opcode.ADDQ, Size.LONG,
            EffectiveAddress.immediate(8), EffectiveAddress.addrReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x1008, cpu.registers().address(0)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void subqOpRejectsByteAddressRegisterDestination() {
        M68kCpu cpu = new M68kCpu();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new SubqOp().execute(cpu, null, decoded(Opcode.SUBQ, Size.BYTE,
                EffectiveAddress.immediate(1), EffectiveAddress.addrReg(0)))
        );

        assertEquals("SUBQ to address register must not be decoded as BYTE", thrown.getMessage());
    }

    @Test
    void shiftOpsAcceptImmediateAndRegisterCounts() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x80);
        cpu.registers().setData(1, 1);
        cpu.registers().setData(2, 0x80);
        cpu.registers().setData(3, 1);
        cpu.registers().setData(4, 0x4000);
        cpu.registers().setData(5, 1);

        new AslOp().execute(cpu, null, decoded(Opcode.ASL, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0)));
        new AsrOp().execute(cpu, null, decoded(Opcode.ASR, Size.BYTE, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));
        new LslOp().execute(cpu, null, decoded(Opcode.LSL, Size.WORD, EffectiveAddress.dataReg(5), EffectiveAddress.dataReg(4)));
        new LsrOp().execute(cpu, null, decoded(Opcode.LSR, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3)));
        new RolOp().execute(cpu, null, decoded(Opcode.ROL, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3)));
        int cycles = new RorOp().execute(cpu, null, decoded(Opcode.ROR, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_0000, cpu.registers().data(0) & 0xFF),
            () -> assertEquals(0x0000_00C0, cpu.registers().data(2) & 0xFF),
            () -> assertEquals(0x0000_8000, cpu.registers().data(4) & 0xFFFF)
        );
    }

    private static DecodedInstruction decoded(Opcode opcode, Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(opcode, size, src, dst, 0, 0x0040_0104);
    }
}
