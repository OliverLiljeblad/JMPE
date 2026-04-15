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
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Tas;
import org.junit.jupiter.api.Test;

class TasOpTest {
    @Test
    void setsBitSevenInDataRegisterLowByteAndUpdatesFlagsFromOriginalValue() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x1234_5601);
        cpu.statusRegister().setExtend(true);

        int cycles = new TasOp().execute(cpu, null, decoded(EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Tas.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x1234_5681, cpu.registers().data(0)),
            () -> assertFalse(cpu.statusRegister().isNegativeSet()),
            () -> assertFalse(cpu.statusRegister().isZeroSet()),
            () -> assertFalse(cpu.statusRegister().isOverflowSet()),
            () -> assertFalse(cpu.statusRegister().isCarrySet()),
            () -> assertTrue(cpu.statusRegister().isExtendSet())
        );
    }

    @Test
    void setsBitSevenInMemoryByte() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));
        bus.writeByte(0x2000, 0x00);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(1, 0x2000);

        int cycles = new TasOp().execute(cpu, bus, decoded(EffectiveAddress.addrRegInd(1)));

        assertAll(
            () -> assertEquals(Tas.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x80, bus.readByte(0x2000)),
            () -> assertTrue(cpu.statusRegister().isZeroSet())
        );
    }

    @Test
    void rejectsNonAlterableDestinations() {
        M68kCpu cpu = new M68kCpu();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new TasOp().execute(cpu, null, decoded(EffectiveAddress.addrReg(0)))
        );

        assertEquals("TAS requires data-alterable destination but was AddrReg[reg=0]", thrown.getMessage());
    }

    private static DecodedInstruction decoded(EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.TAS, Size.BYTE, EffectiveAddress.none(), dst, 0, 0x1002);
    }
}
