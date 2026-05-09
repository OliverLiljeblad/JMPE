package com.JMPE.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.bus.Rom;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RomLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsRomFromDiskAndRespectsBaseAddress() throws IOException {
        Path romPath = tempDir.resolve("macplus.rom");
        Files.write(romPath, new byte[] {
            0x00, 0x10, 0x00, 0x20, 0x12, 0x34, 0x56, 0x78
        });

        Rom rom = RomLoader.load(romPath, 0x0040_0000);

        assertEquals(0x0040_0000, rom.base());
        assertEquals(0x0010, rom.readWord(0));
        assertEquals(0x1234_5678L, rom.readLong(4));
    }

    @Test
    void rejectsRomWithoutResetVectors() {
        assertThrows(IllegalArgumentException.class, () ->
            RomLoader.fromBytes(new byte[] {0x01, 0x02, 0x03, 0x04}, 0x0)
        );
    }

    @Test
    void rejectsMisalignedRomInputs() {
        assertThrows(IllegalArgumentException.class, () ->
            RomLoader.fromBytes(new byte[] {
                0x00, 0x10, 0x00, 0x20, 0x12, 0x34, 0x56
            }, 0x0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            RomLoader.fromBytes(new byte[] {
                0x00, 0x10, 0x00, 0x20, 0x12, 0x34, 0x56, 0x78
            }, 0x1)
        );
    }
}
