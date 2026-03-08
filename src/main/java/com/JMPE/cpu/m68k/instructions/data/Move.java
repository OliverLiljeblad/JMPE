package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Registers;
import com.JMPE.cpu.m68k.exceptions.BusError;

public class Move implements Op {

    @Override
    public void execute(M68kCpu cpu, Bus bus, int opcode) throws BusError {
        Registers registers = cpu.getRegisters();

        cpu.advanceProgramCounter();
    }
}
