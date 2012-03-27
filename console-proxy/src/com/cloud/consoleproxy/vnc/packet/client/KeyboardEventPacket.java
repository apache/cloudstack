package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;

public class KeyboardEventPacket implements ClientPacket {

  private final int downFlag, key;
  
  public KeyboardEventPacket(int downFlag, int key) {
    this.downFlag = downFlag;
    this.key = key;
  }

  @Override
  public void write(DataOutputStream os) throws IOException {
    os.writeByte(RfbConstants.CLIENT_KEYBOARD_EVENT);

    os.writeByte(downFlag);
    os.writeShort(0); // padding
    os.writeInt(key);
  }

}
