package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class DecoderPhaseThreeTest {
    private static final int INSTRUCTION_PC = 0x1000;
    private static final int EXTENSION_PC = INSTRUCTION_PC + 2;

    private final Decoder decoder = new Decoder();

    @Test
    void decodesOrWordDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0x8240, Opcode.OR, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1));
    }

    @Test
    void decodesSubByteDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0x9200, Opcode.SUB, Size.BYTE, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1));
    }

    @Test
    void decodesCmpWordDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0xB240, Opcode.CMP, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1));
    }

    @Test
    void decodesEorWordDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0xB340, Opcode.EOR, Size.WORD, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(0));
    }

    @Test
    void decodesAndWordDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0xC240, Opcode.AND, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1));
    }

    @Test
    void decodesAddWordDataRegisterToDataRegister() throws IllegalInstructionException {
        assertDecoded(0xD240, Opcode.ADD, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(1));
    }

    @Test
    void decodesRegisterShiftAndRotateForms() throws IllegalInstructionException {
        assertDecoded(0xE300, Opcode.ASL, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(0));
        assertDecoded(0xE222, Opcode.ASR, Size.BYTE, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2));
        assertDecoded(0xEB6C, Opcode.LSL, Size.WORD, EffectiveAddress.dataReg(5), EffectiveAddress.dataReg(4));
        assertDecoded(0xE20B, Opcode.LSR, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3));
        assertDecoded(0xE31B, Opcode.ROL, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3));
        assertDecoded(0xE21B, Opcode.ROR, Size.BYTE, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(3));
    }

    private void assertDecoded(int opword,
                               Opcode opcode,
                               Size size,
                               EffectiveAddress src,
                               EffectiveAddress dst) throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(opword, null, EXTENSION_PC);

        assertEquals(opcode, decoded.opcode());
        assertEquals(size, decoded.size());
        assertEquals(src, decoded.src());
        assertEquals(dst, decoded.dst());
        assertEquals(0, decoded.extension());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }
}
