package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Exg;
import com.JMPE.cpu.m68k.instructions.data.Swap;
import org.junit.jupiter.api.Test;

class MoveFamilyOpTest {
    @Test
    void moveOpWritesSizedResultAndUpdatesMoveFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_0000);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);

        int cycles = new MoveOp().execute(cpu, null, decoded(Opcode.MOVE, Size.WORD,
            EffectiveAddress.immediate(0x8001), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x1234_8001, cpu.registers().data(0)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void moveaOpSignExtendsWordSourceAndPreservesFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new MoveaOp().execute(cpu, null, decoded(Opcode.MOVEA, Size.WORD,
            EffectiveAddress.immediate(0x8000), EffectiveAddress.addrReg(1)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0xFFFF_8000, cpu.registers().address(1)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void moveQOpSignExtendsLowByteAndUpdatesFlags() {
        M68kCpu cpu = new M68kCpu();

        int cycles = new MoveQOp().execute(cpu, null, decoded(Opcode.MOVEQ, Size.LONG,
            EffectiveAddress.immediate(0x80), EffectiveAddress.dataReg(3)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(-128, cpu.registers().data(3)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void swapOpExchangesRegisterHalvesAndUpdatesMoveFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x1234_5678);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);

        int cycles = new SwapOp().execute(cpu, null, decoded(Opcode.SWAP, Size.LONG,
            EffectiveAddress.none(), EffectiveAddress.dataReg(1)));

        assertAll(
            () -> assertEquals(Swap.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x5678_1234, cpu.registers().data(1)),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void exgOpSwapsDataAndAddressRegistersWithoutChangingFlags() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x1111_2222);
        cpu.registers().setAddress(5, 0x3333_4444);
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new ExgOp().execute(cpu, null, decoded(Opcode.EXG, Size.LONG,
            EffectiveAddress.dataReg(1), EffectiveAddress.addrReg(5)));

        assertAll(
            () -> assertEquals(Exg.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x3333_4444, cpu.registers().data(1)),
            () -> assertEquals(0x1111_2222, cpu.registers().address(5)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void moveFromSrOpCopiesWordWithoutChangingStatusRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new MoveFromSrOp().execute(cpu, null, decoded(Opcode.MOVE_FROM_SR, Size.WORD,
            EffectiveAddress.sr(), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_A71F, cpu.registers().data(0)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void moveToCcrOpLoadsLowByteOnly() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x2700);
        cpu.registers().setData(0, 0x0000_0015);

        int cycles = new MoveToCcrOp().execute(cpu, null, decoded(Opcode.MOVE_TO_CCR, Size.WORD,
            EffectiveAddress.dataReg(0), EffectiveAddress.ccr()));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x2715, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void moveToSrOpRequiresSupervisorMode() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(false);
        cpu.registers().setData(0, 0x0000_2700);

        PrivilegeViolation thrown = assertThrows(
            PrivilegeViolation.class,
            () -> new MoveToSrOp().execute(cpu, null, decoded(Opcode.MOVE_TO_SR, Size.WORD,
                EffectiveAddress.dataReg(0), EffectiveAddress.sr()))
        );

        assertEquals("MOVE to SR requires supervisor mode", thrown.getMessage());
    }

    @Test
    void moveToSrOpWritesFullStatusRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(true);
        cpu.registers().setData(0, 0x0000_2304);

        int cycles = new MoveToSrOp().execute(cpu, null, decoded(Opcode.MOVE_TO_SR, Size.WORD,
            EffectiveAddress.dataReg(0), EffectiveAddress.sr()));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x2304, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void oriToCcrOpOrsOnlyConditionCodeBits() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x2701);

        int cycles = new OriToCcrOp().execute(cpu, null, decoded(Opcode.ORI_TO_CCR, Size.BYTE,
            EffectiveAddress.immediate(0x14), EffectiveAddress.ccr()));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x2715, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void oriToSrOpRequiresSupervisorAndOrsWholeWord() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setRawValue(0x2004);

        int cycles = new OriToSrOp().execute(cpu, null, decoded(Opcode.ORI_TO_SR, Size.WORD,
            EffectiveAddress.immediate(0x0710), EffectiveAddress.sr()));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x2714, cpu.statusRegister().rawValue())
        );
    }

    private static DecodedInstruction decoded(Opcode opcode, Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(opcode, size, src, dst, 0, 0x0040_0104);
    }
}
