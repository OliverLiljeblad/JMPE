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
    private static final int T1C_L = 4;
    private static final int T1C_H = 5;
    private static final int T1L_L = 6;
    private static final int T1L_H = 7;
    private static final int T2C_L = 8;
    private static final int T2C_H = 9;
    private static final int ACR = 11;
    private static final int IFR = 13;
    private static final int IER = 14;
    private static final int ORA_NO_HANDSHAKE = 15;
    private static final int INTERRUPT_BITS = 0x7F;
    private static final int IER_SET_MASK = 0x80;
    private static final int IRQ_SUMMARY_FLAG = 0x80;
    private static final int CA1_INTERRUPT_FLAG = 0x02;
    private static final int T1_INTERRUPT_FLAG = 0x40;
    private static final int T2_INTERRUPT_FLAG = 0x20;
    private static final int ACR_T1_FREE_RUN = 0x40;
    private static final int ACR_T2_PB6_PULSE_COUNT = 0x20;
    // VIA is clocked at 1/10 of the 68000 system clock on the Mac Plus.
    private static final int VIA_CYCLES_PER_CPU_CYCLE = 10;
    // Mac Plus drives VIA CA1 from the video VBL signal at ~60.15 Hz. With a 7.8336 MHz CPU
    // clock that is one CA1 edge every 7833600 / 60.15 ~= 130235 CPU cycles.
    private static final int CA1_VBL_PERIOD_CPU_CYCLES = 130_235;

    private final int[] registers = new int[REGISTER_COUNT];
    private final IntConsumer portAListener;

    private int orb;
    private int ora = 0xFF;
    private int ddrb;
    private int ddra;
    private int interruptFlags = CA1_INTERRUPT_FLAG;
    private int interruptEnable;

    // T1 timer state.
    private int t1Latch = 0xFFFF;
    private int t1Counter = 0xFFFF;
    private int t1LatchLowHolding;
    private int acr;
    private boolean t1ArmedForInterrupt;
    private int viaCycleAccumulator;
    // T2 timer state. T2 is always one-shot in interval-timer mode (ACR bit 5 = 0). Pulse-counting
    // (PB6) mode is recognised but does not currently advance the counter.
    private int t2Counter = 0xFFFF;
    private int t2LatchLowHolding;
    private boolean t2ArmedForInterrupt;
    // CA1 (VBL) periodic assertion accumulator.
    private int ca1CycleAccumulator;

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
            case T1C_L -> {
                // Reading T1C-L clears the T1 interrupt flag.
                clearT1InterruptFlag();
                yield t1Counter & 0xFF;
            }
            case T1C_H -> (t1Counter >>> 8) & 0xFF;
            case T1L_L -> t1Latch & 0xFF;
            case T1L_H -> (t1Latch >>> 8) & 0xFF;
            case T2C_L -> {
                // Reading T2C-L clears the T2 interrupt flag.
                clearT2InterruptFlag();
                yield t2Counter & 0xFF;
            }
            case T2C_H -> (t2Counter >>> 8) & 0xFF;
            case ACR -> acr;
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
            case T1C_L, T1L_L -> {
                // Both addresses write to the T1 low-order latch holding register.
                t1LatchLowHolding = byteValue;
            }
            case T1C_H -> {
                // Write the high latch, transfer latch->counter, clear T1 IFR, and arm for free-run.
                t1Latch = ((byteValue & 0xFF) << 8) | (t1LatchLowHolding & 0xFF);
                t1Counter = t1Latch;
                clearT1InterruptFlag();
                t1ArmedForInterrupt = true;
            }
            case T1L_H -> {
                t1Latch = ((byteValue & 0xFF) << 8) | (t1LatchLowHolding & 0xFF);
                clearT1InterruptFlag();
            }
            case T2C_L -> {
                // Latches the T2 low-order count (no interrupt clear, no transfer).
                t2LatchLowHolding = byteValue;
            }
            case T2C_H -> {
                // Loads the high-order counter, transfers the held low byte, clears T2 IFR, arms.
                t2Counter = ((byteValue & 0xFF) << 8) | (t2LatchLowHolding & 0xFF);
                clearT2InterruptFlag();
                t2ArmedForInterrupt = true;
            }
            case ACR -> {
                acr = byteValue;
                registers[ACR] = byteValue;
            }
            case IFR -> clearInterruptFlags(byteValue);
            case IER -> updateInterruptEnable(byteValue);
            default -> registers[normalized] = byteValue;
        }
    }

    /**
     * Advance the VIA timers by the given number of CPU cycles. The 6522 inside the Mac Plus is
     * clocked at one tenth of the 68000 clock, so cycles are accumulated and divided by ten before
     * decrementing T1.
     */
    public void tick(int cpuCycles) {
        if (cpuCycles <= 0) {
            return;
        }
        // Drive the CA1 (vertical blank) input at ~60.15 Hz independently of the VIA clock.
        ca1CycleAccumulator += cpuCycles;
        while (ca1CycleAccumulator >= CA1_VBL_PERIOD_CPU_CYCLES) {
            ca1CycleAccumulator -= CA1_VBL_PERIOD_CPU_CYCLES;
            interruptFlags |= CA1_INTERRUPT_FLAG;
            registers[IFR] = interruptFlags;
        }
        viaCycleAccumulator += cpuCycles;
        int viaCycles = viaCycleAccumulator / VIA_CYCLES_PER_CPU_CYCLE;
        viaCycleAccumulator -= viaCycles * VIA_CYCLES_PER_CPU_CYCLE;
        if (viaCycles == 0) {
            return;
        }
        // Decrement T1; on underflow set the T1 interrupt flag and either reload (free-run) or
        // disarm (one-shot). In free-run mode the counter wraps after (latch + 2) cycles.
        int remaining = viaCycles;
        while (remaining > 0) {
            int countToUnderflow = (t1Counter & 0xFFFF) + 1;
            if (remaining < countToUnderflow) {
                t1Counter = (t1Counter - remaining) & 0xFFFF;
                break;
            }
            remaining -= countToUnderflow;
            if (t1ArmedForInterrupt) {
                interruptFlags |= T1_INTERRUPT_FLAG;
                registers[IFR] = interruptFlags;
            }
            if ((acr & ACR_T1_FREE_RUN) != 0) {
                t1Counter = t1Latch & 0xFFFF;
            } else {
                // One-shot: only fire once until the timer is rewritten.
                t1ArmedForInterrupt = false;
                t1Counter = 0xFFFF;
            }
        }
        // Decrement T2 (interval-timer mode only). Pulse-counting mode is not modelled and the
        // counter is left alone, which is correct for code that doesn't toggle PB6.
        if ((acr & ACR_T2_PB6_PULSE_COUNT) == 0) {
            int t2Remaining = viaCycles;
            while (t2Remaining > 0) {
                int countToUnderflow = (t2Counter & 0xFFFF) + 1;
                if (t2Remaining < countToUnderflow) {
                    t2Counter = (t2Counter - t2Remaining) & 0xFFFF;
                    break;
                }
                t2Remaining -= countToUnderflow;
                if (t2ArmedForInterrupt) {
                    interruptFlags |= T2_INTERRUPT_FLAG;
                    registers[IFR] = interruptFlags;
                    t2ArmedForInterrupt = false;
                }
                // T2 free-runs through 0xFFFF after underflow; it never reloads automatically.
                t2Counter = 0xFFFF;
            }
        }
    }

    private void clearT1InterruptFlag() {
        interruptFlags &= ~T1_INTERRUPT_FLAG;
        registers[IFR] = interruptFlags;
    }

    private void clearT2InterruptFlag() {
        interruptFlags &= ~T2_INTERRUPT_FLAG;
        registers[IFR] = interruptFlags;
    }

    public boolean isIrqAsserted() {
        return (composeInterruptFlagRegister() & IRQ_SUMMARY_FLAG) != 0;
    }

    /** Diagnostic snapshot for boot bring-up. */
    public String debugState() {
        return String.format(
            "VIA{IFR=0x%02X IER=0x%02X ACR=0x%02X T1ctr=0x%04X T1lat=0x%04X T2ctr=0x%04X t1=%s t2=%s}",
            interruptFlags & 0xFF, interruptEnable & 0xFF, acr & 0xFF,
            t1Counter & 0xFFFF, t1Latch & 0xFFFF, t2Counter & 0xFFFF,
            t1ArmedForInterrupt, t2ArmedForInterrupt);
    }

    private int readInterruptFlagRegister() {
        return composeInterruptFlagRegister();
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

    //TODO: Does viaRegister need to be normalized?
    private static int viaRegister(int offset) {
        return (offset >>> 9) & 0x0F;
    }

    private static int normalize(int register) {
        return register & 0x0F;
    }
}
