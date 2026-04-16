package com.JMPE;

import com.JMPE.bus.Ram;
import com.JMPE.bus.Rom;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.machine.MacPlusMachine;
import com.JMPE.util.RomLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String DEFAULT_ROM_PATH = "src/test/java/com/JMPE/integration/roms/Mac-Plus.ROM";
    private static final int MAC_PLUS_ROM_BASE = 0x0040_0000;
    private static final int RAM_BASE = 0x0000_0000;
    private static final int RAM_SIZE = 0x0040_0000;

    public static void main(String[] args) {
        String romPath = args.length > 0 ? args[0] : DEFAULT_ROM_PATH;
        IO.println("ROM: " + Path.of(romPath).toAbsolutePath());

        try {
            Rom rom = RomLoader.load(Path.of(romPath), MAC_PLUS_ROM_BASE);
            Ram ram = new Ram(RAM_BASE, RAM_SIZE);
            MacPlusMachine machine = MacPlusMachine.bootMachine(rom, ram);

            int stepCount = 500_000;
            List<M68kCpu.StepReport> reports = new ArrayList<>();
            List<String> logs = new ArrayList<>();

            boolean dumpedRegs = false;
            for (int step = 0; step < stepCount; step++) {
                try {
                    reports.add(machine.step(logs::add));
                    int pc = machine.cpu().registers().programCounter();
                    if (pc >= 0x400100 && pc <= 0x400130 && !dumpedRegs) {
                        dumpedRegs = true;
                        var r = machine.cpu().registers();
                        IO.println("=== REGISTER DUMP at step " + step + " pc=0x"
                                + String.format("%08X", pc) + " ===");
                        for (int i = 0; i < 8; i++)
                            IO.println("  d" + i + "=0x" + String.format("%08X", r.data(i))
                                    + "  a" + i + "=0x" + String.format("%08X", r.address(i)));
                    }
                    if (step % 50_000 == 0
                            || (pc >= 0x400100 && pc <= 0x400130 && step < 93_570)) {
                        IO.println("Step " + step + ": " + logs.getLast());
                    }
                } catch (IllegalInstructionException exception) {
                    IO.println("HALT at step " + step
                        + " pc=" + String.format("0x%08X", machine.cpu().registers().programCounter()));
                    int start = Math.max(0, logs.size() - 10);
                    for (int i = start; i < logs.size(); i++) {
                        IO.println(logs.get(i));
                    }
                    throw new RuntimeException(exception);
                }
            }
            IO.println("Completed " + stepCount + " steps successfully.");
            IO.println("Final: " + logs.getLast());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
