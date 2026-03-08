package com.JMPE.cpu.m68k;

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
    private static final int SUPERVISOR_BIT = 13;

    private int value;

    public int rawValue() {
        return value & 0xFFFF;
    }

    public void setRawValue(int rawValue) {
        this.value = rawValue & 0xFFFF;
    }

    public int conditionCodeRegister() {
        return rawValue() & 0xFF;
    }

    public void setConditionCodeRegister(int ccr) {
        value = (rawValue() & 0xFF00) | (ccr & 0xFF);
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
        if (set) {
            value = rawValue() | (1 << bit);
            return;
        }
        value = rawValue() & ~(1 << bit);
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
}
