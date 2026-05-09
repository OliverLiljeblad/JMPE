package com.JMPE.ui;

import javax.swing.*;
import java.awt.*;

public class DesktopWindow {

    private final JFrame frame;
    private final RendererPanel rendererPanel;

    public DesktopWindow(String title, int width, int height, int scale) {
        this.rendererPanel = new RendererPanel(width, height, scale);

        this.frame = new JFrame(title);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLayout(new BorderLayout());
        this.frame.add(this.rendererPanel, BorderLayout.CENTER);
        this.frame.pack();
        this.frame.setVisible(true);
    }

    public void show() {
        this.frame.setVisible(true);
    }

    public RendererPanel renderPanel() {
        return rendererPanel;
    }
}
