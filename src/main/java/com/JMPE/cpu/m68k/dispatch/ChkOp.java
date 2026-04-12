package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.ChkException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.arithmetic.Chk;

import java.util.Objects;

public final class ChkOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.CHK, "CHK");
        DispatchSupport.requireSize(decoded, Size.WORD, "CHK");
        DispatchSupport.requireSource(decoded, "CHK");
        DispatchSupport.requireDestination(decoded, "CHK");
        DispatchSupport.requireNoExtension(decoded, "CHK");

        int destinationRegister = DispatchSupport.requireDataRegister(decoded.dst(), "destination", "CHK");
        return Chk.execute(
            0,
            0,
            destinationRegister,
            decoded.size(),
            (eaMode, eaReg, size) -> OperandResolver.read(decoded.src(), cpu, bus, size),
            registerIndex -> cpu.registers().data(registerIndex),
            vector -> {
                throw new ChkException(vector);
            },
            cpu.statusRegister()::setNegative
        );
    }
}
