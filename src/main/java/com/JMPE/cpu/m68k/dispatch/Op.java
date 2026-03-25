package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;

/**
 * Executes one already-decoded instruction against live CPU state.
 *
 * <p>
 * The decoder is responsible for turning an opword into a
 * {@link DecodedInstruction}. An {@code Op} is the next layer: it interprets
 * that decoded shape and bridges into the helper implementations under
 * {@code cpu.m68k.instructions}.
 * </p>
 */
@FunctionalInterface
public interface Op {
    /**
     * Executes the decoded instruction and returns the instruction cycle cost.
     *
     * @param cpu the CPU state to read and mutate
     * @param decoded the already-decoded instruction descriptor
     * @return the execution cycle cost for the instruction
     */
    int execute(M68kCpu cpu, DecodedInstruction decoded);
}
