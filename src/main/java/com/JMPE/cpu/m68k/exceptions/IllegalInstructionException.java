package com.JMPE.cpu.m68k.exceptions;

public class IllegalInstructionException extends Exception {
    public IllegalInstructionException(int opword, int extPc) {
        super(String.format("<[IllegalInstructionException]> %d does not correspond to a valid instruction opcode", opword));
    }
}
