package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Swap;

import java.util.Objects;

public final class SwapOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "SWAP");
        return Swap.execute(
            cpu.registers().data(destinationRegister),
            value -> cpu.registers().setData(destinationRegister, value),
            cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.SWAP, "SWAP");
        DispatchSupport.requireSize(decoded, Size.LONG, "SWAP");
        DispatchSupport.requireNoSource(decoded, "SWAP");
        DispatchSupport.requireDestination(decoded, "SWAP");
        DispatchSupport.requireNoExtension(decoded, "SWAP");
    }
}
