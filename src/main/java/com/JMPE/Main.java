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

            DesktopWindow window = new DesktopWindow("JMPE; Mac Plus Emulator", 512, 342, 2);
            window.show();

            // Drive the CPU on its own thread; repaint the framebuffer at ~60 Hz
            // from a Swing timer so we can watch the boot icon appear/blink.
            Thread cpuThread = new Thread(() -> {
                try {
                    while (true) {
                        machine.step();
                    }
                } catch (IllegalInstructionException e) {
                    IO.println("HALT pc=" + String.format("0x%08X",
                        machine.cpu().registers().programCounter()));
                }
            }, "cpu");
            cpuThread.setDaemon(true);
            cpuThread.start();

            javax.swing.Timer timer = new javax.swing.Timer(16, e -> {
                if (machine.videoController() != null) {
                    window.renderPanel().update(machine.videoController().getFrame());
                }
            });
            timer.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
