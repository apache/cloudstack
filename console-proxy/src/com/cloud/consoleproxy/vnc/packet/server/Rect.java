package com.cloud.consoleproxy.vnc.packet.server;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public interface Rect {
  
  void paint(BufferedImage offlineImage, Graphics2D graphics);

  int getX();
  int getY();
  int getWidth();
  int getHeight();
}
