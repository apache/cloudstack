package com.cloud.consoleproxy.vnc.packet.server;

import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.BufferedImageCanvas;
import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.FrameBufferEventListener;
import com.cloud.consoleproxy.vnc.VncScreenDescription;
import com.cloud.consoleproxy.vnc.packet.server.CopyRect;
import com.cloud.consoleproxy.vnc.packet.server.RawRect;
import com.cloud.consoleproxy.vnc.packet.server.Rect;

public class FramebufferUpdatePacket {

  private final VncScreenDescription screen;
  private final BufferedImageCanvas canvas;
  private final FrameBufferEventListener clientListener;

  public FramebufferUpdatePacket(BufferedImageCanvas canvas, VncScreenDescription screen, DataInputStream is, 
    FrameBufferEventListener clientListener) throws IOException {
	  
    this.screen = screen;
    this.canvas = canvas;
    this.clientListener = clientListener;
    readPacketData(is);
  }

  private void readPacketData(DataInputStream is) throws IOException {
    is.skipBytes(1);// Skip padding

    // Read number of rectangles
    int numberOfRectangles = is.readUnsignedShort();

    // For all rectangles
    for (int i = 0; i < numberOfRectangles; i++) {

      // Read coordinate of rectangle
      int x = is.readUnsignedShort();
      int y = is.readUnsignedShort();
      int width = is.readUnsignedShort();
      int height = is.readUnsignedShort();

      int encodingType = is.readInt();

      // Process rectangle
      Rect rect;
      switch (encodingType) {

      case RfbConstants.ENCODING_RAW: {
        rect = new RawRect(screen, x, y, width, height, is);
        break;
      }

      case RfbConstants.ENCODING_COPY_RECT: {
        rect = new CopyRect(x, y, width, height, is);
        break;
      }

      case RfbConstants.ENCODING_DESKTOP_SIZE: {
        rect = new FrameBufferSizeChangeRequest(canvas, width, height);
        break;
      }

      default:
        throw new RuntimeException("Unsupported ecnoding: " + encodingType);
      }

      paint(rect, canvas);
      
      if(this.clientListener != null)
    	  this.clientListener.onFramebufferUpdate(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

  }

  public void paint(Rect rect, BufferedImageCanvas canvas) {
    // Draw rectangle on offline buffer
    rect.paint(canvas.getOfflineImage(), canvas.getOfflineGraphics());
    
    // Request update of repainted area
    canvas.repaint(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
  }

}
