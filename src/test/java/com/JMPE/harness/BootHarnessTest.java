package com.JMPE.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.JMPE.machine.MacPlusMachine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootHarnessTest {
    @TempDir
    Path tempDir;

    @Test
    void trimsLocalMacPlusRomFileToTheReal128KilobytePayload() throws IOException {
        byte[] bytes = new byte[0x0002_0002];
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x20;
        bytes[3] = 0x00;
        bytes[4] = 0x00;
        bytes[5] = 0x40;
        bytes[6] = 0x01;
        bytes[7] = 0x00;
        bytes[0x0001_FFFE] = 0x12;
        bytes[0x0001_FFFF] = 0x34;
        bytes[0x0002_0000] = (byte) 0xDE;
        bytes[0x0002_0001] = (byte) 0xAD;

        Path romPath = tempDir.resolve("Mac-Plus.ROM");
        Files.write(romPath, bytes);

        MacPlusMachine machine = BootHarness.loadLocalMacPlusRom(romPath);

        assertEquals(0x0002_0000, machine.rom().size());
        assertEquals(0x1234, machine.rom().readWord(BootHarness.DEFAULT_ROM_BASE + 0x0001_FFFE));
    }
}
