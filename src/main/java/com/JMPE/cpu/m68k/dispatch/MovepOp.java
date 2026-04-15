package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Movep;

import java.util.Objects;

public final class MovepOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);

        return switch (decoded.src()) {
            case EffectiveAddress.DataReg(int srcReg) -> {
                EffectiveAddress memoryOperand = requireMovepMemoryOperand(decoded.src(), decoded.dst());
                int address = DispatchSupport.computeAddress(memoryOperand, cpu);
                yield Movep.moveRegisterToMemory(decoded.size(), () -> cpu.registers().data(srcReg), address, bus::writeByte);
            }
            case EffectiveAddress.AddrRegIndDisp ignored -> {
                EffectiveAddress.DataReg destination = requireMovepRegisterOperand(decoded.src(), decoded.dst());
                int address = DispatchSupport.computeAddress(decoded.src(), cpu);
                yield Movep.moveMemoryToRegister(
                    decoded.size(),
                    address,
                    bus::readByte,
                    value -> DataRegisterWriter.write(cpu, destination.reg(), decoded.size(), value)
                );
            }
            default -> throw invalidOperands(decoded.src(), decoded.dst());
        };
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.MOVEP, "MOVEP");
        if (decoded.size() != Size.WORD && decoded.size() != Size.LONG) {
            throw new IllegalArgumentException("MOVEP must be decoded as WORD or LONG");
        }
        DispatchSupport.requireSource(decoded, "MOVEP");
        DispatchSupport.requireDestination(decoded, "MOVEP");
        DispatchSupport.requireNoExtension(decoded, "MOVEP");
    }

    private static EffectiveAddress.AddrRegIndDisp requireMovepMemoryOperand(EffectiveAddress src, EffectiveAddress dst) {
        if (dst instanceof EffectiveAddress.AddrRegIndDisp memoryOperand) {
            return memoryOperand;
        }
        throw invalidOperands(src, dst);
    }

    private static EffectiveAddress.DataReg requireMovepRegisterOperand(EffectiveAddress src, EffectiveAddress dst) {
        if (dst instanceof EffectiveAddress.DataReg registerOperand) {
            return registerOperand;
        }
        throw invalidOperands(src, dst);
    }

    private static IllegalArgumentException invalidOperands(EffectiveAddress src, EffectiveAddress dst) {
        return new IllegalArgumentException(
            "MOVEP requires one data register and one (d16,An) operand but was src=" + src + " dst=" + dst
        );
    }
}
