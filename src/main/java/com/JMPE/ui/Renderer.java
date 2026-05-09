package com.JMPE.ui;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class Renderer {

    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;

    private Renderer() {}

    public static void render1bpp(byte[] pixels, BufferedImage target, int width, int height) {
        Objects.requireNonNull(pixels, "framebuffer must not be null");
        Objects.requireNonNull(target, "target image must not be null");
        if (target.getWidth() != width || target.getHeight() != height) {
            throw new IllegalArgumentException("target image must be " + width + "x" + height);
        }

        int bytesPerRow = width / 8;
        int requiredBytes = bytesPerRow * height;
        if (pixels.length != requiredBytes) {
            throw new IllegalArgumentException("framebuffer must be " + requiredBytes + " bytes, got" + pixels.length);
        }

        for (int y = 0; y < height; y++) {
            int rowOffset = y * bytesPerRow;

            for (int x = 0; x < width; x++) {
                int byteIndex = rowOffset + (x / 8);
                int bitIndex = 7 - (x % 8);
                int bit = (pixels[byteIndex] >>> bitIndex) & 1;

                target.setRGB(x, y, bit == 0 ? WHITE : BLACK);
            }
        }
    }
}
