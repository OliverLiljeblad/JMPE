package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

class DecoderDispatchReachabilityTest {
    @Test
    void everyBuiltInDispatchHandlerIsReachableFromDecoder() {
        Decoder decoder = new Decoder();
        DispatchTable dispatchTable = new DispatchTable();
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 0x4000));

        EnumSet<Opcode> expected = EnumSet.noneOf(Opcode.class);
        for (Opcode opcode : Opcode.values()) {
            if (dispatchTable.hasHandler(opcode)) {
                expected.add(opcode);
            }
        }

        EnumSet<Opcode> reachable = EnumSet.noneOf(Opcode.class);
        for (int opword = 0; opword <= 0xFFFF; opword++) {
            try {
                Opcode opcode = decoder.decode(opword, bus, 0x1002).opcode();
                if (dispatchTable.hasHandler(opcode)) {
                    reachable.add(opcode);
                }
            } catch (IllegalInstructionException | RuntimeException ignored) {
            }
        }

        assertEquals(expected, reachable);
    }
}
