package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;

public class SetEncodingsPacket implements ClientPacket {

  private final int[] encodings;

  public SetEncodingsPacket(int[] encodings)
  {
    this.encodings = encodings;
  }
  
  @Override
  public void write(DataOutputStream os) throws IOException
  {
    os.writeByte(RfbConstants.CLIENT_SET_ENCODINGS);
    
    os.writeByte(0);//padding
    
    os.writeShort(encodings.length);
    
    for(int i=0;i<encodings.length;i++)
    {
      os.writeInt(encodings[i]);
    }
  }

}
