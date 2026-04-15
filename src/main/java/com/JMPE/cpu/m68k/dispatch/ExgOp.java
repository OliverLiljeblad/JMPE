package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import com.JMPE.cpu.m68k.instructions.data.Exg;

import java.util.Objects;

public final class ExgOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        validate(decoded);
        return executeExchange(cpu, decoded.src(), decoded.dst());
    }

    private static void validate(DecodedInstruction decoded) {
        DispatchSupport.requireOpcode(decoded, Opcode.EXG, "EXG");
        DispatchSupport.requireSize(decoded, Size.LONG, "EXG");
        DispatchSupport.requireSource(decoded, "EXG");
        DispatchSupport.requireDestination(decoded, "EXG");
        DispatchSupport.requireNoExtension(decoded, "EXG");
    }

    private static int executeExchange(M68kCpu cpu, EffectiveAddress src, EffectiveAddress dst) {
        return switch (src) {
            case EffectiveAddress.DataReg(int srcReg) -> switch (dst) {
                case EffectiveAddress.DataReg(int dstReg) -> Exg.execute(
                    () -> cpu.registers().data(srcReg),
                    () -> cpu.registers().data(dstReg),
                    value -> cpu.registers().setData(srcReg, value),
                    value -> cpu.registers().setData(dstReg, value)
                );
                case EffectiveAddress.AddrReg(int dstReg) -> Exg.execute(
                    () -> cpu.registers().data(srcReg),
                    () -> cpu.registers().address(dstReg),
                    value -> cpu.registers().setData(srcReg, value),
                    value -> cpu.registers().setAddress(dstReg, value)
                );
                default -> throw invalidOperands(src, dst);
            };
            case EffectiveAddress.AddrReg(int srcReg) -> switch (dst) {
                case EffectiveAddress.DataReg(int dstReg) -> Exg.execute(
                    () -> cpu.registers().address(srcReg),
                    () -> cpu.registers().data(dstReg),
                    value -> cpu.registers().setAddress(srcReg, value),
                    value -> cpu.registers().setData(dstReg, value)
                );
                case EffectiveAddress.AddrReg(int dstReg) -> Exg.execute(
                    () -> cpu.registers().address(srcReg),
                    () -> cpu.registers().address(dstReg),
                    value -> cpu.registers().setAddress(srcReg, value),
                    value -> cpu.registers().setAddress(dstReg, value)
                );
                default -> throw invalidOperands(src, dst);
            };
            default -> throw invalidOperands(src, dst);
        };
    }

    private static IllegalArgumentException invalidOperands(EffectiveAddress src, EffectiveAddress dst) {
        return new IllegalArgumentException(
            "EXG requires data/data, address/address, or data/address registers but was src=" + src + " dst=" + dst
        );
    }
}
