package com.JMPE.devices.video;

import com.JMPE.bus.Ram;
import com.JMPE.ui.FrameProvider;
import com.JMPE.util.Unit;

public class VideoController implements FrameProvider {

    private static final int MAIN_FRAIMBUFFER = 0;
    private static final int ALTERNATE_FRAIMBUFFER = 1;

    private final Ram ram;
    private Framebuffer1bpp[] framebuffers = new Framebuffer1bpp[2];

    private int index = MAIN_FRAIMBUFFER;

    public VideoController(Ram ram) {
        this.ram = ram;
        // references: https://www.osdata.com/system/physical/memmap.htm
        switch (ram.size()) {
            case 128 * Unit.KB -> {
                int ScrnBase = 0x1A700;
                int ScrnBaseAlt = 0x12700;
                framebuffers = createFrameBuffers(ram, ScrnBase, ScrnBaseAlt);
            }
            case 512 * Unit.KB -> {
                int ScrnBase = 0x7A700;
                int ScrnBaseAlt = 0x72700;
                framebuffers = createFrameBuffers(ram, ScrnBase, ScrnBaseAlt);
            }
            case 1 * Unit.MB -> {
                int ScrnBase = 0xFA700;
                int ScrnBaseAlt = 0xF2700;
                framebuffers = createFrameBuffers(ram, ScrnBase, ScrnBaseAlt);
            }
            case 2 * Unit.MB -> {
                int ScrnBase = 0x1FA700;
                int ScrnBaseAlt = 0x1F2700;
                framebuffers = createFrameBuffers(ram, ScrnBase, ScrnBaseAlt);
            }
            //NOTE: We currently dont support 2.5MBRAM
            case 4 * Unit.MB -> {
                int ScrnBase = 0x3FA700;
                int ScrnBaseAlt = 0x3F2700;
                framebuffers = createFrameBuffers(ram, ScrnBase, ScrnBaseAlt);
            }
            default -> throw new IllegalArgumentException(
                String.format("Video controller does not support RAM size <%d>. " +
                    "Consider using 128k, 512k, 1MB, 2MB, 2.5MB, or 4MB RAM"));
        }

    }

    private static Framebuffer1bpp[] createFrameBuffers(Ram ram, int mainAddress, int alternateAddress) {
        return new Framebuffer1bpp[]{
            new Framebuffer1bpp(ram, mainAddress),
            new Framebuffer1bpp(ram, alternateAddress)
        };
    }

    public Framebuffer1bpp current() { return framebuffers[index]; }

    public byte[] copyBytes() { return current().copyBytes(); }

    public void selectMain() { index = MAIN_FRAIMBUFFER; }
    public void selectAlternate() { index = ALTERNATE_FRAIMBUFFER; }

    public void select(int index) {
        if (index < 0 || index >= framebuffers.length) {
            throw new IllegalArgumentException("Invalid framebuffer index");
        }
        this.index = index;
    }

    public void toggle() { index = (index + 1) % framebuffers.length; }

    @Override
    public int width() {
        return Framebuffer1bpp.WIDTH;
    }

    @Override
    public int height() {
        return Framebuffer1bpp.HEIGHT;
    }

    @Override
    public byte[] getFrame() {
        return copyBytes();
    }
}
