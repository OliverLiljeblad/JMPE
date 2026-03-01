package com.JMPE.memory;

public enum Unit {
    BYTES(1),
    KB(1 << 10),
    MB(1 << 20);

    private final int multiplier;
    Unit(int size) {
        this.multiplier = size;
    }

    public int the() {
        return this.multiplier;
    }
}
