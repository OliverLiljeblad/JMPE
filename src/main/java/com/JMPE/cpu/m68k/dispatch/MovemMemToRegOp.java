package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Movem;

import java.util.Objects;

public final class MovemMemToRegOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVEM_MEM_TO_REG, "MOVEM_MEM_TO_REG");
        DispatchSupport.requireSource(decoded, "MOVEM_MEM_TO_REG");
        DispatchSupport.requireNoDestination(decoded, "MOVEM_MEM_TO_REG");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("MOVEM_MEM_TO_REG must be decoded as a WORD or LONG operation");
        }

        int registerCount = DispatchSupport.movemRegisterCount(decoded.extension());
        int startAddress = DispatchSupport.movemStartAddress(decoded.src(), cpu, decoded.size(), registerCount);
        int effectiveAddressRegister = DispatchSupport.movemEffectiveAddressRegister(decoded.src());
        Movem.AddressingMode addressingMode = DispatchSupport.movemAddressingMode(decoded.src());
        DispatchSupport.applyMovemWriteback(decoded.src(), cpu, startAddress, decoded.size(), registerCount);
        int cycles = Movem.executeMemoryToRegisters(
            decoded.size(),
            addressingMode,
            decoded.extension(),
            startAddress,
            effectiveAddressRegister,
            (address, size) -> switch (size) {
                case WORD -> bus.readWord(address);
                case LONG -> bus.readLong(address);
                default -> throw new IllegalArgumentException("MOVEM_MEM_TO_REG requires WORD or LONG size");
            },
            (registerIndex, value) -> DispatchSupport.writeMovemRegister(cpu, registerIndex, value)
        );
        return cycles;
    }
}
