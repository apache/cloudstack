package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.VncScreenDescription;

public class SetPixelFormatPacket implements ClientPacket {

  private final int bitsPerPixel, depth, bigEndianFlag, trueColourFlag, redMax, greenMax, blueMax, redShift, greenShift, blueShift;
  
  private final VncScreenDescription screen;

  public SetPixelFormatPacket(VncScreenDescription screen, int bitsPerPixel, int depth, int bigEndianFlag, int trueColorFlag, int redMax, int greenMax,
      int blueMax, int redShift, int greenShift, int blueShift) {
    this.screen = screen;
    this.bitsPerPixel = bitsPerPixel;
    this.depth = depth;
    this.bigEndianFlag = bigEndianFlag;
    this.trueColourFlag = trueColorFlag;
    this.redMax = redMax;
    this.greenMax = greenMax;
    this.blueMax = blueMax;
    this.redShift = redShift;
    this.greenShift = greenShift;
    this.blueShift = blueShift;
  }

  @Override
  public void write(DataOutputStream os) throws IOException {
    os.writeByte(RfbConstants.CLIENT_SET_PIXEL_FORMAT);

    // Padding
    os.writeByte(0);
    os.writeByte(0);
    os.writeByte(0);

    // Send pixel format
    os.writeByte(bitsPerPixel);
    os.writeByte(depth);
    os.writeByte(bigEndianFlag);
    os.writeByte(trueColourFlag);
    os.writeShort(redMax);
    os.writeShort(greenMax);
    os.writeShort(blueMax);
    os.writeByte(redShift);
    os.writeByte(greenShift);
    os.writeByte(blueShift);

    // Padding
    os.writeByte(0);
    os.writeByte(0);
    os.writeByte(0);

    screen.setPixelFormat(bitsPerPixel, depth, bigEndianFlag, trueColourFlag, redMax, greenMax, blueMax, redShift, greenShift, blueShift);
  }

}
