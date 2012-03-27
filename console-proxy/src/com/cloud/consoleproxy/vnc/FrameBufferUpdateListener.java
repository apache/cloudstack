package com.cloud.consoleproxy.vnc;

public interface FrameBufferUpdateListener {

  /**
   * Notify listener, that frame buffer update packet is received, so client is
   * permitted (but not obligated) to ask server to send another update.
   */
  void frameBufferPacketReceived();
}
