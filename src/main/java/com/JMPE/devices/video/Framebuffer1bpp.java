package com.JMPE.devices.video;

import com.JMPE.bus.MemoryRegion;
import com.JMPE.bus.Ram;

import java.util.Arrays;


/*
 * 1bpp framebuffer
 * The framebuffer occupies a fixed range in RAM and represents
 * the full-screen image as a bitmap. Because the screen is 512
 * pixels wide and 342 pixels tall, the memory footprint is
 * exactly 512 x 342 / 8 = 21,888 bytes. The video system scans
 * this memory in raster order, so what you write into that
 * region becomes the next visible screen image.
 */
public class Framebuffer1bpp {

    public final Ram ram;
    public final int base;

    public static final int WIDTH = 512;
    public static final int HEIGHT = 342;

    public final int BYTES_PER_ROW = WIDTH / 8;
    public final int BUFFER_SIZE = BYTES_PER_ROW * HEIGHT;

    public Framebuffer1bpp(Ram ram, int base) {
        this.ram = ram;
        this.base = base;
    }

    public int pixelAt(int x, int y) {
        int byteOffset = y * BYTES_PER_ROW + (x / 8);
        int bit = 7 - (x % 8);
        return (ram.readByte(base + byteOffset) >>> bit) & 1;
    }

    public byte[] copyBytes() {
        byte[] bytes = new byte[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; ++i) {
            bytes[i] = (byte) ram.readByte(base + i);
        }
        return bytes;
    }
}
