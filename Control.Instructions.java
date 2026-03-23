// 
// CHK - Check Register Against Bounds

// Syntax: CHK <ea>, Dn
// Sizes: Word (.W) and Long (.L) [.L only on 68020+]


using System.ComponentModel;
using System.Security.Cryptography.X509Certificates;

public void executeCHK(int eaMode, int eaReg, int dn, int size) {

    int upperBound = readOperand (eaMode, eaReg, size);
    int regVal = cpu.getDataregister(dn);

    // Sign-extend the upper bound if it's a word operation
    if (size == 2) { // Size of word
        upperBound = (short) upperBound; // Sign-extend to 32 bits
        regVal = (short) regVal; // Sign-extend the register value as well    
    }   

    if (regVal < 0 || regVal > upperBound) {
        cpu.ccr.SetN(false);
        cpu.raiseException(6); // Raise exception #6 (CHK)

    }

}

// CLR - Clear Destination 
// Syntax: CLR <ea>
// Sizes: Byte (.B), Word (.W), Long (.L)


public void executeCLR(int eaMode, int eaReg, int size) {

    writeOperand(eaMode, eaReg, 0, size); // Write zero to the destination operand

    // Clear condition codes
    cpu.ccr.SetN(false);
    cpu.ccr.SetZ(true); // Result is zero
    cpu.ccr.SetV(false);
    cpu.ccr.SetC(false);



// CMP - Compare
// Syntax: CMP <ea>, Dn
// Sizes: Byte (.B), Word (.W), Long (.L)

}
public void executeCMP(int eaMode, int eaReg, int dn, int size){
    int operandValue = readOperand(eaMode, eaReg, size);
    int regValue = cpu.getDataregister(dn);

    int mask, signBit;
    switch (size)
    {
            case 1: // Byte
                mask = 0xFF;
                signBit = 0x80;
                break;
            case 2: // Word
                mask = 0xFFFF;
                signBit = 0x8000;
                break;
            case 4: // Long
                mask = 0xFFFFFFFF;
                signBit = 0x80000000;
                break;
            default:
                throw new ArgumentException("Invalid size");
    }

    src &= mask;
    regVal &= mask;
    int result = regVal - operandValue;
    // Set condition codes
    cpu.ccr.SetN((result & signBit) != 0); // Negative flag 
    cpu.ccr.SetZ(result == 0); // Zero flag
    cpu.ccr.SetV(((regVal ^ operandValue) & (regVal ^ result)) (& signBit) != 0); // Overflow flag
    cpu.ccr.SetC(regVal < operandValue); // Carry flag      

}

private int signExtendWord(int value, int size) {
    int signBit = 1 << ((size * 8) - 1);
    return (value ^ signBit) - signBit; // Sign-extend the value

    }

    // Size Constants
    private static int SIZE_BYTE = 0;    
    private static int SIZE_WORD = 1;
    private static int SIZE_LONG = 2;


    