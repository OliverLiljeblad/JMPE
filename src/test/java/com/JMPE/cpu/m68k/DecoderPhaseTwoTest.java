package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Ram;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class DecoderPhaseTwoTest {
    private static final int INSTRUCTION_PC = 0x1000;
    private static final int EXTENSION_PC = INSTRUCTION_PC + 2;

    private final Decoder decoder = new Decoder();

    @Test
    void decodesMoveqImmediateToDataRegister() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x7680, null, EXTENSION_PC);

        assertEquals(Opcode.MOVEQ, decoded.opcode());
        assertEquals(Size.LONG, decoded.size());
        assertEquals(EffectiveAddress.immediate(0x80), decoded.src());
        assertEquals(EffectiveAddress.dataReg(3), decoded.dst());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }

    @Test
    void rejectsLine7WhenBit8IsSet() {
        assertThrows(IllegalInstructionException.class, () -> decoder.decode(0x7100, null, EXTENSION_PC));
    }

    @Test
    void decodesAddqByteToDataRegister() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x5200, null, EXTENSION_PC);

        assertEquals(Opcode.ADDQ, decoded.opcode());
        assertEquals(Size.BYTE, decoded.size());
        assertEquals(EffectiveAddress.immediate(1), decoded.src());
        assertEquals(EffectiveAddress.dataReg(0), decoded.dst());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }

    @Test
    void decodesQuickZeroEncodingAsEightForSubq() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x5180, null, EXTENSION_PC);

        assertEquals(Opcode.SUBQ, decoded.opcode());
        assertEquals(Size.LONG, decoded.size());
        assertEquals(EffectiveAddress.immediate(8), decoded.src());
        assertEquals(EffectiveAddress.dataReg(0), decoded.dst());
    }

    @Test
    void rejectsByteQuickToAddressRegister() {
        assertThrows(IllegalInstructionException.class, () -> decoder.decode(0x5208, null, EXTENSION_PC));
    }

    @Test
    void decodesDbraWithSignedWordDisplacementAndCounterRegister() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x51C8, busWithWords(EXTENSION_PC, 0xFFFC), EXTENSION_PC);

        assertEquals(Opcode.DBcc, decoded.opcode());
        assertEquals(Size.UNSIZED, decoded.size());
        assertEquals(EffectiveAddress.immediate(-4), decoded.src());
        assertEquals(EffectiveAddress.dataReg(0), decoded.dst());
        assertEquals(0x1, decoded.extension());
        assertEquals(INSTRUCTION_PC + 4, decoded.nextPc());
    }

    @Test
    void decodesSneToDataRegisterWithConditionPayload() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x56C0, null, EXTENSION_PC);

        assertEquals(Opcode.Scc, decoded.opcode());
        assertEquals(Size.BYTE, decoded.size());
        assertEquals(EffectiveAddress.none(), decoded.src());
        assertEquals(EffectiveAddress.dataReg(0), decoded.dst());
        assertEquals(0x6, decoded.extension());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }

    @Test
    void rejectsSccToImmediateDestination() {
        assertThrows(IllegalInstructionException.class, () -> decoder.decode(0x56FC, null, EXTENSION_PC));
    }

    @Test
    void decodesBraWithByteDisplacement() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x60FE, null, EXTENSION_PC);

        assertEquals(Opcode.BRA, decoded.opcode());
        assertEquals(Size.BYTE, decoded.size());
        assertEquals(EffectiveAddress.immediate(-2), decoded.src());
        assertEquals(EffectiveAddress.none(), decoded.dst());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }

    @Test
    void decodesBsrWithSignedWordDisplacement() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x6100, busWithWords(EXTENSION_PC, 0xFFFC), EXTENSION_PC);

        assertEquals(Opcode.BSR, decoded.opcode());
        assertEquals(Size.WORD, decoded.size());
        assertEquals(EffectiveAddress.immediate(-4), decoded.src());
        assertEquals(EffectiveAddress.none(), decoded.dst());
        assertEquals(INSTRUCTION_PC + 4, decoded.nextPc());
    }

    @Test
    void decodesBneAsGenericBccWithConditionPayload() throws IllegalInstructionException {
        DecodedInstruction decoded = decoder.decode(0x6602, null, EXTENSION_PC);

        assertEquals(Opcode.BCC, decoded.opcode());
        assertEquals(Size.BYTE, decoded.size());
        assertEquals(EffectiveAddress.immediate(2), decoded.src());
        assertEquals(EffectiveAddress.none(), decoded.dst());
        assertEquals(0x6, decoded.extension());
        assertEquals(EXTENSION_PC, decoded.nextPc());
    }

    @Test
    void rejectsLongBranchSentinelOn68000() {
        assertThrows(IllegalInstructionException.class, () -> decoder.decode(0x60FF, null, EXTENSION_PC));
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
