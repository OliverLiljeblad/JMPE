package com.JMPE.cpu;

public class Clock {
    // Mac Plus: 7,833,600 Hz
    public static final long MASTER_HZ = 7_833_600;

    private long cycles = 0;
    public void advance(int cycles) {
        this.cycles += cycles;
    }

    public long getCycles() {
        return cycles;
    }

    public static long asCycles(long period) {
        return MASTER_HZ / period;
    }
}
