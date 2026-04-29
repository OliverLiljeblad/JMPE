package com.JMPE.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class RendererPanel extends JPanel {

    private final int width;
    private final int height;

    private final int scale;

    private BufferedImage image;

    RendererPanel(int frameWidth, int frameHeight, int scale) {
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be at least 1");
        }

        if (frameWidth < 1 || frameHeight < 1) {
            throw new IllegalArgumentException("frame dimensions must be at least 1x1, got " + frameWidth + "x" + frameHeight);
        }

        this.width = frameWidth;
        this.height = frameHeight;
        this.scale = scale;

        //TODO: Change to be BufferedImage.TYPE_BYTE_BINARY once all bugs are fixed
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        setPreferredSize(new Dimension(width * scale, height * scale));
        setFocusable(true);
    }

    public void update(byte[] frameBytes) {
        byte[] snapshot = Arrays.copyOf(frameBytes, frameBytes.length);
        Renderer.render1bpp(snapshot, image, width, height);
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(image, 0, 0, width * scale, height * scale, null);
    }
}
