package com.JMPE.cpu.m68k.dispatch;

import java.util.Arrays;
//import com.JMPE.cpu.m68k.instructions.data.Move;

public class DispatchTable {
    //NOTE: Array of Function References
    private static final Op[] handlers = new Op[0x10000];

    public DispatchTable() {
        this.init_table();
    }

    private void init_table() {
        Op illegal = new Op.Illegal();
        int addressOfFirstInstruction = 0x10000;
        Arrays.fill(handlers, 0, addressOfFirstInstruction, illegal);

        Op move = new instructions.data.Move();
        //TODO: DO other things
    }

    public static Op at(int opcode) {
        return handlers[opcode];
    }
}
