package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.DivideByZeroException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Divs;
import com.JMPE.cpu.m68k.instructions.arithmetic.Divu;
import com.JMPE.cpu.m68k.instructions.arithmetic.Muls;
import com.JMPE.cpu.m68k.instructions.arithmetic.Mulu;
import org.junit.jupiter.api.Test;

class MultiplyAndDivideOpsTest {
    @Test
    void muluOpReadsWordSourceAndWritesFullLongResult() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));
        bus.writeWord(0x2000, 3);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(2, 0xAAAA_0004);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);

        int cycles = new MuluOp().execute(cpu, bus, decoded(Opcode.MULU, Size.WORD,
            EffectiveAddress.absoluteLong(0x2000), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Mulu.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_000C, cpu.registers().data(2)),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void mulsOpSignExtendsLowWordsBeforeMultiplying() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0x0000_FFFE);
        cpu.registers().setData(2, 0xAAAA_0003);
        cpu.statusRegister().setExtend(true);

        int cycles = new MulsOp().execute(cpu, null, decoded(Opcode.MULS, Size.WORD,
            EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Muls.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFFFF_FFFA, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void divuOpStoresRemainderInHighWordAndQuotientInLowWord() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 3);
        cpu.registers().setData(2, 20);
        cpu.statusRegister().setExtend(true);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setOverflow(true);

        int cycles = new DivuOp().execute(cpu, null, decoded(Opcode.DIVU, Size.WORD,
            EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Divu.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0002_0006, cpu.registers().data(2)),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void divuOpSetsOverflowAndPreservesDestinationWhenQuotientDoesNotFitWord() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 1);
        cpu.registers().setData(2, 0x0001_0000);
        cpu.statusRegister().setNegative(true);
        cpu.statusRegister().setZero(false);
        cpu.statusRegister().setCarry(true);
        cpu.statusRegister().setExtend(true);

        int cycles = new DivuOp().execute(cpu, null, decoded(Opcode.DIVU, Size.WORD,
            EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Divu.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0001_0000, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertTrue(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void divsOpStoresSignedRemainderAndQuotient() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 3);
        cpu.registers().setData(2, -20);
        cpu.statusRegister().setExtend(true);

        int cycles = new DivsOp().execute(cpu, null, decoded(Opcode.DIVS, Size.WORD,
            EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Divs.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xFFFE_FFFA, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void divsOpThrowsDivideByZeroException() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 0);
        cpu.registers().setData(2, -20);
        cpu.statusRegister().setRawValue(0x001F);

        DivideByZeroException thrown = assertThrows(
            DivideByZeroException.class,
            () -> new DivsOp().execute(cpu, null, decoded(Opcode.DIVS, Size.WORD,
                EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)))
        );

        assertAll(
            () -> assertEquals(DivideByZeroException.VECTOR, thrown.vector()),
            () -> assertEquals("Integer divide by zero triggered exception vector 5", thrown.getMessage()),
            () -> assertEquals(-20, cpu.registers().data(2)),
            () -> assertEquals(0x001F, cpu.statusRegister().rawValue())
        );
    }

    private static DecodedInstruction decoded(Opcode opcode, Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(opcode, size, src, dst, 0, 0x1002);
    }
}
