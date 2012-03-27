package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;

/**
 * FramebufferUpdateRequestPacket
 * 
 * @author Volodymyr M. Lisivka
 */
public class FramebufferUpdateRequestPacket implements ClientPacket {

  private final int incremental;
  private final int x, y, width, height;

  public FramebufferUpdateRequestPacket(int incremental, int x, int y, int width, int height) {
    this.incremental = incremental;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }


  @Override
  public void write(DataOutputStream os) throws IOException {
    os.writeByte(RfbConstants.CLIENT_FRAMEBUFFER_UPDATE_REQUEST);

    os.writeByte(incremental);
    os.writeShort(x);
    os.writeShort(y);
    os.writeShort(width);
    os.writeShort(height);
  }

}
