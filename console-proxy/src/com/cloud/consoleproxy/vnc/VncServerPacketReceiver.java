package com.cloud.consoleproxy.vnc;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.packet.server.FramebufferUpdatePacket;
import com.cloud.consoleproxy.vnc.packet.server.ServerCutText;

public class VncServerPacketReceiver implements Runnable {

  private final VncScreenDescription screen;
  private BufferedImageCanvas canvas;
  private DataInputStream is;

  private boolean connectionAlive = true;
  private VncClient vncConnection;
  private final FrameBufferUpdateListener fburListener;
  private final FrameBufferEventListener clientListener;

  public VncServerPacketReceiver(DataInputStream is, BufferedImageCanvas canvas, VncScreenDescription screen, VncClient vncConnection,
      FrameBufferUpdateListener fburListener, FrameBufferEventListener clientListener) {
    this.screen = screen;
    this.canvas = canvas;
    this.is = is;
    this.vncConnection = vncConnection;
    this.fburListener = fburListener;
    this.clientListener = clientListener;
  }
  
  public BufferedImageCanvas getCanvas() { 
	return canvas; 
  }

  @Override
  public void run() {
    try {
      while (connectionAlive) {

        // Read server message type
        int messageType = is.readUnsignedByte();

        // Invoke packet handler by packet type.
        switch (messageType) {

        case RfbConstants.SERVER_FRAMEBUFFER_UPDATE: {
          // Notify sender that frame buffer update is received,
          // so it can send another frame buffer update request
          fburListener.frameBufferPacketReceived();
          // Handle frame buffer update
          new FramebufferUpdatePacket(canvas, screen, is, clientListener);
          break;
        }

        case RfbConstants.SERVER_BELL: {
          serverBell();
          break;
        }

        case RfbConstants.SERVER_CUT_TEXT: {
          serverCutText(is);
          break;
        }

        default:
          throw new RuntimeException("Unknown server packet type: " + messageType + ".");
        }

      }
    } catch (Throwable e) {
      if (connectionAlive) {
        closeConnection();
        vncConnection.shutdown();
      }
    }
  }

  public void closeConnection() {
    connectionAlive = false;
  }

  /**
   * Handle server bell packet.
   */
  private void serverBell() {
    Toolkit.getDefaultToolkit().beep();
  }

  /**
   * Handle packet with server clip-board.
   */
  private void serverCutText(DataInputStream is) throws IOException {
    ServerCutText clipboardContent = new ServerCutText(is);
    StringSelection contents = new StringSelection(clipboardContent.getContent());
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);
    
    SimpleLogger.info("Server clipboard buffer: "+clipboardContent.getContent());
  }
}
