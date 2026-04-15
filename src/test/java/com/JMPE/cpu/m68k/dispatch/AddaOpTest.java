package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Adda;
import org.junit.jupiter.api.Test;

class AddaOpTest {
    @Test
    void addsSignExtendedWordSourceIntoAddressRegisterWithoutTouchingCcr() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setData(0, 0x0000_FFFF);
        cpu.registers().setAddress(1, 0x0000_1000);
        cpu.statusRegister().setRawValue(0x2715);

        int cycles = new AddaOp().execute(cpu, null, decoded(Size.WORD,
            EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(1)));

        assertAll(
            () -> assertEquals(Adda.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_0FFF, cpu.registers().address(1)),
            () -> assertEquals(0x2715, cpu.statusRegister().rawValue())
        );
    }

    @Test
    void addsLongMemorySourceIntoAddressRegister() {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x2000, 0x1000));
        bus.writeLong(0x2000, 0x0000_0100);

        M68kCpu cpu = new M68kCpu();
        cpu.registers().setAddress(0, 0x0000_2200);

        int cycles = new AddaOp().execute(cpu, bus, decoded(Size.LONG,
            EffectiveAddress.absoluteLong(0x2000), EffectiveAddress.addrReg(0)));

        assertAll(
            () -> assertEquals(Adda.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x0000_2300, cpu.registers().address(0))
        );
    }

    private static DecodedInstruction decoded(Size size, EffectiveAddress src, EffectiveAddress dst) {
        return new DecodedInstruction(Opcode.ADDA, size, src, dst, 0, 0x1002);
    }
}
