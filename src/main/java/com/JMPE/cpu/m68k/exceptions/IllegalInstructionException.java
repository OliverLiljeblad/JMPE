package com.JMPE.cpu.m68k.exceptions;

public class IllegalInstructionException extends RuntimeException  {
    public IllegalInstructionException(int opcode) {
        String message = "Illegal instruction code " + opcode;
        super(message);
    }
}
