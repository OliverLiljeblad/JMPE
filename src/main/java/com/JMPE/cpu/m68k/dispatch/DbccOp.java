package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.control.Dbcc;

import java.util.Objects;

public final class DbccOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.DBcc, "DBCC");
        DispatchSupport.requireUnsized(decoded, "DBCC");

        EffectiveAddress.Immediate displacement = DispatchSupport.requireImmediateSource(decoded, "DBCC");
        int counterRegister = DispatchSupport.requireDataRegister(decoded.dst(), "counter", "DBCC");

        return Dbcc.execute(
            decoded.extension(),
            DispatchSupport.branchBase(cpu, Size.WORD),
            displacement.value(),
            () -> cpu.registers().data(counterRegister),
            value -> cpu.registers().setData(counterRegister, value),
            DispatchSupport.conditionCodesReader(cpu),
            cpu.registers()::setProgramCounter
        );
    }
}
