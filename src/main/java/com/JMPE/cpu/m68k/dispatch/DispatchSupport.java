package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Registers;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Move;
import com.JMPE.cpu.m68k.instructions.data.Movem;

import java.util.Objects;

final class DispatchSupport {
    private static final int MOVEM_REGISTER_COUNT = 16;
    private static final int MOVEM_ADDRESS_REGISTER_BASE = 8;

    private DispatchSupport() {
    }

    static void requireOpcode(DecodedInstruction decoded, Opcode expected, String operation) {
        if (decoded.opcode() != expected) {
            throw new IllegalArgumentException(operation + " requires opcode " + expected + " but was " + decoded.opcode());
        }
    }

    static void requireSized(DecodedInstruction decoded, String operation) {
        if (!decoded.size().isSized()) {
            throw new IllegalArgumentException(operation + " must be decoded with a sized operand");
        }
    }

    static void requireUnsized(DecodedInstruction decoded, String operation) {
        if (!decoded.isUnsized()) {
            throw new IllegalArgumentException(operation + " must be decoded as UNSIZED");
        }
    }

    static void requireSize(DecodedInstruction decoded, Size expected, String operation) {
        if (decoded.size() != expected) {
            throw new IllegalArgumentException(operation + " must be decoded as a " + expected + " operation");
        }
    }

    static void requireNoSource(DecodedInstruction decoded, String operation) {
        if (!decoded.hasNoSource()) {
            throw new IllegalArgumentException(operation + " must not have a source operand");
        }
    }

    static void requireSource(DecodedInstruction decoded, String operation) {
        if (decoded.hasNoSource()) {
            throw new IllegalArgumentException(operation + " must have a source operand");
        }
    }

    static void requireNoDestination(DecodedInstruction decoded, String operation) {
        if (!decoded.hasNoDestination()) {
            throw new IllegalArgumentException(operation + " must not have a destination operand");
        }
    }

    static void requireDestination(DecodedInstruction decoded, String operation) {
        if (decoded.hasNoDestination()) {
            throw new IllegalArgumentException(operation + " must have a destination operand");
        }
    }

    static void requireNoExtension(DecodedInstruction decoded, String operation) {
        if (decoded.extension() != 0) {
            throw new IllegalArgumentException(operation + " must not carry an extension payload");
        }
    }

    static EffectiveAddress.Immediate requireImmediateSource(DecodedInstruction decoded, String operation) {
        requireSource(decoded, operation);
        if (decoded.src() instanceof EffectiveAddress.Immediate immediate) {
            return immediate;
        }
        throw new IllegalArgumentException(operation + " requires immediate source but was " + decoded.src());
    }

    static EffectiveAddress.Sr requireSrOperand(EffectiveAddress operand, String role, String operation) {
        if (operand instanceof EffectiveAddress.Sr sr) {
            return sr;
        }
        throw new IllegalArgumentException(operation + " requires SR as the " + role + " but was " + operand);
    }

    static EffectiveAddress.Ccr requireCcrOperand(EffectiveAddress operand, String role, String operation) {
        if (operand instanceof EffectiveAddress.Ccr ccr) {
            return ccr;
        }
        throw new IllegalArgumentException(operation + " requires CCR as the " + role + " but was " + operand);
    }

    static int requireDataRegister(EffectiveAddress operand, String role, String operation) {
        if (operand instanceof EffectiveAddress.DataReg dataReg) {
            return dataReg.reg();
        }
        throw new IllegalArgumentException(operation + " requires data-register " + role + " but was " + operand);
    }

    static int requireAddressRegister(EffectiveAddress operand, String role, String operation) {
        if (operand instanceof EffectiveAddress.AddrReg addrReg) {
            return addrReg.reg();
        }
        throw new IllegalArgumentException(operation + " requires address-register " + role + " but was " + operand);
    }

    static OperandResolver.Location resolveDestination(DecodedInstruction decoded, M68kCpu cpu, Bus bus) {
        return OperandResolver.resolveLocation(decoded.dst(), cpu, bus, decoded.size());
    }

    static int readSource(DecodedInstruction decoded, M68kCpu cpu, Bus bus) {
        return OperandResolver.read(decoded.src(), cpu, bus, decoded.size());
    }

    static void writeDestination(DecodedInstruction decoded, M68kCpu cpu, Bus bus, int value) {
        OperandResolver.write(decoded.dst(), cpu, bus, decoded.size(), value);
    }

    static int computeAddress(EffectiveAddress operand, M68kCpu cpu) {
        return OperandResolver.computeAddress(operand, cpu);
    }

    static void requireSupervisor(M68kCpu cpu, String operation) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        if (!cpu.statusRegister().isSupervisorSet()) {
            throw new PrivilegeViolation(operation);
        }
    }

    static void pushLong(M68kCpu cpu, Bus bus, int value) {
        int stackPointer = cpu.registers().stackPointer() - Size.LONG.bytes();
        cpu.registers().setStackPointer(stackPointer);
        bus.writeLong(stackPointer, value);
    }

    static int popLong(M68kCpu cpu, Bus bus) {
        int stackPointer = cpu.registers().stackPointer();
        int value = bus.readLong(stackPointer);
        cpu.registers().setStackPointer(stackPointer + Size.LONG.bytes());
        return value;
    }

    static int moveCycles() {
        return Move.DEFAULT_CYCLES;
    }

    static int movemRegisterCount(int registerMask) {
        return Integer.bitCount(registerMask & 0xFFFF);
    }

    static int movemStartAddress(EffectiveAddress operand, M68kCpu cpu, Size size, int registerCount) {
        return switch (operand) {
            case EffectiveAddress.AddrRegIndPostInc(int reg) -> cpu.registers().address(reg);
            case EffectiveAddress.AddrRegIndPreDec(int reg) -> cpu.registers().address(reg) - (registerCount * size.bytes());
            default -> OperandResolver.computeAddress(operand, cpu);
        };
    }

    static Movem.AddressingMode movemAddressingMode(EffectiveAddress operand) {
        return switch (operand) {
            case EffectiveAddress.AddrRegIndPostInc ignored -> Movem.AddressingMode.POSTINCREMENT;
            case EffectiveAddress.AddrRegIndPreDec ignored -> Movem.AddressingMode.PREDECREMENT;
            default -> Movem.AddressingMode.CONTROL;
        };
    }

    static int movemEffectiveAddressRegister(EffectiveAddress operand) {
        return switch (operand) {
            case EffectiveAddress.AddrRegInd(int reg) -> reg;
            case EffectiveAddress.AddrRegIndPostInc(int reg) -> reg;
            case EffectiveAddress.AddrRegIndPreDec(int reg) -> reg;
            case EffectiveAddress.AddrRegIndDisp(int reg, int ignored) -> reg;
            case EffectiveAddress.AddrRegIndIndex indexed -> indexed.baseReg();
            default -> -1;
        };
    }

    static void applyMovemWriteback(EffectiveAddress operand, M68kCpu cpu, int startAddress, Size size, int registerCount) {
        switch (operand) {
            case EffectiveAddress.AddrRegIndPostInc(int reg) ->
                cpu.registers().setAddress(reg, startAddress + (registerCount * size.bytes()));
            case EffectiveAddress.AddrRegIndPreDec(int reg) ->
                cpu.registers().setAddress(reg, startAddress);
            default -> {
            }
        }
    }

    static int readMovemRegister(M68kCpu cpu, int registerIndex) {
        validateMovemRegisterIndex(registerIndex);
        return registerIndex < Registers.DATA_REGISTER_COUNT
            ? cpu.registers().data(registerIndex)
            : cpu.registers().address(registerIndex - MOVEM_ADDRESS_REGISTER_BASE);
    }

    static void writeMovemRegister(M68kCpu cpu, int registerIndex, int value) {
        validateMovemRegisterIndex(registerIndex);
        if (registerIndex < Registers.DATA_REGISTER_COUNT) {
            cpu.registers().setData(registerIndex, value);
            return;
        }
        cpu.registers().setAddress(registerIndex - MOVEM_ADDRESS_REGISTER_BASE, value);
    }

    static int quickValue(DecodedInstruction decoded, String operation) {
        return requireImmediateSource(decoded, operation).value();
    }

    static int shiftCount(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        return switch (decoded.src()) {
            case EffectiveAddress.None ignored -> 1;
            case EffectiveAddress.Immediate immediate -> immediate.value();
            default -> OperandResolver.read(decoded.src(), cpu, bus, Size.LONG) & 0x3F;
        };
    }

    private static void validateMovemRegisterIndex(int registerIndex) {
        if (registerIndex < 0 || registerIndex >= MOVEM_REGISTER_COUNT) {
            throw new IllegalArgumentException("MOVEM register index must be in range 0..15");
        }
    }
}
