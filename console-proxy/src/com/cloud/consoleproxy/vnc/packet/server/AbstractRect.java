package com.cloud.consoleproxy.vnc.packet.server;

public abstract class AbstractRect implements Rect {

  protected final int x;
  protected final int y;
  protected final int width;
  protected final int height;

  public AbstractRect(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  @Override
  public int getX() {
    return x;
  }

  @Override
  public int getY() {
    return y;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

}