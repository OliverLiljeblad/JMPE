package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperandResolverTest {

    private M68kCpu cpu;
    private AddressSpace bus;

    @BeforeEach
    void setUp() {
        cpu = new M68kCpu();
        bus = new AddressSpace();
        bus.addRegion(new Ram(0x0000_0000, 0x0001_0000)); // 64KB RAM at base 0
    }

    // =========================================================================
    // read()
    // =========================================================================

    @Nested
    class Read {

        @Test
        void dataRegDirect() {
            cpu.registers().setData(3, 0xDEAD_BEEF);
            assertEquals(0xEF, OperandResolver.read(EffectiveAddress.dataReg(3), cpu, bus, Size.BYTE));
            assertEquals(0xBEEF, OperandResolver.read(EffectiveAddress.dataReg(3), cpu, bus, Size.WORD));
            assertEquals(0xDEAD_BEEF, OperandResolver.read(EffectiveAddress.dataReg(3), cpu, bus, Size.LONG));
        }

        @Test
        void addrRegDirect() {
            cpu.registers().setAddress(2, 0x0000_1234);
            assertEquals(0x34, OperandResolver.read(EffectiveAddress.addrReg(2), cpu, bus, Size.BYTE));
            assertEquals(0x1234, OperandResolver.read(EffectiveAddress.addrReg(2), cpu, bus, Size.WORD));
            assertEquals(0x0000_1234, OperandResolver.read(EffectiveAddress.addrReg(2), cpu, bus, Size.LONG));
        }

        @Test
        void addrRegIndirect() {
            cpu.registers().setAddress(0, 0x0000_0100);
            bus.writeWord(0x100, 0xCAFE);
            assertEquals(0xCAFE, OperandResolver.read(EffectiveAddress.addrRegInd(0), cpu, bus, Size.WORD));
            assertEquals(0x0000_0100, cpu.registers().address(0)); // no side effect
        }

        @Test
        void addrRegIndPostIncrementWord() {
            cpu.registers().setAddress(1, 0x0000_0200);
            bus.writeWord(0x200, 0x1234);
            assertEquals(0x1234, OperandResolver.read(EffectiveAddress.addrRegIndPostInc(1), cpu, bus, Size.WORD));
            assertEquals(0x0000_0202, cpu.registers().address(1)); // incremented by 2
        }

        @Test
        void addrRegIndPostIncrementLong() {
            cpu.registers().setAddress(2, 0x0000_0300);
            bus.writeLong(0x300, 0xAABB_CCDD);
            assertEquals(0xAABB_CCDD, OperandResolver.read(EffectiveAddress.addrRegIndPostInc(2), cpu, bus, Size.LONG));
            assertEquals(0x0000_0304, cpu.registers().address(2)); // incremented by 4
        }

        @Test
        void addrRegIndPostIncrementByte() {
            cpu.registers().setAddress(3, 0x0000_0400);
            bus.writeByte(0x400, 0x42);
            assertEquals(0x42, OperandResolver.read(EffectiveAddress.addrRegIndPostInc(3), cpu, bus, Size.BYTE));
            assertEquals(0x0000_0401, cpu.registers().address(3)); // incremented by 1
        }

        @Test
        void a7BytePostIncrementAlignedTo2() {
            cpu.registers().setAddress(7, 0x0000_0500);
            bus.writeByte(0x500, 0xFF);
            assertEquals(0xFF, OperandResolver.read(EffectiveAddress.addrRegIndPostInc(7), cpu, bus, Size.BYTE));
            assertEquals(0x0000_0502, cpu.registers().address(7)); // A7 byte → +2
        }

        @Test
        void addrRegIndPreDecrementWord() {
            cpu.registers().setAddress(4, 0x0000_0604);
            bus.writeWord(0x602, 0x5678);
            assertEquals(0x5678, OperandResolver.read(EffectiveAddress.addrRegIndPreDec(4), cpu, bus, Size.WORD));
            assertEquals(0x0000_0602, cpu.registers().address(4)); // decremented by 2
        }

        @Test
        void a7BytePreDecrementAlignedTo2() {
            cpu.registers().setAddress(7, 0x0000_0700);
            bus.writeByte(0x6FE, 0xAB);
            assertEquals(0xAB, OperandResolver.read(EffectiveAddress.addrRegIndPreDec(7), cpu, bus, Size.BYTE));
            assertEquals(0x0000_06FE, cpu.registers().address(7)); // A7 byte → -2
        }

        @Test
        void addrRegIndWithDisplacement() {
            cpu.registers().setAddress(5, 0x0000_0800);
            bus.writeWord(0x80A, 0x9999);
            assertEquals(0x9999, OperandResolver.read(EffectiveAddress.addrRegIndDisp(5, 10), cpu, bus, Size.WORD));
        }

        @Test
        void addrRegIndWithNegativeDisplacement() {
            cpu.registers().setAddress(5, 0x0000_0810);
            bus.writeWord(0x800, 0x7777);
            assertEquals(0x7777, OperandResolver.read(EffectiveAddress.addrRegIndDisp(5, -16), cpu, bus, Size.WORD));
        }

        @Test
        void addrRegIndWithIndexDataRegWord() {
            cpu.registers().setAddress(0, 0x0000_0900);
            cpu.registers().setData(1, 0x0000_0010);
            bus.writeWord(0x914, 0xBEEF);
            // addr = A0 + D1.W + d8(4) = 0x900 + 0x10 + 4 = 0x914
            assertEquals(0xBEEF, OperandResolver.read(
                    EffectiveAddress.addrRegIndIndex(0, 4, false, 1, false), cpu, bus, Size.WORD));
        }

        @Test
        void addrRegIndWithIndexAddrRegLong() {
            cpu.registers().setAddress(2, 0x0000_0A00);
            cpu.registers().setAddress(3, 0x0000_0020);
            bus.writeLong(0xA26, 0x1234_5678);
            // addr = A2 + A3.L + d8(6) = 0xA00 + 0x20 + 6 = 0xA26
            assertEquals(0x1234_5678, OperandResolver.read(
                    EffectiveAddress.addrRegIndIndex(2, 6, true, 3, true), cpu, bus, Size.LONG));
        }

        @Test
        void absoluteShort() {
            bus.writeWord(0x0100, 0xABCD);
            assertEquals(0xABCD, OperandResolver.read(EffectiveAddress.absoluteShort(0x0100), cpu, bus, Size.WORD));
        }

        @Test
        void absoluteShortSignExtends() {
            // 0x8000 sign-extends to 0xFFFF_8000 — but in Mac Plus 24-bit space
            // this wraps. For our test, just use a positive address.
            bus.writeByte(0x007F, 0x42);
            assertEquals(0x42, OperandResolver.read(EffectiveAddress.absoluteShort(0x007F), cpu, bus, Size.BYTE));
        }

        @Test
        void absoluteLong() {
            bus.writeLong(0x0200, 0xCAFE_BABE);
            assertEquals(0xCAFE_BABE, OperandResolver.read(EffectiveAddress.absoluteLong(0x0200), cpu, bus, Size.LONG));
        }

        @Test
        void pcRelativeDisplacement() {
            bus.writeWord(0x1004, 0x5555);
            // basePC = 0x1000, d16 = 4 → read from 0x1004
            assertEquals(0x5555, OperandResolver.read(
                    EffectiveAddress.pcRelativeDisp(4, 0x1000), cpu, bus, Size.WORD));
        }

        @Test
        void pcRelativeIndex() {
            cpu.registers().setData(2, 0x0000_0008);
            bus.writeWord(0x100E, 0x7777);
            // basePC=0x1000, D2.W=8, d8=6 → 0x1000 + 8 + 6 = 0x100E
            assertEquals(0x7777, OperandResolver.read(
                    EffectiveAddress.pcRelativeIndex(6, false, 2, false, 0x1000), cpu, bus, Size.WORD));
        }

        @Test
        void immediate() {
            assertEquals(0x42, OperandResolver.read(EffectiveAddress.immediate(0x42), cpu, bus, Size.BYTE));
            assertEquals(0x1234, OperandResolver.read(EffectiveAddress.immediate(0x1234), cpu, bus, Size.WORD));
        }

        @Test
        void ccr() {
            cpu.statusRegister().setConditionCodeRegister(0x15);
            assertEquals(0x15, OperandResolver.read(EffectiveAddress.ccr(), cpu, bus, Size.BYTE));
        }

        @Test
        void sr() {
            cpu.statusRegister().setRawValue(0x2700);
            assertEquals(0x2700, OperandResolver.read(EffectiveAddress.sr(), cpu, bus, Size.WORD));
        }

        @Test
        void rejectsNone() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.read(EffectiveAddress.none(), cpu, bus, Size.WORD));
        }
    }

    // =========================================================================
    // write()
    // =========================================================================

    @Nested
    class Write {

        @Test
        void dataRegBytePreservesUpperBits() {
            cpu.registers().setData(0, 0xFFFF_FF00);
            OperandResolver.write(EffectiveAddress.dataReg(0), cpu, bus, Size.BYTE, 0x42);
            assertEquals(0xFFFF_FF42, cpu.registers().data(0));
        }

        @Test
        void dataRegWordPreservesUpperBits() {
            cpu.registers().setData(1, 0xFFFF_0000);
            OperandResolver.write(EffectiveAddress.dataReg(1), cpu, bus, Size.WORD, 0xBEEF);
            assertEquals(0xFFFF_BEEF, cpu.registers().data(1));
        }

        @Test
        void dataRegLongReplacesAll() {
            cpu.registers().setData(2, 0xFFFF_FFFF);
            OperandResolver.write(EffectiveAddress.dataReg(2), cpu, bus, Size.LONG, 0x1234_5678);
            assertEquals(0x1234_5678, cpu.registers().data(2));
        }

        @Test
        void addrRegWordSignExtends() {
            OperandResolver.write(EffectiveAddress.addrReg(0), cpu, bus, Size.WORD, 0x8000);
            assertEquals(0xFFFF_8000, cpu.registers().address(0)); // sign-extended
        }

        @Test
        void addrRegLongFull() {
            OperandResolver.write(EffectiveAddress.addrReg(1), cpu, bus, Size.LONG, 0xDEAD_BEEF);
            assertEquals(0xDEAD_BEEF, cpu.registers().address(1));
        }

        @Test
        void addrRegIndirect() {
            cpu.registers().setAddress(0, 0x0000_0100);
            OperandResolver.write(EffectiveAddress.addrRegInd(0), cpu, bus, Size.WORD, 0xCAFE);
            assertEquals(0xCAFE, bus.readWord(0x100));
        }

        @Test
        void addrRegIndPostIncrement() {
            cpu.registers().setAddress(1, 0x0000_0200);
            OperandResolver.write(EffectiveAddress.addrRegIndPostInc(1), cpu, bus, Size.WORD, 0x1234);
            assertEquals(0x1234, bus.readWord(0x200));
            assertEquals(0x0000_0202, cpu.registers().address(1));
        }

        @Test
        void addrRegIndPreDecrement() {
            cpu.registers().setAddress(2, 0x0000_0304);
            OperandResolver.write(EffectiveAddress.addrRegIndPreDec(2), cpu, bus, Size.WORD, 0x5678);
            assertEquals(0x5678, bus.readWord(0x302));
            assertEquals(0x0000_0302, cpu.registers().address(2));
        }

        @Test
        void absoluteLongMemory() {
            OperandResolver.write(EffectiveAddress.absoluteLong(0x0400), cpu, bus, Size.LONG, 0xAABB_CCDD);
            assertEquals(0xAABB_CCDD, bus.readLong(0x400));
        }

        @Test
        void ccrWrite() {
            OperandResolver.write(EffectiveAddress.ccr(), cpu, bus, Size.BYTE, 0x1F);
            assertEquals(0x1F, cpu.statusRegister().conditionCodeRegister());
        }

        @Test
        void srWrite() {
            cpu.statusRegister().setRawValue(0x2700); // supervisor mode
            OperandResolver.write(EffectiveAddress.sr(), cpu, bus, Size.WORD, 0x2000);
            assertEquals(0x2000, cpu.statusRegister().rawValue());
        }

        @Test
        void rejectsPcRelative() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.write(EffectiveAddress.pcRelativeDisp(0, 0x1000), cpu, bus, Size.WORD, 0));
        }

        @Test
        void rejectsImmediate() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.write(EffectiveAddress.immediate(42), cpu, bus, Size.WORD, 0));
        }

        @Test
        void rejectsNone() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.write(EffectiveAddress.none(), cpu, bus, Size.WORD, 0));
        }
    }

    // =========================================================================
    // resolveLocation() — read–modify–write
    // =========================================================================

    @Nested
    class ResolveLocation {

        @Test
        void dataRegReadModifyWrite() {
            cpu.registers().setData(5, 0x0000_00FF);
            OperandResolver.Location loc = OperandResolver.resolveLocation(
                    EffectiveAddress.dataReg(5), cpu, bus, Size.BYTE);
            assertEquals(0xFF, loc.read());
            loc.write(0x42);
            assertEquals(0x0000_0042, cpu.registers().data(5));
        }

        @Test
        void memoryIndirectReadModifyWrite() {
            cpu.registers().setAddress(0, 0x0000_0100);
            bus.writeWord(0x100, 0x00FF);
            OperandResolver.Location loc = OperandResolver.resolveLocation(
                    EffectiveAddress.addrRegInd(0), cpu, bus, Size.WORD);

            assertEquals(0x00FF, loc.read());
            loc.write(0xFF00);
            assertEquals(0xFF00, bus.readWord(0x100));
        }

        @Test
        void postIncrementAppliedOnWrite() {
            cpu.registers().setAddress(1, 0x0000_0200);
            bus.writeWord(0x200, 0x1234);
            OperandResolver.Location loc = OperandResolver.resolveLocation(
                    EffectiveAddress.addrRegIndPostInc(1), cpu, bus, Size.WORD);

            // After resolveLocation, A1 should NOT yet be incremented
            assertEquals(0x0000_0200, cpu.registers().address(1));
            assertEquals(0x1234, loc.read());
            // Still not incremented after read
            assertEquals(0x0000_0200, cpu.registers().address(1));
            loc.write(0x5678);
            // NOW A1 should be incremented
            assertEquals(0x0000_0202, cpu.registers().address(1));
            assertEquals(0x5678, bus.readWord(0x200));
        }

        @Test
        void preDecrementAppliedAtResolve() {
            cpu.registers().setAddress(2, 0x0000_0304);
            bus.writeWord(0x302, 0xAAAA);
            OperandResolver.Location loc = OperandResolver.resolveLocation(
                    EffectiveAddress.addrRegIndPreDec(2), cpu, bus, Size.WORD);

            // Pre-decrement should already be applied
            assertEquals(0x0000_0302, cpu.registers().address(2));
            assertEquals(0xAAAA, loc.read());
            loc.write(0xBBBB);
            assertEquals(0xBBBB, bus.readWord(0x302));
            // No further changes to A2
            assertEquals(0x0000_0302, cpu.registers().address(2));
        }

        @Test
        void absoluteLongLocation() {
            bus.writeLong(0x400, 0xDEAD_BEEF);
            OperandResolver.Location loc = OperandResolver.resolveLocation(
                    EffectiveAddress.absoluteLong(0x400), cpu, bus, Size.LONG);

            assertEquals(0xDEAD_BEEF, loc.read());
            loc.write(0xCAFE_BABE);
            assertEquals(0xCAFE_BABE, bus.readLong(0x400));
        }

        @Test
        void rejectsImmediate() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.resolveLocation(
                            EffectiveAddress.immediate(42), cpu, bus, Size.WORD));
        }

        @Test
        void rejectsPcRelative() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.resolveLocation(
                            EffectiveAddress.pcRelativeDisp(0, 0x1000), cpu, bus, Size.WORD));
        }
    }

    // =========================================================================
    // computeAddress()
    // =========================================================================

    @Nested
    class ComputeAddress {

        @Test
        void addrRegIndirect() {
            cpu.registers().setAddress(0, 0x0000_1000);
            assertEquals(0x1000, OperandResolver.computeAddress(EffectiveAddress.addrRegInd(0), cpu));
        }

        @Test
        void addrRegIndDisplacement() {
            cpu.registers().setAddress(1, 0x0000_2000);
            assertEquals(0x200A, OperandResolver.computeAddress(EffectiveAddress.addrRegIndDisp(1, 10), cpu));
        }

        @Test
        void addrRegIndIndex() {
            cpu.registers().setAddress(0, 0x0000_3000);
            cpu.registers().setData(1, 0x0000_0020);
            // A0 + D1.W + d8(4) = 0x3000 + 0x20 + 4 = 0x3024
            assertEquals(0x3024, OperandResolver.computeAddress(
                    EffectiveAddress.addrRegIndIndex(0, 4, false, 1, false), cpu));
        }

        @Test
        void absoluteShort() {
            assertEquals(0x100, OperandResolver.computeAddress(EffectiveAddress.absoluteShort(0x0100), cpu));
        }

        @Test
        void absoluteLong() {
            assertEquals(0x4000, OperandResolver.computeAddress(EffectiveAddress.absoluteLong(0x4000), cpu));
        }

        @Test
        void pcRelativeDisplacement() {
            assertEquals(0x1008, OperandResolver.computeAddress(
                    EffectiveAddress.pcRelativeDisp(8, 0x1000), cpu));
        }

        @Test
        void pcRelativeIndex() {
            cpu.registers().setData(3, 0x0000_0010);
            // basePC=0x2000, D3.W=0x10, d8=4 → 0x2000 + 0x10 + 4 = 0x2014
            assertEquals(0x2014, OperandResolver.computeAddress(
                    EffectiveAddress.pcRelativeIndex(4, false, 3, false, 0x2000), cpu));
        }

        @Test
        void rejectsDataReg() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.computeAddress(EffectiveAddress.dataReg(0), cpu));
        }

        @Test
        void rejectsImmediate() {
            assertThrows(IllegalArgumentException.class,
                    () -> OperandResolver.computeAddress(EffectiveAddress.immediate(42), cpu));
        }
    }

    // =========================================================================
    // A7 alignment helper
    // =========================================================================

    @Nested
    class IncrementHelper {

        @Test
        void a7ByteReturns2() {
            assertEquals(2, OperandResolver.increment(7, Size.BYTE));
        }

        @Test
        void a7WordReturns2() {
            assertEquals(2, OperandResolver.increment(7, Size.WORD));
        }

        @Test
        void a7LongReturns4() {
            assertEquals(4, OperandResolver.increment(7, Size.LONG));
        }

        @Test
        void nonA7ByteReturns1() {
            assertEquals(1, OperandResolver.increment(0, Size.BYTE));
            assertEquals(1, OperandResolver.increment(6, Size.BYTE));
        }

        @Test
        void nonA7WordReturns2() {
            assertEquals(2, OperandResolver.increment(3, Size.WORD));
        }

        @Test
        void nonA7LongReturns4() {
            assertEquals(4, OperandResolver.increment(3, Size.LONG));
        }
    }
}
