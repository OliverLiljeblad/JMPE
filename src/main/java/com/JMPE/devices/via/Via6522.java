package com.JMPE.devices.via;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Minimal 6522 register model for early Mac boot sequencing.
 *
 * <p>This is not a full VIA emulation yet; it only needs to preserve register
 * writes and surface Port A bit 4 so the machine overlay can switch from ROM
 * to RAM.</p>
 */
public final class Via6522 {
    private static final int REGISTER_COUNT = 16;
    private static final int ORB = 0;
    private static final int ORA = 1;
    private static final int DDRB = 2;
    private static final int DDRA = 3;
    private static final int ORA_NO_HANDSHAKE = 15;

    private final int[] registers = new int[REGISTER_COUNT];
    private final IntConsumer portAListener;

    private int orb;
    private int ora = 0xFF;
    private int ddrb;
    private int ddra;

    public Via6522(IntConsumer portAListener) {
        this.portAListener = Objects.requireNonNull(portAListener, "portAListener must not be null");
        registers[ORA] = ora;
        registers[ORA_NO_HANDSHAKE] = ora;
        updatePortA();
    }

    public int readRegister(int register) {
        return switch (normalize(register)) {
            case ORB -> orb;
            case ORA, ORA_NO_HANDSHAKE -> ora;
            case DDRB -> ddrb;
            case DDRA -> ddra;
            default -> registers[normalize(register)];
        };
    }

    public void writeRegister(int register, int value) {
        int normalized = normalize(register);
        int byteValue = value & 0xFF;

        switch (normalized) {
            case ORB -> {
                orb = byteValue;
                registers[ORB] = byteValue;
            }
            case ORA, ORA_NO_HANDSHAKE -> {
                ora = byteValue;
                registers[ORA] = byteValue;
                registers[ORA_NO_HANDSHAKE] = byteValue;
                updatePortA();
            }
            case DDRB -> {
                ddrb = byteValue;
                registers[DDRB] = byteValue;
            }
            case DDRA -> {
                ddra = byteValue;
                registers[DDRA] = byteValue;
                updatePortA();
            }
            default -> registers[normalized] = byteValue;
        }
    }

    private void updatePortA() {
        int effectivePortA = (ora & ddra) | (~ddra & 0xFF);
        portAListener.accept(effectivePortA);
    }

    private static int normalize(int register) {
        return register & 0x0F;
    }
}
