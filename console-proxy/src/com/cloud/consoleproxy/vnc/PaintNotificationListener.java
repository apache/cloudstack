package com.cloud.consoleproxy.vnc;

public interface PaintNotificationListener {

  /**
   * Notify subscriber that screen is updated, so client can send another frame
   * buffer update request to server.
   */
  void imagePaintedOnScreen();

}
