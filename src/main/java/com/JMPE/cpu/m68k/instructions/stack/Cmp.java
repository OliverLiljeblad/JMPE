


// CLR — Clear Operand
//
// Syntax:  CLR <ea>
// Sizes:   Byte (.B), Word (.W), Long (.L)


public class CMP {

    private final CPU cpu;

    public CMP(CPU cpu) {
        this.cpu = cpu;
    }

    public void execute(int eaMode, int eaReg, int dn, int size) {
        int src    = cpu.readOperand(eaMode, eaReg, size);
        int regVal = cpu.getDataRegister(dn);

        int mask;
        int signBit;

        switch (size) {
            case SIZE_BYTE:
                mask    = 0xFF;
                signBit = 0x80;
                break;
            case SIZE_WORD:
                mask    = 0xFFFF;
                signBit = 0x8000;
                break;
            default: // SIZE_LONG
                mask    = -1;
                signBit = Integer.MIN_VALUE;
                break;
        }

        src    = src    & mask;
        regVal = regVal & mask;

        int result = (regVal - src) & mask;

        cpu.ccr.n = (result & signBit) != 0;
        cpu.ccr.z = (result == 0);

        boolean srcNeg    = (src    & signBit) != 0;
        boolean dstNeg    = (regVal & signBit) != 0;
        boolean resultNeg = (result & signBit) != 0;

        cpu.ccr.v = (!srcNeg &&  dstNeg && !resultNeg) ||
                    ( srcNeg && !dstNeg &&  resultNeg);

        cpu.ccr.c = Integer.compareUnsigned(src, regVal) > 0;
    }

    private static final int SIZE_BYTE = 0;
    private static final int SIZE_WORD = 1;
    private static final int SIZE_LONG = 2;
}