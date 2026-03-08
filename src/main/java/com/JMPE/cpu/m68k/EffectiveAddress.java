package com.JMPE.cpu.m68k;

public final class EffectiveAddress {

    public static int resolve(AddressingMode mode, int register, int size, CPU cpu, Bus bus) {
        switch (mode) {
            case DATA_REGISTER_DIRECT -> return cpu.getDataRegister(register, size);
            case POST_INCREMENT -> return bus.read(cpu.getPostIncrementAddresingRegister(register, size), size);
            default: return -1;
        }
    }
}
