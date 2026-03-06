package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.instructions.*;

public class DispatchTable {
    //NOTE: Array of Function References
    
    private final Op[] handlers = new Op[];

    public DispatchTable() {
        this.init_table();
    }

    private void init_table() {

        Op illegal = new Op.Illegal();
        int addressOfFirstInstruction = 0x10000;
        for (int i = 0; i < addressOfFirstInstruction; ++i) {
            handlers[i] = illegal;
        }

        Op move = new Move();
    }
}
