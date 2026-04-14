package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Ext;

import java.util.Objects;

public final class ExtOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "EXT");
        return Ext.execute(
            decoded.size(),
            cpu.registers().data(destinationRegister),
            value -> DataRegisterWriter.write(cpu, destinationRegister, decoded.size(), value),
            cpu.statusRegister().moveConditionCodes()
        );
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.EXT, "EXT");
        DispatchSupport.requireSized(decoded, "EXT");
        DispatchSupport.requireNoSource(decoded, "EXT");
        DispatchSupport.requireDestination(decoded, "EXT");
        DispatchSupport.requireNoExtension(decoded, "EXT");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("EXT must be decoded as WORD or LONG");
        }
    }
}
