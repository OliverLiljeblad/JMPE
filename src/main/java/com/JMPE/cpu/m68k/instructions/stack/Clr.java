
// CLR - Clear Destination 
// Syntax: CLR <ea>
// Sizes: Byte (.B), Word (.W), Long (.L)


public class CLR {

    private final CPU cpu;

    public CLR(CPU cpu) {
        this.cpu = cpu;
    }

    public void execute(int eaMode, int eaReg, int size) {
        // Uncomment for bus-accurate read-before-write behaviour:
        // cpu.readOperand(eaMode, eaReg, size);

        cpu.writeOperand(eaMode, eaReg, size, 0);

        cpu.ccr.n = false;  // ← adjust to match your CCR class
        cpu.ccr.z = true;
        cpu.ccr.v = false;
        cpu.ccr.c = false;
    }
}