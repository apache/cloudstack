package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientPacket {

  void write(DataOutputStream os) throws IOException;

}
