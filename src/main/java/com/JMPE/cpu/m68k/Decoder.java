package com.JMPE.cpu.m68k;

import com.JMPE.bus.Bus;

public class Decoder {
    private final DispatchTable dispatch;

    public Decoder(DispatchTable dispatchTable) {
        this.dispatch = dispatchTable;
    }

    public Op decode(int programCounter) {
        int opcode = Bus.read(programCounter);
        return dispatch.handle(opcode);
    }

    //TODO: implement methods for extracting bits
}
