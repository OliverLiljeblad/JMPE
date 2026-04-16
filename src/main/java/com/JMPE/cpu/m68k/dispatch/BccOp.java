package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Bcc;

import java.util.Objects;

public final class BccOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.BCC, "BCC");
        requireBranchSize(decoded, "BCC");
        DispatchSupport.requireNoDestination(decoded, "BCC");

        EffectiveAddress.Immediate displacement = DispatchSupport.requireImmediateSource(decoded, "BCC");
        return Bcc.execute(
            decodeCondition(decoded.extension()),
            DispatchSupport.branchBase(cpu, decoded.size()),
            displacement.value(),
            DispatchSupport.conditionCodesReader(cpu),
            DispatchSupport.controlTransferPcWriter(cpu)
        );
    }

    private static Bcc.Condition decodeCondition(int rawCondition) {
        if (rawCondition < 0x2 || rawCondition > 0xF) {
            throw new IllegalArgumentException("BCC extension must encode a condition in range 0x2..0xF");
        }
        return Bcc.Condition.values()[rawCondition - 0x2];
    }

    private static void requireBranchSize(DecodedInstruction decoded, String operation) {
        if (decoded.size() != Size.BYTE && decoded.size() != Size.WORD) {
            throw new IllegalArgumentException(operation + " must be decoded with BYTE or WORD displacement");
        }
    }
}
