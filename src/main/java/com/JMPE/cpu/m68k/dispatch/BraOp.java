package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Bra;

import java.util.Objects;

public final class BraOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.BRA, "BRA");
        requireBranchSize(decoded, "BRA");
        DispatchSupport.requireNoDestination(decoded, "BRA");
        DispatchSupport.requireNoExtension(decoded, "BRA");

        EffectiveAddress.Immediate displacement = DispatchSupport.requireImmediateSource(decoded, "BRA");
        return Bra.execute(
            DispatchSupport.branchBase(cpu, decoded.size()),
            displacement.value(),
            cpu.registers()::setProgramCounter
        );
    }

    private static void requireBranchSize(DecodedInstruction decoded, String operation) {
        if (decoded.size() != Size.BYTE && decoded.size() != Size.WORD) {
            throw new IllegalArgumentException(operation + " must be decoded with BYTE or WORD displacement");
        }
    }
}
