package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Bsr;

import java.util.Objects;

public final class BsrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.BSR, "BSR");
        requireBranchSize(decoded, "BSR");
        DispatchSupport.requireNoDestination(decoded, "BSR");
        DispatchSupport.requireNoExtension(decoded, "BSR");

        EffectiveAddress.Immediate displacement = DispatchSupport.requireImmediateSource(decoded, "BSR");
        int returnAddress = cpu.registers().programCounter();
        return Bsr.execute(
            DispatchSupport.branchBase(cpu, decoded.size()),
            displacement.value(),
            returnAddress,
            value -> DispatchSupport.pushLong(cpu, bus, value),
            cpu.registers()::setProgramCounter
        );
    }

    private static void requireBranchSize(DecodedInstruction decoded, String operation) {
        if (decoded.size() != Size.BYTE && decoded.size() != Size.WORD) {
            throw new IllegalArgumentException(operation + " must be decoded with BYTE or WORD displacement");
        }
    }
}
