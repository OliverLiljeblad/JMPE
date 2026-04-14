package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.shift.Roxr;
import org.junit.jupiter.api.Test;

class RoxrOpTest {
    @Test
    void rotatesRegisterDestinationThroughExtend() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(1, 1);
        cpu.registers().setData(2, 0x01);
        cpu.statusRegister().setExtend(true);

        int cycles = new RoxrOp().execute(cpu, null, decoded(Size.BYTE,
            EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2)));

        assertAll(
            () -> assertEquals(Roxr.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x80, cpu.registers().data(2)),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet()),
            () -> assertTrue(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void rotatesMemoryDestinationWithImplicitSingleStepCount() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x1000));
        bus.writeWord(0x2000, 0x0001);

        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setExtend(false);

        int cycles = new RoxrOp().execute(cpu, bus, decoded(Size.WORD,
            EffectiveAddress.none(), EffectiveAddress.absoluteLong(0x2000)));

        assertAll(
            () -> assertEquals(Roxr.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000, bus.readWord(0x2000)),
            () -> assertTrue(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet()),
            () -> assertTrue(cpu.statusRegister().isZeroSet())
        );
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.ROXR, size, src, dst, 0, 0x1002);
    }
}
