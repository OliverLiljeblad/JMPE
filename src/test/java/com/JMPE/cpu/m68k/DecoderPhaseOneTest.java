package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class DecoderPhaseOneTest {
    private static final int INSTRUCTION_PC = 0x1000;
    private static final int EXTENSION_PC = INSTRUCTION_PC + 2;

    private final Decoder decoder = new Decoder();

    @Test
    void decodesMovepMemoryToRegisterForms() throws IllegalInstructionException {
        DecodedInstruction word = decoder.decode(0x0109, busWithWords(EXTENSION_PC, 0x0010), EXTENSION_PC);
        DecodedInstruction longWord = decoder.decode(0x0149, busWithWords(EXTENSION_PC, 0x0010), EXTENSION_PC);

        assertDecoded(word, Size.WORD, EffectiveAddress.addrRegIndDisp(1, 0x0010), EffectiveAddress.dataReg(0));
        assertDecoded(longWord, Size.LONG, EffectiveAddress.addrRegIndDisp(1, 0x0010), EffectiveAddress.dataReg(0));
    }

    @Test
    void decodesMovepRegisterToMemoryForms() throws IllegalInstructionException {
        DecodedInstruction word = decoder.decode(0x0189, busWithWords(EXTENSION_PC, 0x0010), EXTENSION_PC);
        DecodedInstruction longWord = decoder.decode(0x01C9, busWithWords(EXTENSION_PC, 0x0010), EXTENSION_PC);

        assertDecoded(word, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.addrRegIndDisp(1, 0x0010));
        assertDecoded(longWord, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.addrRegIndDisp(1, 0x0010));
    }

    private static void assertDecoded(DecodedInstruction decoded, Size size, EffectiveAddress src, EffectiveAddress dst) {
        assertEquals(Opcode.MOVEP, decoded.opcode());
        assertEquals(size, decoded.size());
        assertEquals(src, decoded.src());
        assertEquals(dst, decoded.dst());
        assertEquals(0, decoded.extension());
        assertEquals(INSTRUCTION_PC + 4, decoded.nextPc());
    }

    private static AddressSpace busWithWords(int baseAddress, int... words) {
        AddressSpace bus = new AddressSpace();
        bus.addRegion(new Ram(0x1000, 0x2000));
        for (int index = 0; index < words.length; index++) {
            bus.writeWord(baseAddress + (index * 2), words[index]);
        }
        return bus;
    }
}
