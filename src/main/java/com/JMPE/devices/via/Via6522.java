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
    private static final int IFR = 13;
    private static final int IER = 14;
    private static final int ORA_NO_HANDSHAKE = 15;
    private static final int INTERRUPT_BITS = 0x7F;
    private static final int IER_SET_MASK = 0x80;
    private static final int IRQ_SUMMARY_FLAG = 0x80;
    private static final int CA1_INTERRUPT_FLAG = 0x02;

    private final int[] registers = new int[REGISTER_COUNT];
    private final IntConsumer portAListener;

    private int orb;
    private int ora = 0xFF;
    private int ddrb;
    private int ddra;
    private int interruptFlags = CA1_INTERRUPT_FLAG;
    private int interruptEnable;

    public Via6522(IntConsumer portAListener) {
        this.portAListener = Objects.requireNonNull(portAListener, "portAListener must not be null");
        registers[ORA] = ora;
        registers[ORA_NO_HANDSHAKE] = ora;
        registers[IFR] = interruptFlags;
        registers[IER] = interruptEnable;
        updatePortA();
    }

    public int readRegister(int register) {
        register = viaRegister(register);
        return switch (normalize(register)) {
            case ORB -> orb;
            case ORA, ORA_NO_HANDSHAKE -> ora;
            case DDRB -> ddrb;
            case DDRA -> ddra;
            case IFR -> readInterruptFlagRegister();
            case IER -> IER_SET_MASK | interruptEnable;
            default -> registers[normalize(register)];
        };
    }

    public void writeRegister(int register, int value) {
        int normalized = normalize(viaRegister(register));
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
            case IFR -> clearInterruptFlags(byteValue);
            case IER -> updateInterruptEnable(byteValue);
            default -> registers[normalized] = byteValue;
        }
    }

    public boolean isIrqAsserted() {
        return (composeInterruptFlagRegister() & IRQ_SUMMARY_FLAG) != 0;
    }

    private int readInterruptFlagRegister() {
        int value = composeInterruptFlagRegister();
        if ((interruptFlags & CA1_INTERRUPT_FLAG) == 0) {
            interruptFlags |= CA1_INTERRUPT_FLAG;
            registers[IFR] = interruptFlags;
        }
        return value;
    }

    private int composeInterruptFlagRegister() {
        int value = interruptFlags & INTERRUPT_BITS;
        if ((value & interruptEnable) != 0) {
            value |= IRQ_SUMMARY_FLAG;
        }
        return value;
    }

    private void clearInterruptFlags(int value) {
        interruptFlags &= ~(value & INTERRUPT_BITS);
        registers[IFR] = interruptFlags;
    }

    private void updateInterruptEnable(int value) {
        int interruptMask = value & INTERRUPT_BITS;
        if ((value & IER_SET_MASK) != 0) {
            interruptEnable |= interruptMask;
        } else {
            interruptEnable &= ~interruptMask;
        }
        registers[IER] = interruptEnable;
    }

    private void updatePortA() {
        int effectivePortA = (ora & ddra) | (~ddra & 0xFF);
        portAListener.accept(effectivePortA);
    }

    private static int viaRegister(int offset) {
        return (offset >>> 9) & 0x0F;
    }

    private static int normalize(int register) {
        return register & 0x0F;
    }
}
