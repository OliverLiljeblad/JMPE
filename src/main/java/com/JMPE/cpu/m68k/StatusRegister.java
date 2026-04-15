package com.JMPE.cpu.m68k;

import com.JMPE.cpu.m68k.instructions.arithmetic.Add;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addq;
import com.JMPE.cpu.m68k.instructions.arithmetic.Abcd;
import com.JMPE.cpu.m68k.instructions.arithmetic.Nbcd;
import com.JMPE.cpu.m68k.instructions.arithmetic.Sbcd;
import com.JMPE.cpu.m68k.instructions.ConditionCodes;
import com.JMPE.cpu.m68k.instructions.arithmetic.Sub;
import com.JMPE.cpu.m68k.instructions.arithmetic.Subq;
import com.JMPE.cpu.m68k.instructions.shift.Asl;
import com.JMPE.cpu.m68k.instructions.shift.Asr;
import com.JMPE.cpu.m68k.instructions.shift.Lsl;
import com.JMPE.cpu.m68k.instructions.shift.Lsr;
import com.JMPE.cpu.m68k.instructions.shift.Roxl;
import com.JMPE.cpu.m68k.instructions.shift.Roxr;

/**
 * Motorola 68000 status register model.
 * <p>
 * This class focuses on stable bit helpers and arithmetic-flag updates so instruction
 * implementations can reuse one precise source of truth.
 */
public final class StatusRegister {
    private static final int CARRY_BIT = 0;
    private static final int OVERFLOW_BIT = 1;
    private static final int ZERO_BIT = 2;
    private static final int NEGATIVE_BIT = 3;
    private static final int EXTEND_BIT = 4;
    private static final int INTERRUPT_MASK_SHIFT = 8;
    private static final int INTERRUPT_MASK_BITS = 0b111;
    private static final int SUPERVISOR_BIT = 13;
    private static final int TRACE_BIT = 15;

    private int value;
    private final ArithmeticConditionCodes arithmeticConditionCodes = new ArithmeticConditionCodes();
    private final MoveConditionCodes moveConditionCodes = new MoveConditionCodes();
    private final ShiftConditionCodes shiftConditionCodes = new ShiftConditionCodes();
    private SupervisorModeListener supervisorModeListener;

    public int rawValue() {
        return value & 0xFFFF;
    }

    public void setRawValue(int rawValue) {
        boolean previousSupervisor = isSupervisorSet();
        this.value = rawValue & 0xFFFF;
        notifySupervisorModeChange(previousSupervisor);
    }

    public int conditionCodeRegister() {
        return rawValue() & 0xFF;
    }

    public void setConditionCodeRegister(int ccr) {
        value = (rawValue() & 0xFF00) | (ccr & 0xFF);
    }

    public int interruptMask() {
        return (rawValue() >>> INTERRUPT_MASK_SHIFT) & INTERRUPT_MASK_BITS;
    }

    public void setInterruptMask(int level) {
        if (level < 0 || level > INTERRUPT_MASK_BITS) {
            throw new IllegalArgumentException("interrupt mask level must be in range 0..7");
        }
        value = (rawValue() & ~(INTERRUPT_MASK_BITS << INTERRUPT_MASK_SHIFT))
            | (level << INTERRUPT_MASK_SHIFT);
    }

    public boolean isCarrySet() {
        return isBitSet(CARRY_BIT);
    }

    public void setCarry(boolean set) {
        setBit(CARRY_BIT, set);
    }

    public boolean isOverflowSet() {
        return isBitSet(OVERFLOW_BIT);
    }

    public void setOverflow(boolean set) {
        setBit(OVERFLOW_BIT, set);
    }

    public boolean isZeroSet() {
        return isBitSet(ZERO_BIT);
    }

    public void setZero(boolean set) {
        setBit(ZERO_BIT, set);
    }

    public boolean isNegativeSet() {
        return isBitSet(NEGATIVE_BIT);
    }

    public void setNegative(boolean set) {
        setBit(NEGATIVE_BIT, set);
    }

    public boolean isExtendSet() {
        return isBitSet(EXTEND_BIT);
    }

    public void setExtend(boolean set) {
        setBit(EXTEND_BIT, set);
    }

    public boolean isSupervisorSet() {
        return isBitSet(SUPERVISOR_BIT);
    }

    public void setSupervisor(boolean set) {
        setBit(SUPERVISOR_BIT, set);
    }

    public boolean isTraceSet() {
        return isBitSet(TRACE_BIT);
    }

    public void setTrace(boolean set) {
        setBit(TRACE_BIT, set);
    }

    public void setSupervisorModeListener(SupervisorModeListener supervisorModeListener) {
        this.supervisorModeListener = supervisorModeListener;
    }

    /**
     * CCR adapter for MOVE/CMP-like helpers that only update N/Z/V/C.
     */
    public ConditionCodes moveConditionCodes() {
        return moveConditionCodes;
    }

    /**
     * CCR adapter for arithmetic helpers that must also set X equal to carry/borrow.
     */
    public Add.ConditionCodes addConditionCodes() {
        return arithmeticConditionCodes;
    }

    public Addq.ConditionCodes addqConditionCodes() {
        return arithmeticConditionCodes;
    }

    public Sub.ConditionCodes subConditionCodes() {
        return arithmeticConditionCodes;
    }

    public Subq.ConditionCodes subqConditionCodes() {
        return arithmeticConditionCodes;
    }

    public Asl.ConditionCodes aslConditionCodes() {
        return shiftConditionCodes;
    }

    public Asr.ConditionCodes asrConditionCodes() {
        return shiftConditionCodes;
    }

    public Lsl.ConditionCodes lslConditionCodes() {
        return shiftConditionCodes;
    }

    public Lsr.ConditionCodes lsrConditionCodes() {
        return shiftConditionCodes;
    }

    public Roxl.ConditionCodes roxlConditionCodes() {
        return shiftConditionCodes;
    }

    public Roxr.ConditionCodes roxrConditionCodes() {
        return shiftConditionCodes;
    }

    public Abcd.ConditionCodes abcdConditionCodes() {
        return bcdConditionCodes;
    }

    public Sbcd.ConditionCodes sbcdConditionCodes() {
        return bcdConditionCodes;
    }

    public Nbcd.ConditionCodes nbcdConditionCodes() {
        return bcdConditionCodes;
    }

    public void updateAddFlags(long source, long destination, long result, int bits) {
        long mask = maskFor(bits);
        long signBit = signBitFor(bits);
        long src = source & mask;
        long dst = destination & mask;
        long res = result & mask;

        boolean carry = (src + dst) > mask;
        boolean overflow = ((~(dst ^ src) & (dst ^ res)) & signBit) != 0;

        setCarry(carry);
        setExtend(carry);
        setOverflow(overflow);
        setZero(res == 0);
        setNegative((res & signBit) != 0);
    }

    public void updateSubFlags(long source, long destination, long result, int bits) {
        long mask = maskFor(bits);
        long signBit = signBitFor(bits);
        long src = source & mask;
        long dst = destination & mask;
        long res = result & mask;

        boolean borrow = dst < src;
        boolean overflow = (((dst ^ src) & (dst ^ res)) & signBit) != 0;

        setCarry(borrow);
        setExtend(borrow);
        setOverflow(overflow);
        setZero(res == 0);
        setNegative((res & signBit) != 0);
    }

    private boolean isBitSet(int bit) {
        return (rawValue() & (1 << bit)) != 0;
    }

    private void setBit(int bit, boolean set) {
        boolean previousSupervisor = isSupervisorSet();
        if (set) {
            value = rawValue() | (1 << bit);
            notifySupervisorModeChange(previousSupervisor);
            return;
        }
        value = rawValue() & ~(1 << bit);
        notifySupervisorModeChange(previousSupervisor);
    }

    private void notifySupervisorModeChange(boolean previousSupervisor) {
        if (supervisorModeListener != null && previousSupervisor != isSupervisorSet()) {
            supervisorModeListener.onChange(isSupervisorSet());
        }
    }

    private long maskFor(int bits) {
        return switch (bits) {
            case 8 -> 0xFFL;
            case 16 -> 0xFFFFL;
            case 32 -> 0xFFFF_FFFFL;
            default -> throw new IllegalArgumentException("Unsupported operand width: " + bits);
        };
    }

    private long signBitFor(int bits) {
        return switch (bits) {
            case 8 -> 0x80L;
            case 16 -> 0x8000L;
            case 32 -> 0x8000_0000L;
            default -> throw new IllegalArgumentException("Unsupported operand width: " + bits);
        };
    }

    private final class MoveConditionCodes implements ConditionCodes {
        @Override
        public void setNegative(boolean value) {
            StatusRegister.this.setNegative(value);
        }

        @Override
        public void setZero(boolean value) {
            StatusRegister.this.setZero(value);
        }

        @Override
        public void setOverflow(boolean value) {
            StatusRegister.this.setOverflow(value);
        }

        @Override
        public void setCarry(boolean value) {
            StatusRegister.this.setCarry(value);
        }
    }

    @FunctionalInterface
    public interface SupervisorModeListener {
        void onChange(boolean supervisor);
    }

    private final class ArithmeticConditionCodes
        implements Add.ConditionCodes, Addq.ConditionCodes, Sub.ConditionCodes, Subq.ConditionCodes {
        @Override
        public void setNegative(boolean value) {
            StatusRegister.this.setNegative(value);
        }

        @Override
        public void setZero(boolean value) {
            StatusRegister.this.setZero(value);
        }

        @Override
        public void setOverflow(boolean value) {
            StatusRegister.this.setOverflow(value);
        }

        @Override
        public void setCarry(boolean value) {
            StatusRegister.this.setCarry(value);
        }

        @Override
        public void setExtend(boolean value) {
            StatusRegister.this.setExtend(value);
        }
    }

    private final class ShiftConditionCodes
        implements Asl.ConditionCodes, Asr.ConditionCodes, Lsl.ConditionCodes, Lsr.ConditionCodes,
        Roxl.ConditionCodes, Roxr.ConditionCodes {
        @Override
        public void setNegative(boolean value) {
            StatusRegister.this.setNegative(value);
        }

        @Override
        public void setZero(boolean value) {
            StatusRegister.this.setZero(value);
        }

        @Override
        public void setOverflow(boolean value) {
            StatusRegister.this.setOverflow(value);
        }

        @Override
        public void setCarry(boolean value) {
            StatusRegister.this.setCarry(value);
        }

        @Override
        public void setExtend(boolean value) {
            StatusRegister.this.setExtend(value);
        }
    }

    private final class BcdConditionCodes
        implements Abcd.ConditionCodes, Sbcd.ConditionCodes, Nbcd.ConditionCodes {
        @Override
        public void clearZero() {
            StatusRegister.this.setZero(false);
        }

        @Override
        public void setCarry(boolean value) {
            StatusRegister.this.setCarry(value);
        }

        @Override
        public void setExtend(boolean value) {
            StatusRegister.this.setExtend(value);
        }
    }

    private final BcdConditionCodes bcdConditionCodes = new BcdConditionCodes();
}
