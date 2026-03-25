package com.JMPE.util;

import com.JMPE.bus.Rom;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Centralized ROM loading and light validation for 68k boot requirements.
 */
public final class RomLoader {
    private static final int MIN_RESET_VECTOR_BYTES = 8;
    private static final int WORD_BYTES = 2;

    private RomLoader() {
    }

    public static Rom load(Path romPath) throws IOException {
        return load(romPath, 0x0000_0000);
    }

    public static Rom load(Path romPath, int baseAddress) throws IOException {
        return fromBytes(Files.readAllBytes(romPath), baseAddress);
    }

    public static Rom fromBytes(byte[] bytes, int baseAddress) {
        if (bytes == null || bytes.length < MIN_RESET_VECTOR_BYTES) {
            throw new IllegalArgumentException(
                "ROM must contain at least 8 bytes for initial SSP and PC vectors");
        }
        if ((bytes.length % WORD_BYTES) != 0) {
            throw new IllegalArgumentException("ROM length must be even for 16-bit 68000 bus access");
        }
        if ((baseAddress & 1) != 0) {
            throw new IllegalArgumentException("ROM base address must be word-aligned");
        }
        return new Rom(baseAddress, bytes);
    }
}
