package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class DecoderPhaseThreeRemainingTest {
    private static final int INSTRUCTION_PC = 0x1000;
    private static final int EXTENSION_PC = INSTRUCTION_PC + 2;

    private final Decoder decoder = new Decoder();

    @Test
    void decodesDivideForms() throws IllegalInstructionException {
        assertDecoded(0x80C0, Opcode.DIVU, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(0));
        assertDecoded(0x81C0, Opcode.DIVS, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(0));
    }

    @Test
    void decodesSbcdRegisterAndPredecrementForms() throws IllegalInstructionException {
        assertDecoded(0x8503, Opcode.SBCD, Size.BYTE, EffectiveAddress.dataReg(3), EffectiveAddress.dataReg(2));
        assertDecoded(0x850B, Opcode.SBCD, Size.BYTE,
            EffectiveAddress.addrRegIndPreDec(3), EffectiveAddress.addrRegIndPreDec(2));
    }

    @Test
    void decodesSubaAndSubxForms() throws IllegalInstructionException {
        assertDecoded(0x90C0, Opcode.SUBA, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(0));
        assertDecoded(0x91C0, Opcode.SUBA, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(0));
        assertDecoded(0x9704, Opcode.SUBX, Size.BYTE, EffectiveAddress.dataReg(4), EffectiveAddress.dataReg(3));
        assertDecoded(0x970C, Opcode.SUBX, Size.BYTE,
            EffectiveAddress.addrRegIndPreDec(4), EffectiveAddress.addrRegIndPreDec(3));
    }

    @Test
    void decodesCmpaAndCmpmForms() throws IllegalInstructionException {
        assertDecoded(0xB3CA, Opcode.CMPA, Size.LONG, EffectiveAddress.addrReg(2), EffectiveAddress.addrReg(1));
        assertDecoded(0xB70D, Opcode.CMPM, Size.BYTE,
            EffectiveAddress.addrRegIndPostInc(5), EffectiveAddress.addrRegIndPostInc(3));
    }

    @Test
    void decodesMultiplyAbcdAndExgForms() throws IllegalInstructionException {
        assertDecoded(0xC0C0, Opcode.MULU, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(0));
        assertDecoded(0xC1C0, Opcode.MULS, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.dataReg(0));
        assertDecoded(0xC503, Opcode.ABCD, Size.BYTE, EffectiveAddress.dataReg(3), EffectiveAddress.dataReg(2));
        assertDecoded(0xC50B, Opcode.ABCD, Size.BYTE,
            EffectiveAddress.addrRegIndPreDec(3), EffectiveAddress.addrRegIndPreDec(2));
        assertDecoded(0xC342, Opcode.EXG, Size.LONG, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2));
        assertDecoded(0xC54B, Opcode.EXG, Size.LONG, EffectiveAddress.addrReg(2), EffectiveAddress.addrReg(3));
        assertDecoded(0xC38D, Opcode.EXG, Size.LONG, EffectiveAddress.dataReg(1), EffectiveAddress.addrReg(5));
    }

    @Test
    void decodesAddaAndAddxForms() throws IllegalInstructionException {
        assertDecoded(0xD0C0, Opcode.ADDA, Size.WORD, EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(0));
        assertDecoded(0xD1C0, Opcode.ADDA, Size.LONG, EffectiveAddress.dataReg(0), EffectiveAddress.addrReg(0));
        assertDecoded(0xD704, Opcode.ADDX, Size.BYTE, EffectiveAddress.dataReg(4), EffectiveAddress.dataReg(3));
        assertDecoded(0xD70C, Opcode.ADDX, Size.BYTE,
            EffectiveAddress.addrRegIndPreDec(4), EffectiveAddress.addrRegIndPreDec(3));
    }

    @Test
    void decodesRoxRegisterForms() throws IllegalInstructionException {
        assertDecoded(0xE292, Opcode.ROXR, Size.LONG, EffectiveAddress.immediate(1), EffectiveAddress.dataReg(2));
        assertDecoded(0xE332, Opcode.ROXL, Size.BYTE, EffectiveAddress.dataReg(1), EffectiveAddress.dataReg(2));
    }

    @Test
    void decodesMemoryShiftAndRotateForms() throws IllegalInstructionException {
        assertDecoded(0xE0D0, Opcode.ASR, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE1D0, Opcode.ASL, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE2D0, Opcode.LSR, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE3D0, Opcode.LSL, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE4D0, Opcode.ROXR, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE5D0, Opcode.ROXL, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE6D0, Opcode.ROR, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
        assertDecoded(0xE7D0, Opcode.ROL, Size.WORD, EffectiveAddress.none(), EffectiveAddress.addrRegInd(0));
    }

    @Test
    void rejectsInvalidMemoryShiftRegisterDestination() {
        assertThrows(IllegalInstructionException.class, () -> decoder.decode(0xE0C0, null, EXTENSION_PC));
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
