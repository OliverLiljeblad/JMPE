// 
// CHK - Check Register Against Bounds

// Syntax: CHK <ea>, Dn
// Sizes: Word (.W) and Long (.L) [.L only on 68020+]


public class CHK {

    private final CPU cpu;

    public CHK(CPU cpu) {
        this.cpu = cpu;
    }

    public void execute(int eaMode, int eaReg, int dn, int size) {
        int upperBound = cpu.readOperand(eaMode, eaReg, size);
        int regVal     = cpu.getDataRegister(dn);

        if (size == SIZE_WORD) {
            upperBound = signExtendWord(upperBound);
            regVal     = signExtendWord(regVal);
        }

        if (regVal < 0) {
            cpu.ccr.n = true;
            cpu.triggerException(6);
        } else if (regVal > upperBound) {
            cpu.ccr.n = false;
            cpu.triggerException(6);
        }
    }

    private int signExtendWord(int value) {
        return (value << 16) >> 16;
    }

    private static final int SIZE_WORD = 1;
    private static final int SIZE_LONG = 2;
}
