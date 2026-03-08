package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;

/**
 * Represents a single 68k instruction handler.
 * Instances are shared across multiple opcodes to save memory.
 */
@FunctionalInterface
public interface Op {
    /**
     * Executes the instruction.
     *
     * @param cpu     The CPU context (registers, PC, SR)
     * @param bus     The system bus for memory access
     * @param opcode  The full 16-bit opcode word (needed to decode operands)
     * @throws com.JMPE.cpu.m68k.exceptions.AddressError if alignment fails
     * @throws com.JMPE.cpu.m68k.exceptions.BusError if memory access fails
     */
    void execute(M68kCpu cpu, Bus bus, int opcode, int metadata);

    /**
     * Returns the number of clock cycles this instruction consumes.
     * Can be static or dynamic depending on addressing mode.
     * For simplicity in dispatch, we often calculate this inside execute()
     * and update cpu.cycles directly, but this method helps for profiling.
     */
    default int consumedCycles(int opcode) { return 4; }

    //NOTE: Decide everything Op.Illegal should do
    public final class Illegal implements Op {
        public void execute(M68kCpu cpu, Bus bus, int opcode, int metadata) {
            //TODO: decide what to do here
        }
    }
}
