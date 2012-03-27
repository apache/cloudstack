package com.cloud.consoleproxy.vnc.packet.server;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

public class CopyRect extends AbstractRect {

  private final int srcX, srcY;

  public CopyRect(int x, int y, int width, int height, DataInputStream is) throws IOException {
    super(x, y, width, height);

    srcX = is.readUnsignedShort();
    srcY = is.readUnsignedShort();
  }

  @Override
  public void paint(BufferedImage image, Graphics2D graphics) {
    graphics.copyArea(srcX, srcY, width, height, x - srcX, y - srcY);
  }
}
