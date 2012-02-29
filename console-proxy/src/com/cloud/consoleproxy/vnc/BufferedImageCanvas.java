package com.cloud.consoleproxy.vnc;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A <code>BuffereImageCanvas</code> component represents frame buffer image on the
 * screen. It also notifies its subscribers when screen is repainted.
 */
public class BufferedImageCanvas extends Canvas {
  private static final long serialVersionUID = 1L;

   // Offline screen buffer
  private BufferedImage offlineImage;
  
  // Cached Graphics2D object for offline screen buffer
  private Graphics2D graphics;

  private PaintNotificationListener listener;

  public BufferedImageCanvas(PaintNotificationListener listener, int width, int height) {
    super();
    this.listener = listener;

    setBackground(Color.black);
    
    setFocusable(true);

    // Don't intercept TAB key
    setFocusTraversalKeysEnabled(false);

    setCanvasSize(width, height);
  }

  public void setCanvasSize(int width, int height) {
    this.offlineImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    graphics = offlineImage.createGraphics();

    setSize(offlineImage.getWidth(), offlineImage.getHeight());
  }

  @Override
  public void update(Graphics g) {
    // Call paint() directly, without clearing screen first
    paint(g);
  }

  @Override
  public void paint(Graphics g) {
    // Only part of image, requested with repaint(Rectangle), will be
    // painted on screen.
    g.drawImage(offlineImage, 0, 0, this);

    // Notify server that update is painted on screen
    listener.imagePaintedOnScreen();
  }

  public BufferedImage getOfflineImage() {
    return offlineImage;
  }

  public Graphics2D getOfflineGraphics() {
    return graphics;
  }

}