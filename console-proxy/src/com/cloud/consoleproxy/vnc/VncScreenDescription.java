package com.cloud.consoleproxy.vnc;

/**
 * VncScreenDescription - contains information about remote VNC screen.
 */
public class VncScreenDescription {

  // Frame buffer size
  private int framebufferWidth = -1;
  private int framebufferHeight = -1;

  // Desktop name
  private String desktopName;

  // Bytes per pixel
  private int bytesPerPixel;

  // Indicates that screen uses format which we want to use:
  // RGB 24bit packed into 32bit little-endian int.
  private boolean rgb888_32_le = false;

  public VncScreenDescription() {
  }

  /**
   * Store information about server pixel format.
   */
  public void setPixelFormat(int bitsPerPixel, int depth, int bigEndianFlag, int trueColorFlag, int redMax, int greenMax, int blueMax, int redShift,
      int greenShift, int blueShift) {

    bytesPerPixel = (bitsPerPixel + 7) / 8;

    rgb888_32_le = (depth == 24 && bitsPerPixel == 32 && redShift == 16 && greenShift == 8 && blueShift == 0 && redMax == 255 && greenMax == 255
        && blueMax == 255 && bigEndianFlag == RfbConstants.LITTLE_ENDIAN && trueColorFlag == RfbConstants.TRUE_COLOR);
  }

  /**
   * Store information about server screen size.
   */
  public void setFramebufferSize(int framebufferWidth, int framebufferHeight) {
    this.framebufferWidth = framebufferWidth;
    this.framebufferHeight = framebufferHeight;
  }

  /**
   * Store server desktop name.
   */
  public void setDesktopName(String desktopName) {
    this.desktopName = desktopName;
  }

  // Getters for variables, as usual

  public String getDesktopName() {
    return desktopName;
  }

  public int getBytesPerPixel() {
    return bytesPerPixel;
  }

  public int getFramebufferHeight() {
    return framebufferHeight;
  }

  public int getFramebufferWidth() {
    return framebufferWidth;
  }

  public boolean isRGB888_32_LE() {
    return rgb888_32_le;
  }

}
