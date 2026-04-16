package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Registers;
import com.JMPE.cpu.m68k.Size;

import java.util.Objects;

/**
 * Resolves {@link EffectiveAddress} descriptors into runtime operand
 * reads, writes, and read–modify–write locations.
 *
 * <p>
 * The decoder produces pure descriptors ("this operand is at (A0)+").
 * Instruction helpers expect functional interfaces — lambdas that read
 * or write a value. {@code OperandResolver} bridges the gap:
 * </p>
 * <pre>
 *   int value = OperandResolver.read(decoded.src(), cpu, bus, size);
 *   OperandResolver.write(decoded.dst(), cpu, bus, size, result);
 * </pre>
 *
 * <h2>Side-effect timing</h2>
 * <ul>
 *   <li>{@code -(An)} pre-decrement: applied before the address is used.</li>
 *   <li>{@code (An)+} post-increment: applied after the value is read or written.</li>
 *   <li>{@link #resolveLocation} applies pre-decrement at construction and
 *       post-increment on the <em>first</em> call to
 *       {@link Location#write(int)}, making it safe for read–modify–write
 *       patterns where the destination is read then written.</li>
 * </ul>
 *
 * <h2>A7 byte-alignment rule</h2>
 * When the stack pointer (A7) is used with {@code (A7)+} or {@code -(A7)}
 * at byte size, the increment/decrement is 2 (not 1) to keep the stack
 * word-aligned. This is a 68000 hardware rule applied transparently here.
 *
 * <h2>Register write semantics</h2>
 * <ul>
 *   <li><b>Data registers (Dn):</b> BYTE/WORD writes preserve the upper bits
 *       of the register; LONG writes replace the whole 32-bit value.</li>
 *   <li><b>Address registers (An):</b> always receive the full 32-bit value.
 *       WORD operands are sign-extended to 32 bits before writing.</li>
 *   <li><b>Memory:</b> only the sized bytes are written to the bus.</li>
 * </ul>
 */
public final class OperandResolver {
    private OperandResolver() {}

    // -------------------------------------------------------------------------
    // read — source-only operands
    // -------------------------------------------------------------------------

    /**
     * Reads a sized value from the given effective address.
     * Applies all side-effects (pre-decrement, post-increment).
     */
    public static int read(EffectiveAddress ea, M68kCpu cpu, Bus bus, Size size) {
        Objects.requireNonNull(ea, "ea must not be null");
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(size, "size must not be null");

        Registers regs = cpu.registers();

        return switch (ea) {
            case EffectiveAddress.DataReg(int reg) ->
                    size.mask(regs.data(reg));

            case EffectiveAddress.AddrReg(int reg) ->
                    size.mask(regs.address(reg));

            case EffectiveAddress.AddrRegInd(int reg) ->
                    busRead(bus, regs.address(reg), size);

            case EffectiveAddress.AddrRegIndPostInc(int reg) -> {
                int addr = regs.address(reg);
                regs.setAddress(reg, addr + increment(reg, size));
                yield busRead(bus, addr, size);
            }

            case EffectiveAddress.AddrRegIndPreDec(int reg) -> {
                int addr = regs.address(reg) - increment(reg, size);
                regs.setAddress(reg, addr);
                yield busRead(bus, addr, size);
            }

            case EffectiveAddress.AddrRegIndDisp(int reg, int d16) ->
                    busRead(bus, regs.address(reg) + d16, size);

            case EffectiveAddress.AddrRegIndIndex ai ->
                    busRead(bus, indexedAddress(regs, ai.baseReg(), ai.d8(),
                            ai.indexIsAddrReg(), ai.indexRegNum(), ai.indexIsLong()), size);

            case EffectiveAddress.AbsoluteShort(int address) ->
                    busRead(bus, (short) address, size);

            case EffectiveAddress.AbsoluteLong(int address) ->
                    busRead(bus, address, size);

            case EffectiveAddress.PcRelativeDisp(int d16, int basePC) ->
                    busRead(bus, basePC + d16, size);

            case EffectiveAddress.PcRelativeIndex pi ->
                    busRead(bus, pcIndexedAddress(regs, pi), size);

            case EffectiveAddress.Immediate(int value) ->
                    value;

            case EffectiveAddress.Ccr() ->
                    cpu.statusRegister().conditionCodeRegister();

            case EffectiveAddress.Sr() ->
                    cpu.statusRegister().rawValue();

            case EffectiveAddress.None() ->
                    throw new IllegalArgumentException("Cannot read from EffectiveAddress.None");
        };
    }

    // -------------------------------------------------------------------------
    // write — destination-only operands
    // -------------------------------------------------------------------------

    /**
     * Writes a sized value to the given effective address.
     * Applies all side-effects (pre-decrement, post-increment).
     */
    public static void write(EffectiveAddress ea, M68kCpu cpu, Bus bus, Size size, int value) {
        Objects.requireNonNull(ea, "ea must not be null");
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(size, "size must not be null");

        Registers regs = cpu.registers();

        switch (ea) {
            case EffectiveAddress.DataReg(int reg) ->
                    DataRegisterWriter.write(cpu, reg, size, value);

            case EffectiveAddress.AddrReg(int reg) ->
                    regs.setAddress(reg, size == Size.WORD ? (short) value : value);

            case EffectiveAddress.AddrRegInd(int reg) ->
                    busWrite(bus, regs.address(reg), size, value);

            case EffectiveAddress.AddrRegIndPostInc(int reg) -> {
                int addr = regs.address(reg);
                regs.setAddress(reg, addr + increment(reg, size));
                busWrite(bus, addr, size, value);
            }

            case EffectiveAddress.AddrRegIndPreDec(int reg) -> {
                int addr = regs.address(reg) - increment(reg, size);
                regs.setAddress(reg, addr);
                busWrite(bus, addr, size, value);
            }

            case EffectiveAddress.AddrRegIndDisp(int reg, int d16) ->
                    busWrite(bus, regs.address(reg) + d16, size, value);

            case EffectiveAddress.AddrRegIndIndex ai ->
                    busWrite(bus, indexedAddress(regs, ai.baseReg(), ai.d8(),
                            ai.indexIsAddrReg(), ai.indexRegNum(), ai.indexIsLong()), size, value);

            case EffectiveAddress.AbsoluteShort(int address) ->
                    busWrite(bus, (short) address, size, value);

            case EffectiveAddress.AbsoluteLong(int address) ->
                    busWrite(bus, address, size, value);

            case EffectiveAddress.Ccr() ->
                    cpu.statusRegister().setConditionCodeRegister(Size.BYTE.mask(value));

            case EffectiveAddress.Sr() ->
                    cpu.statusRegister().setRawValue(Size.WORD.mask(value));

            case EffectiveAddress.PcRelativeDisp d ->
                    throw new IllegalArgumentException("Cannot write to PC-relative address");
            case EffectiveAddress.PcRelativeIndex i ->
                    throw new IllegalArgumentException("Cannot write to PC-relative address");
            case EffectiveAddress.Immediate i ->
                    throw new IllegalArgumentException("Cannot write to immediate operand");
            case EffectiveAddress.None() ->
                    throw new IllegalArgumentException("Cannot write to EffectiveAddress.None");
        }
    }

    // -------------------------------------------------------------------------
    // resolveLocation — read–modify–write destinations
    // -------------------------------------------------------------------------

    /**
     * Resolves an effective address into a {@link Location} that supports
     * both reading and writing with correct side-effect timing.
     *
     * <p>Pre-decrement ({@code -(An)}) is applied immediately.
     * Post-increment ({@code (An)+}) is deferred until
     * {@link Location#write(int)} is called.</p>
     */
    public static Location resolveLocation(EffectiveAddress ea, M68kCpu cpu, Bus bus, Size size) {
        Objects.requireNonNull(ea, "ea must not be null");
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(size, "size must not be null");

        Registers regs = cpu.registers();

        return switch (ea) {
            case EffectiveAddress.DataReg(int reg) -> new Location() {
                @Override public int read() { return size.mask(regs.data(reg)); }
                @Override public void write(int value) { DataRegisterWriter.write(cpu, reg, size, value); }
            };

            case EffectiveAddress.AddrReg(int reg) -> new Location() {
                @Override public int read() { return size.mask(regs.address(reg)); }
                @Override public void write(int value) {
                    regs.setAddress(reg, size == Size.WORD ? (short) value : value);
                }
            };

            case EffectiveAddress.AddrRegInd(int reg) -> {
                int addr = regs.address(reg);
                yield memoryLocation(bus, addr, size);
            }

            case EffectiveAddress.AddrRegIndPostInc(int reg) -> {
                int addr = regs.address(reg);
                int inc = increment(reg, size);
                regs.setAddress(reg, addr + inc);
                yield new Location() {
                    @Override public int read() { return busRead(bus, addr, size); }
                    @Override public void write(int value) {
                        busWrite(bus, addr, size, value);
                    }
                };
            }

            case EffectiveAddress.AddrRegIndPreDec(int reg) -> {
                int addr = regs.address(reg) - increment(reg, size);
                regs.setAddress(reg, addr);
                yield memoryLocation(bus, addr, size);
            }

            case EffectiveAddress.AddrRegIndDisp(int reg, int d16) -> {
                int addr = regs.address(reg) + d16;
                yield memoryLocation(bus, addr, size);
            }

            case EffectiveAddress.AddrRegIndIndex ai -> {
                int addr = indexedAddress(regs, ai.baseReg(), ai.d8(),
                        ai.indexIsAddrReg(), ai.indexRegNum(), ai.indexIsLong());
                yield memoryLocation(bus, addr, size);
            }

            case EffectiveAddress.AbsoluteShort(int address) ->
                    memoryLocation(bus, (short) address, size);

            case EffectiveAddress.AbsoluteLong(int address) ->
                    memoryLocation(bus, address, size);

            default ->
                    throw new IllegalArgumentException(
                            "Cannot resolve read–write location for " + ea);
        };
    }

    // -------------------------------------------------------------------------
    // computeAddress — for LEA, PEA, JMP, JSR
    // -------------------------------------------------------------------------

    /**
     * Computes the effective address without reading or writing.
     * Only valid for memory-addressable modes (no registers, no immediate).
     */
    public static int computeAddress(EffectiveAddress ea, M68kCpu cpu) {
        Objects.requireNonNull(ea, "ea must not be null");
        Objects.requireNonNull(cpu, "cpu must not be null");

        Registers regs = cpu.registers();

        return switch (ea) {
            case EffectiveAddress.AddrRegInd(int reg) ->
                    regs.address(reg);

            case EffectiveAddress.AddrRegIndDisp(int reg, int d16) ->
                    regs.address(reg) + d16;

            case EffectiveAddress.AddrRegIndIndex ai ->
                    indexedAddress(regs, ai.baseReg(), ai.d8(),
                            ai.indexIsAddrReg(), ai.indexRegNum(), ai.indexIsLong());

            case EffectiveAddress.AbsoluteShort(int address) ->
                    (short) address;

            case EffectiveAddress.AbsoluteLong(int address) ->
                    address;

            case EffectiveAddress.PcRelativeDisp(int d16, int basePC) ->
                    basePC + d16;

            case EffectiveAddress.PcRelativeIndex pi ->
                    pcIndexedAddress(regs, pi);

            default ->
                    throw new IllegalArgumentException(
                            "Cannot compute address for " + ea);
        };
    }

    // -------------------------------------------------------------------------
    // Location — read–modify–write accessor
    // -------------------------------------------------------------------------

    /**
     * A resolved operand location that supports both reading and writing.
     * Used for read–modify–write instructions (AND, OR, ADD, SUB, NOT, NEG, etc.).
     */
    public interface Location {
        /** Reads the current value at this location. */
        int read();

        /** Writes a new value to this location, applying any deferred side-effects. */
        void write(int value);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * A7 (stack pointer) byte operations use increment/decrement of 2
     * to keep the stack word-aligned. All other cases use size.bytes().
     */
    static int increment(int reg, Size size) {
        return (reg == Registers.STACK_POINTER_REGISTER && size == Size.BYTE) ? 2 : size.bytes();
    }

    private static int busRead(Bus bus, int address, Size size) {
        return switch (size) {
            case BYTE -> bus.readByte(address);
            case WORD -> bus.readWord(address);
            case LONG -> bus.readLong(address);
            case UNSIZED -> throw new IllegalStateException("UNSIZED bus read");
        };
    }

    private static void busWrite(Bus bus, int address, Size size, int value) {
        switch (size) {
            case BYTE -> bus.writeByte(address, value);
            case WORD -> bus.writeWord(address, value);
            case LONG -> bus.writeLong(address, value);
            case UNSIZED -> throw new IllegalStateException("UNSIZED bus write");
        }
    }

    private static int indexedAddress(Registers regs, int baseReg, int d8,
                                      boolean indexIsAddrReg, int indexRegNum, boolean indexIsLong) {
        int base = regs.address(baseReg);
        int index = indexIsAddrReg ? regs.address(indexRegNum) : regs.data(indexRegNum);
        if (!indexIsLong) {
            index = (short) index; // sign-extend low 16 bits
        }
        return base + index + d8;
    }

    private static int pcIndexedAddress(Registers regs, EffectiveAddress.PcRelativeIndex pi) {
        int index = pi.indexIsAddrReg() ? regs.address(pi.indexRegNum()) : regs.data(pi.indexRegNum());
        if (!pi.indexIsLong()) {
            index = (short) index;
        }
        return pi.basePC() + index + pi.d8();
    }

    private static Location memoryLocation(Bus bus, int address, Size size) {
        return new Location() {
            @Override public int read() { return busRead(bus, address, size); }
            @Override public void write(int value) { busWrite(bus, address, size, value); }
        };
    }
}
