package com.JMPE.cpu.m68k.instructions.arithmetic;

final class PackedBcd {
    private PackedBcd() {
    }

    static Result add(int source, int destination, boolean extendSet) {
        int low = (source & 0x0F) + (destination & 0x0F) + (extendSet ? 1 : 0);
        int high = (source & 0xF0) + (destination & 0xF0);
        int corrected = (high + low) & 0xFFFF;
        int uncorrected = corrected;

        if (low > 9) {
            corrected = (corrected + 0x06) & 0xFFFF;
        }

        boolean carry = (corrected & 0x3F0) > 0x90;
        if (carry) {
            corrected = (corrected + 0x60) & 0xFFFF;
        }

        int value = corrected & 0xFF;
        boolean overflow = (uncorrected & 0x80) == 0 && (value & 0x80) != 0;
        return new Result(value, carry, overflow);
    }

    static Result subtract(int source, int destination, boolean extendSet) {
        int low = ((destination & 0x0F) - (source & 0x0F) - (extendSet ? 1 : 0)) & 0xFFFF;
        int high = ((destination & 0xF0) - (source & 0xF0)) & 0xFFFF;
        int corrected = (high + low) & 0xFFFF;
        int uncorrected = corrected;
        int lowAdjust = 0;

        if ((low & 0xF0) != 0) {
            corrected = (corrected - 0x06) & 0xFFFF;
            lowAdjust = 0x06;
        }

        if ((((destination & 0xFF) - (source & 0xFF) - (extendSet ? 1 : 0)) & 0x100) != 0) {
            corrected = (corrected - 0x60) & 0xFFFF;
        }

        boolean carry = ((((destination & 0xFF) - (source & 0xFF) - lowAdjust - (extendSet ? 1 : 0)) & 0x300) > 0xFF);
        int value = corrected & 0xFF;
        boolean overflow = (uncorrected & 0x80) != 0 && (value & 0x80) == 0;
        return new Result(value, carry, overflow);
    }

    record Result(int value, boolean carry, boolean overflow) {
    }
}
