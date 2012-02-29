package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;

public class MouseEventPacket implements ClientPacket {

  private final int buttonMask, x, y;

  public MouseEventPacket(int buttonMask, int x, int y) {
    this.buttonMask = buttonMask;
    this.x = x;
    this.y = y;
  }

  @Override
  public void write(DataOutputStream os) throws IOException {
    os.writeByte(RfbConstants.CLIENT_POINTER_EVENT);

    os.writeByte(buttonMask);
    os.writeShort(x);
    os.writeShort(y);
  }

}
