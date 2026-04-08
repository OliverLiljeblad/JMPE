package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Movem;

import java.util.Objects;

public final class MovemRegToMemOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVEM_REG_TO_MEM, "MOVEM_REG_TO_MEM");
        DispatchSupport.requireNoSource(decoded, "MOVEM_REG_TO_MEM");
        DispatchSupport.requireDestination(decoded, "MOVEM_REG_TO_MEM");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("MOVEM_REG_TO_MEM must be decoded as a WORD or LONG operation");
        }

        int registerCount = DispatchSupport.movemRegisterCount(decoded.extension());
        int startAddress = DispatchSupport.movemStartAddress(decoded.dst(), cpu, decoded.size(), registerCount);
        int effectiveAddressRegister = DispatchSupport.movemEffectiveAddressRegister(decoded.dst());
        Movem.AddressingMode addressingMode = DispatchSupport.movemAddressingMode(decoded.dst());
        int cycles = Movem.executeRegistersToMemory(
            decoded.size(),
            addressingMode,
            decoded.extension(),
            startAddress,
            effectiveAddressRegister,
            registerIndex -> DispatchSupport.readMovemRegister(cpu, registerIndex),
            (address, size, value) -> {
                switch (size) {
                    case WORD -> bus.writeWord(address, value);
                    case LONG -> bus.writeLong(address, value);
                    default -> throw new IllegalArgumentException("MOVEM_REG_TO_MEM requires WORD or LONG size");
                }
            }
        );
        DispatchSupport.applyMovemWriteback(decoded.dst(), cpu, startAddress, decoded.size(), registerCount);
        return cycles;
    }
}
