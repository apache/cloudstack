package com.cloud.consoleproxy.vnc.packet.server;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.cloud.consoleproxy.vnc.BufferedImageCanvas;

public class FrameBufferSizeChangeRequest extends AbstractRect {
  
  private final BufferedImageCanvas canvas;

  public FrameBufferSizeChangeRequest(BufferedImageCanvas canvas, int width, int height) {
    super(0, 0, width, height);
    this.canvas=canvas;
  }

  @Override
  public void paint(BufferedImage offlineImage, Graphics2D graphics) {
    canvas.setCanvasSize(width, height);
  }

}
