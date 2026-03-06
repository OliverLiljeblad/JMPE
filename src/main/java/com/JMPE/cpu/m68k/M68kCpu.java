package com.JMPE.cpu.m68k;

public class M68kCpu implements Resettable {
    private final Registers register;
    private final Bus bus;
    private final Decoder decoder;
    private final ExceptionDispather;

    private long cycles;

    public void step() {

        int programCounter = registers.getProgramCounter();
        Op next = decoder.decode(bus, programCounter);

        try {
            op.execute(this, bus, registers);
        } catch (AddressError | BusError exception) {
            exeptions.dispatch(exception, registers, bus);
        }
    }
}
