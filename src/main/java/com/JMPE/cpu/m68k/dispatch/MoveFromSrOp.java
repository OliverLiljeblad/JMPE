package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.AddressErrorException;
import com.JMPE.cpu.m68k.exceptions.FaultAccessType;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.Objects;

public final class MoveFromSrOp implements Op {
    @Override
    public int execute(M68kCpu cpu, Bus bus, DecodedInstruction decoded) {
        Objects.requireNonNull(cpu, "cpu must not be null");
        Objects.requireNonNull(decoded, "decoded must not be null");

        DispatchSupport.requireOpcode(decoded, Opcode.MOVE_FROM_SR, "MOVE_FROM_SR");
        DispatchSupport.requireSize(decoded, Size.WORD, "MOVE_FROM_SR");
        DispatchSupport.requireDestination(decoded, "MOVE_FROM_SR");
        DispatchSupport.requireNoExtension(decoded, "MOVE_FROM_SR");
        DispatchSupport.requireSrOperand(decoded.src(), "source", "MOVE_FROM_SR");

        try {
            DispatchSupport.writeDestination(decoded, cpu, bus, cpu.statusRegister().rawValue());
        } catch (AddressErrorException addressErrorException) {
            // Preserve MOVE_FROM_SR fault semantics as a READ while retaining the original
            // exception as the cause for accurate stack traces and lower-level fault context.
            AddressErrorException remappedAddressError =
                    new AddressErrorException(addressErrorException.address(), FaultAccessType.READ);
            remappedAddressError.initCause(addressErrorException);
            throw remappedAddressError;
        }
        return DispatchSupport.moveCycles();
    }
}
