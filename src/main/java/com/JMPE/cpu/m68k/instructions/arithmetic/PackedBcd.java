package com.JMPE.cpu.m68k.instructions.arithmetic;

final class PackedBcd {
    private PackedBcd() {
    }

    static Result add(int source, int destination, boolean extendSet) {
        int low = lowDigit(destination) + lowDigit(source) + (extendSet ? 1 : 0);
        int lowCarry = 0;
        if (low >= 10) {
            low -= 10;
            lowCarry = 1;
        }

        int high = highDigit(destination) + highDigit(source) + lowCarry;
        boolean carry = false;
        if (high >= 10) {
            high -= 10;
            carry = true;
        }

        return new Result((high << 4) | low, carry);
    }

    static Result subtract(int source, int destination, boolean extendSet) {
        int low = lowDigit(destination) - lowDigit(source) - (extendSet ? 1 : 0);
        int lowBorrow = 0;
        if (low < 0) {
            low += 10;
            lowBorrow = 1;
        }

        int high = highDigit(destination) - highDigit(source) - lowBorrow;
        boolean borrow = false;
        if (high < 0) {
            high += 10;
            borrow = true;
        }

        return new Result((high << 4) | low, borrow);
    }

    static int lowDigit(int value) {
        return value & 0x0F;
    }

    static int highDigit(int value) {
        return (value >>> 4) & 0x0F;
    }

    record Result(int value, boolean carry) {
    }
}
