package com.JMPE;

import com.JMPE.bus.Ram;
import com.JMPE.bus.Rom;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.machine.MacPlusMachine;
import com.JMPE.ui.DesktopWindow;
import com.JMPE.util.RomLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String DEFAULT_ROM_PATH = "src/test/java/com/JMPE/integration/roms/Mac-Plus.ROM";
    private static final int MAC_PLUS_ROM_BASE = 0x0040_0000;
    private static final int RAM_BASE = 0x0000_0000;
    // Real Mac Plus tops out at 1 MB of RAM. The ROM RAM-test loop iterates
    // (RAM_SIZE / 4) longwords, so capping at 1 MB cuts that down 4x.
    private static final int RAM_SIZE = 0x0010_0000;

    public static void main(String[] args) {
        String romPath = args.length > 0 ? args[0] : DEFAULT_ROM_PATH;
        IO.println("ROM: " + Path.of(romPath).toAbsolutePath());

        try {
            Rom rom = RomLoader.load(Path.of(romPath), MAC_PLUS_ROM_BASE);
            Ram ram = new Ram(RAM_BASE, RAM_SIZE);
            MacPlusMachine machine = MacPlusMachine.bootMachine(rom, ram);

            int stepCount = 2_000_000;
            int tailSize = 200;
            java.util.ArrayDeque<String> tail = new java.util.ArrayDeque<>(tailSize + 1);
            String[] last = new String[1];
            java.util.function.Consumer<String> sink = msg -> {
                last[0] = msg;
                tail.addLast(msg);
                if (tail.size() > tailSize) tail.removeFirst();
            };

            int progressInterval = 250_000;
            for (int step = 0; step < stepCount; step++) {
                try {
                    machine.step(sink);
                    if (step > 0 && step % progressInterval == 0) {
                        IO.println("step " + step + " pc=" + String.format("0x%08X",
                            machine.cpu().registers().programCounter()));
                    }
                } catch (IllegalInstructionException exception) {
                    IO.println("HALT at step " + step
                        + " pc=" + String.format("0x%08X", machine.cpu().registers().programCounter()));
                    for (String s : tail) IO.println(s);
                    throw new RuntimeException(exception);
                }
            }
            IO.println("Completed " + stepCount + " steps successfully.");
            IO.println("Final: " + last[0]);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        DesktopWindow window = new DesktopWindow("JMPE; Mac Plus Emulator", 512, 342, 2);
//        window.show();
    }
}
