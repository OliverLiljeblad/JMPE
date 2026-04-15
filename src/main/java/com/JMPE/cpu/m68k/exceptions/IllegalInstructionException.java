package com.JMPE.cpu.m68k.exceptions;

public class IllegalInstructionException extends Exception implements SimpleVectoredException {
    public IllegalInstructionException(int opword, int extPc) {
        super(String.format(
                "<[IllegalInstructionException]> 0x%04X (pc=0x%06X) does not correspond to a valid instruction opcode",
                opword & 0xFFFF,
                extPc & 0xFFFFFF));
    }

    @Override
    public ExceptionVector exceptionVector() {
        return ExceptionVector.ILLEGAL_INSTRUCTION;
    }
}
