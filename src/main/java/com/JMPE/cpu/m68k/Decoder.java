package com.JMPE.cpu.m68k;

import com.JMPE.bus.Bus;
import com.JMPE.util.Bits;

public final class Decoder {

    /**
     * Decode opcode to dispatch table index.
     * @param opcode 16-bit instruction word from PC
     * @return index into DispatchTable.TABLE (0-65535)
     *
     * CONSTRAINT: Must not allocate. Must be O(1) or minimal bit ops.
     */
    public int decode(Bus bus, int programCounter) {
        int opcode = bus.read(programCounter);
        return dispatch.handle(opcode);
    }

    /**
     * Extract addressing mode metadata for execute phase.
     * @return packed int: [mode:3][reg:3][size:2][unused:8]
     * Avoids object allocation for common metadata.
     */
    public static  int extract(int opcode) {
        int addressingMode = Bits.extract(opcode, 3, 6);
        return -1;
    }

    public static enum WordMetaData {
        ADDRESSING_MODE,
        REGISTER,
        SIZE,
        UNUSED;
    };
    public static int extract(int opcode, WordMetaData choice) {
        switch (choice) {
            case ADDRESSING_MODE:
                return  (opcode >> 3) & 0x7;
            case REGISTER:
                return opcode & 0x7;
            case SIZE:
                return (opcode >> 6) & 0x3;
            default:
                return -1;
        }
    }

    public static int countExtensionWords(int opcode) {
        int mode = extract(opcode, ADDRESSING_MODE);
        return -1;
    }

    private Decoder() {}
}
