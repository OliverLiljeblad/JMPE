package com.JMPE.cpu.m68k.exceptions;

/**
 * Address error raised by a control transfer that tries to load an odd program counter.
 */
public final class ProgramCounterAddressErrorException extends AddressErrorException {
    private final int savedProgramCounter;

    public ProgramCounterAddressErrorException(int programCounter) {
        this(programCounter, programCounter - Integer.BYTES);
    }

    public ProgramCounterAddressErrorException(int programCounter, int savedProgramCounter) {
        super(programCounter, FaultAccessType.READ);
        this.savedProgramCounter = savedProgramCounter;
    }

    @Override
    public boolean instructionAccess() {
        return true;
    }

    @Override
    public int savedProgramCounter(int defaultSavedProgramCounter) {
        return savedProgramCounter;
    }
}
