package com.cloud.consoleproxy.vnc.packet.server;

import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.RfbConstants;

public class ServerCutText {
  private static final Logger s_logger = Logger.getLogger(ServerCutText.class);

  private String content;

  public String getContent() {
    return content;
  }

  public ServerCutText(DataInputStream is) throws IOException {
    readPacketData(is);
  }

  private void readPacketData(DataInputStream is) throws IOException {
    is.skipBytes(3);// Skip padding
    int length = is.readInt();
    byte buf[] = new byte[length];
    is.readFully(buf);

    content = new String(buf, RfbConstants.CHARSET);

    /* LOG */s_logger.info("Clippboard content: " + content);
  }

}
