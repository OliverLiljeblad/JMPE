package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Movep;
import org.junit.jupiter.api.Test;

class MovepOpTest {
    @Test
    void readsSpacedWordBytesIntoLowWordOfDataRegister() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));
        bus.writeByte(0x2000, 0x12);
        bus.writeByte(0x2002, 0x34);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(1, 0x1FF0);
        cpu.registers().setData(0, 0xABCD_5678);
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new MovepOp().execute(cpu, bus, decoded(Size.WORD,
            EffectiveAddress.addrRegIndDisp(1, 0x0010), EffectiveAddress.dataReg(0)));

        assertAll(
            () -> assertEquals(Movep.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0xABCD_1234, cpu.registers().data(0)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void writesSpacedLongBytesFromDataRegisterToMemory() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x100));

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(1, 0x1FF0);
        cpu.registers().setData(0, 0x1234_5678);
        cpu.statusRegister().setRawValue(0xA71F);

        int cycles = new MovepOp().execute(cpu, bus, decoded(Size.LONG,
            EffectiveAddress.dataReg(0), EffectiveAddress.addrRegIndDisp(1, 0x0010)));

        assertAll(
            () -> assertEquals(Movep.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x12, bus.readByte(0x2000)),
            () -> assertEquals(0x34, bus.readByte(0x2002)),
            () -> assertEquals(0x56, bus.readByte(0x2004)),
            () -> assertEquals(0x78, bus.readByte(0x2006)),
            () -> assertEquals(0xA71F, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void rejectsInvalidOperandShapes() {
        M68kCpu cpu = new M68kCpu();
        AddressSpace bus = new AddressSpace();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new MovepOp().execute(cpu, bus, decoded(Size.WORD,
                EffectiveAddress.dataReg(0), EffectiveAddress.addrRegInd(1)))
        );

        assertEquals(
            "MOVEP requires one data register and one (d16,An) operand but was src=None[] dst=AddrRegInd[reg=1]",
            thrown.getMessage()
        );
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.MOVEP, size, src, dst, 0, 0x1004);
    }
}
