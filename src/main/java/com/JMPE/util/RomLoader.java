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
        return new Rom(baseAddress, bytes);
    }
}
