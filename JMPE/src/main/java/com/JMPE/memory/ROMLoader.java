package com.JMPE.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads a Mac Plus ROM image from disk into a byte[].
 * Pass the result straight to new ROM(bytes).
 */
public class ROMLoader {
   private ROMLoader() {}

    /**
     * Load a ROM file and return its raw bytes.
     *
     * @param path  path to the .rom file
     * @return      raw ROM bytes (must be exactly 128KB for Mac Plus)
     * @throws IOException              if the file can't be read
     * @throws IllegalArgumentException if the file is the wrong size
     */
    public static byte[] load(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if  (bytes.length != 128 * Unit.KB.the()) {
            throw new IllegalArgumentException(
                    "ROMLoader: Expected 131072 bytes (128KB), got " + bytes.length + " in " + path
            );
        }
        return bytes;
    }

    public static byte[] load(String filename) throws IOException {
        return load(Paths.get(filename));
    }
}
