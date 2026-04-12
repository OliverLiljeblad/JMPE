package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Addi;
import java.util.Objects;

/**
 * Dispatch-layer executor for decoded {@code ADDI} instructions.
 */
public final class Addiop implements Op {

    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu,     "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.ADDI, "ADDI");
        DispatchSupport.requireSized(decoded, "ADDI");
        DispatchSupport.requireSource(decoded, "ADDI");
        DispatchSupport.requireDestination(decoded, "ADDI");
        DispatchSupport.requireNoExtension(decoded, "ADDI");

        OperandResolver.Location destination = DispatchSupport.resolveDestination(decoded, cpu, bus);

        return Addi.execute(
            decoded.size(),
            () -> DispatchSupport.readSource(decoded, cpu, bus),
            destination::read,
            destination::write,
            cpu.statusRegister().addConditionCodes()  // returns Add.ConditionCodes — matches perfectly
        );
    }
}
