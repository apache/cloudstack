//
//  Copyright (C) 2004 Horizon Wimba.  All Rights Reserved.
//  Copyright (C) 2001-2003 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2001,2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

// repackage it to VMOps common packaging structure
package com.cloud.console;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

//
// VncCanvas is a subclass of Canvas which draws a VNC desktop on it.
//
@SuppressWarnings("serial")
public class ConsoleCanvas extends Canvas
  	implements KeyListener, MouseListener, MouseMotionListener {

	private static final Logger s_logger = Logger.getLogger(ConsoleCanvas.class);
	
	// ConsoleViewer viewer;
	RfbViewer viewer;
	public RfbProto rfb;
	ColorModel cm8, cm24;
	Color[] colors;
	public int bytesPixel;

	int maxWidth = 0, maxHeight = 0;
	int scalingFactor;
	public int scaledWidth, scaledHeight;

	public Image memImage;
	public Graphics memGraphics;

	Image rawPixelsImage;
	MemoryImageSource pixelsSource;
	byte[] pixels8;
	int[] pixels24;

	// ZRLE encoder's data.
	byte[] zrleBuf;
	int zrleBufLen = 0;
	byte[] zrleTilePixels8;
	int[] zrleTilePixels24;
	ZlibInStream zrleInStream;
	boolean zrleRecWarningShown = false;

	// Zlib encoder's data.
	byte[] zlibBuf;
	int zlibBufLen = 0;
	Inflater zlibInflater;

    // Tight encoder's data.
	final static int tightZlibBufferSize = 512;
	Inflater[] tightInflaters;

	// Since JPEG images are loaded asynchronously, we have to remember
	// their position in the framebuffer. Also, this jpegRect object is
	// used for synchronization between the rfbThread and a JVM's thread
	// which decodes and loads JPEG images.
	Rectangle jpegRect;

	// True if we process keyboard and mouse events.
	boolean inputEnabled;

	//
	// The constructors.
	//

	// public ConsoleCanvas(ConsoleViewer v, int maxWidth_, int maxHeight_) {
	public ConsoleCanvas(RfbViewer v, int maxWidth_, int maxHeight_) {

		viewer = v;
		maxWidth = maxWidth_;
		maxHeight = maxHeight_;
    
		rfb = viewer.getRfb();
		scalingFactor = viewer.getScalingFactor();

		tightInflaters = new Inflater[4];

		cm8 = new DirectColorModel(8, 7, (7 << 3), (3 << 6));
		cm24 = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);

		colors = new Color[256];
		for (int i = 0; i < 256; i++)
			colors[i] = new Color(cm8.getRGB(i));
    
		inputEnabled = false;
    
    // Keyboard listener is enabled even in view-only mode, to catch
    // 'r' or 'R' key presses used to request screen update.
		addKeyListener(this);
	}

	// public ConsoleCanvas(ConsoleViewer v) throws IOException {
	public ConsoleCanvas(RfbViewer v) throws IOException {
		this(v, 0, 0);
	}

	//
	// Callback methods to determine geometry of our Component.
	//

	public Dimension getPreferredSize() {
		return new Dimension(scaledWidth, scaledHeight);
	}

	public Dimension getMinimumSize() {
		return new Dimension(scaledWidth, scaledHeight);
	}

	public Dimension getMaximumSize() {
		return new Dimension(scaledWidth, scaledHeight);
	}

	//
	// All painting is performed here.
	//

	public void update(Graphics g) {
		paint(g);
	}

    public void paint(Graphics g) {
    	// if(s_logger.isInfoEnabled())
		// 	s_logger.info("VncCanvas.paint called g=" + g.hashCode() + g +
		// 		" memImage=" + memImage);
		if (rfb == null) {
			return;
		}
		
		synchronized (memImage) {
			if (rfb.framebufferWidth == scaledWidth) {
				g.drawImage(memImage, 0, 0, null);
			} else {
				paintScaledFrameBuffer(g);
			}
		}
		if (showSoftCursor) {
			int x0 = cursorX - hotX, y0 = cursorY - hotY;
			Rectangle r = new Rectangle(x0, y0, cursorWidth, cursorHeight);
			if (r.intersects(g.getClipBounds())) {
				g.drawImage(softCursor, x0, y0, null);
			}
		}
	}

    public void paintScaledFrameBuffer(Graphics g) {
    	g.drawImage(memImage, 0, 0, scaledWidth, scaledHeight, null);
    }

    //
    // Override the ImageObserver interface method to handle drawing of
    // JPEG-encoded data.
    //

    public boolean imageUpdate(Image img, int infoflags,
    		int x, int y, int width, int height) {
    	if ((infoflags & (ALLBITS | ABORT)) == 0) {
    		return true;		// We need more image data.
    	} else {
    		// If the whole image is available, draw it now.
    		if ((infoflags & ALLBITS) != 0) {
    			if (jpegRect != null) {
    				synchronized(jpegRect) {
    					memGraphics.drawImage(img, jpegRect.x, jpegRect.y, null);
    					scheduleRepaint(jpegRect.x, jpegRect.y,
    							jpegRect.width, jpegRect.height);
    					jpegRect.notify();
    				}
    			}
    		}
    		return false;		// All image data was processed.
    	}
    }

    //
    // Start/stop receiving mouse events. Keyboard events are received
    // even in view-only mode, because we want to map the 'r' key to the
    // screen refreshing function.
    //

    public synchronized void enableInput(boolean enable) {
    	if (enable && !inputEnabled) {
    		inputEnabled = true;
    		addMouseListener(this);
    		addMouseMotionListener(this);

    		viewer.onInputEnabled(true);
      
    		createSoftCursor();	// scaled cursor
    	} else if (!enable && inputEnabled) {
    		inputEnabled = false;
    		removeMouseListener(this);
    		removeMouseMotionListener(this);
      
    		viewer.onInputEnabled(false);
    		createSoftCursor();	// non-scaled cursor
    	}
    }

    public void setPixelFormat() throws IOException {
    	bytesPixel = viewer.setPixelFormat(rfb);
    
    	updateFramebufferSize();
    }

    public void updateFramebufferSize() {

    	if(rfb == null) {
    		s_logger.warn("updateFrameBufferSize is called but RFB is not ready");
    		return;
    	}
    	
    	// Useful shortcuts.
    	int fbWidth = rfb.framebufferWidth;
    	int fbHeight = rfb.framebufferHeight;

    	// Calculate scaling factor for auto scaling.
    	if (maxWidth > 0 && maxHeight > 0) {
    		int f1 = maxWidth * 100 / fbWidth;
    		int f2 = maxHeight * 100 / fbHeight;
    		scalingFactor = Math.min(f1, f2);
    		if (scalingFactor > 100)
    			scalingFactor = 100;
      
    		if(s_logger.isInfoEnabled())
    			s_logger.info("Scaling desktop at " + scalingFactor + "%");
    	}

    	// Update scaled framebuffer geometry.
    	scaledWidth = (fbWidth * scalingFactor + 50) / 100;
    	scaledHeight = (fbHeight * scalingFactor + 50) / 100;

    	// Create new off-screen image either if it does not exist, or if
    	// its geometry should be changed. It's not necessary to replace
    	// existing image if only pixel format should be changed.
    	if (memImage == null) {
//      	memImage = viewer.vncContainer.createImage(fbWidth, fbHeight);
    		memImage = new BufferedImage(fbWidth, fbHeight, BufferedImage.TYPE_4BYTE_ABGR);
    		memGraphics = memImage.getGraphics();
    	} else if (memImage.getWidth(null) != fbWidth || memImage.getHeight(null) != fbHeight) {
    		synchronized(memImage) {
//				memImage = viewer.vncContainer.createImage(fbWidth, fbHeight);
    			memImage = new BufferedImage(fbWidth, fbHeight, BufferedImage.TYPE_4BYTE_ABGR);
    			memGraphics = memImage.getGraphics();
    		}
    	}

    	// Images with raw pixels should be re-allocated on every change
    	// of geometry or pixel format.
    	if (bytesPixel == 1) {

    		pixels24 = null;
    		pixels8 = new byte[fbWidth * fbHeight];

    		pixelsSource = new MemoryImageSource(fbWidth, fbHeight, cm8, pixels8, 0, fbWidth);

    		zrleTilePixels24 = null;
    		zrleTilePixels8 = new byte[64 * 64];

    	} else {

    		pixels8 = null;
    		pixels24 = new int[fbWidth * fbHeight];

    		pixelsSource = new MemoryImageSource(fbWidth, fbHeight, cm24, pixels24, 0, fbWidth);

    		zrleTilePixels8 = null;
    		zrleTilePixels24 = new int[64 * 64];
    	}
    	pixelsSource.setAnimated(true);
    	rawPixelsImage = Toolkit.getDefaultToolkit().createImage(pixelsSource);
    	
    	viewer.onFramebufferSizeChange(scaledWidth, scaledHeight);
    }

    public void resizeDesktopFrame() {
    	setSize(scaledWidth, scaledHeight);

    	// FIXME: Find a better way to determine correct size of a
    	// ScrollPane.  -- const
    	viewer.onDesktopResize();

    	// Try to limit the frame size to the screen size.
    	Dimension screenSize = viewer.getScreenSize();
    	Dimension frameSize = viewer.getFrameSize();
    	Dimension newSize = frameSize;

	    // Reduce Screen Size by 30 pixels in each direction;
	    // This is a (poor) attempt to account for
	    //     1) Menu bar on Macintosh (should really also account for
	    //        Dock on OSX).  Usually 22px on top of screen.
	    //     2) Taxkbar on Windows (usually about 28 px on bottom)
	    //     3) Other obstructions.

    	screenSize.height -= 30;
    	screenSize.width  -= 30;

    	boolean needToResizeFrame = false;
    	if (frameSize.height > screenSize.height) {
    		newSize.height = screenSize.height;
    		needToResizeFrame = true;
    	}
    	if (frameSize.width > screenSize.width) {
    		newSize.width = screenSize.width;
    		needToResizeFrame = true;
    	}

    	if (needToResizeFrame)
    		viewer.onFrameResize(newSize);
    }

    //
    // processNormalProtocol() - executed by the rfbThread to deal with the
    // RFB socket.
    //
  
    long timestamp = 0;
    long bytes_sent_last = 0;
    long rate_limit = 100000;
  
    public void processNormalProtocol() throws Exception {
    	try {
    		timestamp = 0;
    		// main dispatch loop
    		while (true) {
			  processNormalProtocol2();
    		}
    	} catch (IOException e) {
		  
			  // Server connection get disrupted.
			  // Send a special msg to the client stream so that the client can blank out
			  // the screen.
    		viewer.onProtocolProcessException(e);
		  
    		// rethrow the exception;
    		throw e;
    	}
    }
  
    private void processNormalProtocol2() throws Exception {
    	byte[] bs = rfb.sis.getSplit();
//      if (viewer.inProxyMode) {
//    	  s_logger.info("bs != null = " + (bs != null) + " viewer.clientStream = " + viewer.clientStream + " info = " + viewer.clientStreamInfo);
//      }
    	viewer.onPreProtocolProcess(bs);
      
      // Read message type from the server.
      int msgType = rfb.readServerMessageType();

      // Process the message depending on its type.
      switch (msgType) {
      case 250:
    	  int b = rfb.is.read();
    	  if (b == 1) {
    	      if(s_logger.isDebugEnabled())
    	    	  s_logger.debug("S->C RFB msg VMOps DISCONNECT");
    		  
    		  /*
    		  viewer.vc.paintErrorString("Disconnected");
    		  */
    		  viewer.onDisconnectMessage();
    		  break;
    	  } else if (b == 2) {
    		  int n = rfb.is.readInt();
    		  
    	      if(s_logger.isDebugEnabled())
    	    	  s_logger.debug("S->C RFB msg VMOps COMPRESSED");
    		  
    		  if(s_logger.isInfoEnabled())
    			  s_logger.info("Reading compressed data size=" + n);
    		  
			  byte[] buf = new byte[n];
			  rfb.is.readFully(buf);
			  ByteArrayInputStream bis = new ByteArrayInputStream(buf);
			  DataInputStream oldis = rfb.is;
			  // swap out the underlying input stream
			  rfb.is = new DataInputStream(new GZIPInputStream(bis, 65536));
    		  try {
    			  processNormalProtocol2();
    		  } finally {
    			  rfb.is = oldis;
    		  }
    		  break;
    	  } else if (b == 3) {
    	      if(s_logger.isDebugEnabled())
    	    	  s_logger.debug("S->C RFB msg VMOps KEEP-ALIVE");
    		  
    		  // Send liveliness message
    		  rfb.writeClientCustomMessage(0);
    		  break;
    	  }
    	  throw new Exception ("Message type 250 comes with bad submessage type: " + b);
    	  
      case RfbProto.FramebufferUpdate:
	      if(s_logger.isDebugEnabled())
	    	  s_logger.debug("S->C RFB FramebufferUpdate");
    	  
    	  rfb.readFramebufferUpdate();
    	  boolean cursorPosReceived = false;
    	  boolean fullUpdateNeeded = false;
    	  for (int i = 0; i < rfb.updateNRects; i++) {
			  rfb.readFramebufferUpdateRectHdr();
			  int rx = rfb.updateRectX, ry = rfb.updateRectY;
			  int rw = rfb.updateRectW, rh = rfb.updateRectH;
		
			  if (rfb.updateRectEncoding == rfb.EncodingLastRect) {
				    if(s_logger.isInfoEnabled())
				    	s_logger.info("Received EncodingLastRect packet in iteration " + i + " of " + rfb.updateNRects);
				    break;
			  }
		
			  if (rfb.updateRectEncoding == rfb.EncodingNewFBSize) {
				  rfb.setFramebufferSize(rw, rh);
				  updateFramebufferSize();
			  
				  if(s_logger.isInfoEnabled())
					  s_logger.info("Frame buffer size is changed to (" + rw + ", " + rh + "), packet in iteration " + i + " of " + rfb.updateNRects);
				  break;
			  }
	
			  if (rfb.updateRectEncoding == rfb.EncodingXCursor ||
			      rfb.updateRectEncoding == rfb.EncodingRichCursor) {
				  
				byte[] cursorData = new byte[ConsoleCanvas.getCursorDataSize(rfb.updateRectEncoding, 
					rw, rh)]; 
			    handleCursorShapeUpdate(rfb.updateRectEncoding, rx, ry, rw, rh, cursorData);
			    viewer.onFramebufferCursorShapeChange(rfb.updateRectEncoding, rx, ry, rw, rh, cursorData);
			    
			    if(s_logger.isInfoEnabled())
			    	s_logger.info("Received CursorShapeUpdate packet in iteration " + i + " of " + rfb.updateNRects);
			    continue;
			  }
	
			  if (rfb.updateRectEncoding == rfb.EncodingPointerPos) {
			    softCursorMove(rx, ry);
			    cursorPosReceived = true;
			    viewer.onFramebufferCursorMove(rx, ry);
			    
			    if(s_logger.isInfoEnabled())
			    	s_logger.info("Received PointerPosUpdate packet in iteration " + i + " of " + rfb.updateNRects);
			    continue;
			  }
	          
	          // synchronize the access to memory pixel images as we have
	          // separated client update into a sender thread
	          rfb.startTiming();

	//	  		if(s_logger.isInfoEnabled())
	//	  		s_logger.info("RFB Update Rect Encoding: " + rfb.updateRectEncoding);
			  switch (rfb.updateRectEncoding) {
			  case RfbProto.EncodingRaw:
			    handleRawRect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingCopyRect:
			    handleCopyRect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingRRE:
			    handleRRERect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingCoRRE:
			    handleCoRRERect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingHextile:
			    handleHextileRect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingZRLE:
			    handleZRLERect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingZlib:
		        handleZlibRect(rx, ry, rw, rh);
			    break;
			  case RfbProto.EncodingTight:
			    handleTightRect(rx, ry, rw, rh);
			    break;
			  default:
			    throw new Exception("Unknown RFB rectangle encoding " +
						rfb.updateRectEncoding);
			  }
			  
			  viewer.onFramebufferUpdate(rx, ry, rw, rh);
	          rfb.stopTiming();
	
	          fullUpdateNeeded = viewer.onPostFrameBufferUpdateProcess(cursorPosReceived);
	//        viewer.autoSelectEncodings();
    	  }
    	  
    	  // send update request outside the for loop!
    	  if(!viewer.isProxy()) {
		      if(s_logger.isDebugEnabled())
		    	  s_logger.debug("Send FrameBufferUpdateRequest in response of server update return");
    		  
		  	rfb.writeFramebufferUpdateRequest(0, 0, rfb.framebufferWidth,
		  			rfb.framebufferHeight, !fullUpdateNeeded);
    	  } else {
				// if don't have client connected, to drive thumbnail updates, we need to continue 
				// to keep the request-cycle with server
				if(!viewer.hasClientConnection()) {
			      if(s_logger.isDebugEnabled())
			    	  s_logger.debug("No client is connected, send FrameBufferUpdateRequest from proxy");
					
		    	  rfb.writeFramebufferUpdateRequest(0, 0, rfb.framebufferWidth,
						  rfb.framebufferHeight,
						  !fullUpdateNeeded);
				} else {
				      if(s_logger.isDebugEnabled())
				    	  s_logger.debug("Client is connected, let client to send FrameBufferUpdateRequest");
				}
    	  }
    	  break;

      case RfbProto.SetColourMapEntries:
	      if(s_logger.isDebugEnabled())
	    	  s_logger.debug("S->C RFB SetColourMapEntries");
    	  
    	  throw new Exception("Can't handle SetColourMapEntries message");

      case RfbProto.Bell:
	      if(s_logger.isDebugEnabled())
	    	  s_logger.debug("S->C RFB Bell");
    	  
    	  viewer.onBellMessage();
    	  break;

      case RfbProto.ServerCutText:
	      if(s_logger.isDebugEnabled())
	    	  s_logger.debug("S->C RFB ServerCutText");
    	  
    	  String s = rfb.readServerCutText();
    	  // viewer.clipboard.setCutText(s);
    	  break;

      default:
	      if(s_logger.isDebugEnabled())
	    	  s_logger.debug("S->C RFB Unknown message type " + msgType);
    	 throw new Exception("Unknown RFB message type " + msgType);
      }
    }

  //
  // Handle a raw rectangle. The second form with paint==false is used
  // by the Hextile decoder for raw-encoded tiles.
  //
    void handleRawRect(int x, int y, int w, int h) throws IOException {
	  handleRawRect(x, y, w, h, true);
    }

    void handleRawRect(int x, int y, int w, int h, boolean paint) throws IOException {
	  // s_logger.info("handleRawRect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h + " paint=" + paint);

	  if (bytesPixel == 1) {
    	for (int dy = y; dy < y + h; dy++) {
    		rfb.readFully(pixels8, dy * rfb.framebufferWidth + x, w);
    		if (rfb.rec != null) {
    			rfb.rec.write(pixels8, dy * rfb.framebufferWidth + x, w);
    		}
    	}
	  } else {
    	byte[] buf = new byte[w * 4];
    	int i, offset;
    	for (int dy = y; dy < y + h; dy++) {
    		rfb.readFully(buf);
    		if (rfb.rec != null) {
    			rfb.rec.write(buf);
    		}
    		
    		offset = dy * rfb.framebufferWidth + x;
    		for (i = 0; i < w; i++) {
    			pixels24[offset + i] =
    				(buf[i * 4 + 2] & 0xFF) << 16 |
    				(buf[i * 4 + 1] & 0xFF) << 8 |
    				(buf[i * 4] & 0xFF);
    		}
    	}
	  }

	  handleUpdatedPixels(x, y, w, h);
	  if (paint)
		  scheduleRepaint(x, y, w, h);
    }
    
    //
    // Handle a CopyRect rectangle.
    //
    void handleCopyRect(int x, int y, int w, int h) throws IOException {

    	s_logger.info("handleCopyRect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
    	rfb.readCopyRect();
    	memGraphics.copyArea(rfb.copyRectSrcX, rfb.copyRectSrcY, w, h,
			 x - rfb.copyRectSrcX, y - rfb.copyRectSrcY);

    	scheduleRepaint(x, y, w, h);
    }

    //
    // Handle an RRE-encoded rectangle.
    //
    void handleRRERect(int x, int y, int w, int h) throws IOException {
	  //s_logger.info("handleRRERect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);

	  int nSubrects = rfb.is.readInt();

	  byte[] bg_buf = new byte[bytesPixel];
	  rfb.readFully(bg_buf);
	  Color pixel;
	  if (bytesPixel == 1) {
		  pixel = colors[bg_buf[0] & 0xFF];
	  } else {
		  pixel = new Color(bg_buf[2] & 0xFF, bg_buf[1] & 0xFF, bg_buf[0] & 0xFF);
	  }
	  memGraphics.setColor(pixel);
	  memGraphics.fillRect(x, y, w, h);

	  byte[] buf = new byte[nSubrects * (bytesPixel + 8)];
	  rfb.readFully(buf);
	  DataInputStream ds = new DataInputStream(new ByteArrayInputStream(buf));

	  if (rfb.rec != null) {
		  rfb.rec.writeIntBE(nSubrects);
		  rfb.rec.write(bg_buf);
		  rfb.rec.write(buf);
	  }

	  int sx, sy, sw, sh;

	  for (int j = 0; j < nSubrects; j++) {
		  if (bytesPixel == 1) {
			  pixel = colors[ds.readUnsignedByte()];
		  } else {
			  ds.skip(4);
			  pixel = new Color(buf[j*12+2] & 0xFF,
					  buf[j*12+1] & 0xFF,
					  buf[j*12]   & 0xFF);
		  }
		  sx = x + ds.readUnsignedShort();
		  sy = y + ds.readUnsignedShort();
		  sw = ds.readUnsignedShort();
		  sh = ds.readUnsignedShort();

		  memGraphics.setColor(pixel);
		  memGraphics.fillRect(sx, sy, sw, sh);
	  }

	  scheduleRepaint(x, y, w, h);
  	}

    //
    // Handle a CoRRE-encoded rectangle.
    //

  	void handleCoRRERect(int x, int y, int w, int h) throws IOException {
	  //s_logger.info("handleCpRRERect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
	  int nSubrects = rfb.is.readInt();

	  byte[] bg_buf = new byte[bytesPixel];
	  rfb.readFully(bg_buf);
	  Color pixel;
	  if (bytesPixel == 1) {
		  pixel = colors[bg_buf[0] & 0xFF];
	  } else {
		  pixel = new Color(bg_buf[2] & 0xFF, bg_buf[1] & 0xFF, bg_buf[0] & 0xFF);
	  }
	  memGraphics.setColor(pixel);
	  memGraphics.fillRect(x, y, w, h);

	  byte[] buf = new byte[nSubrects * (bytesPixel + 4)];
	  rfb.readFully(buf);

	  if (rfb.rec != null) {
		  rfb.rec.writeIntBE(nSubrects);
		  rfb.rec.write(bg_buf);
		  rfb.rec.write(buf);
	  }

	  int sx, sy, sw, sh;
	  int i = 0;

	  for (int j = 0; j < nSubrects; j++) {
	      if (bytesPixel == 1) {
	    	  pixel = colors[buf[i++] & 0xFF];
	      } else {
	    	  pixel = new Color(buf[i+2] & 0xFF, buf[i+1] & 0xFF, buf[i] & 0xFF);
	    	  i += 4;
	      }
	      sx = x + (buf[i++] & 0xFF);
	      sy = y + (buf[i++] & 0xFF);
	      sw = buf[i++] & 0xFF;
	      sh = buf[i++] & 0xFF;
	
	      memGraphics.setColor(pixel);
	      memGraphics.fillRect(sx, sy, sw, sh);
	  }

	  scheduleRepaint(x, y, w, h);
  	}

  	//
  	// Handle a Hextile-encoded rectangle.
  	//

  	// These colors should be kept between handleHextileSubrect() calls.
  	private Color hextile_bg, hextile_fg;

  	void handleHextileRect(int x, int y, int w, int h) throws IOException {
  		// s_logger.info("handleHextileRect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
  		hextile_bg = new Color(0);
  		hextile_fg = new Color(0);

  		for (int ty = y; ty < y + h; ty += 16) {
  			int th = 16;
  			if (y + h - ty < 16)
  				th = y + h - ty;

  			for (int tx = x; tx < x + w; tx += 16) {
  				int tw = 16;
  				if (x + w - tx < 16)
  					tw = x + w - tx;

  				handleHextileSubrect(tx, ty, tw, th);
  			}

  			// Finished with a row of tiles, now let's show it.

  		}
  		scheduleRepaint(x, y, w, h);
  	}

  	//
  	// Handle one tile in the Hextile-encoded data.
  	//
  	void handleHextileSubrect(int tx, int ty, int tw, int th) throws IOException {

  		int subencoding = rfb.is.readUnsignedByte();
  		if (rfb.rec != null) {
  			rfb.rec.writeByte(subencoding);
  		}

  		// Is it a raw-encoded sub-rectangle?
  		if ((subencoding & rfb.HextileRaw) != 0) {
  			handleRawRect(tx, ty, tw, th, false);
  			return;
  		}

  		// Read and draw the background if specified.
  		byte[] cbuf = new byte[bytesPixel];
  		if ((subencoding & rfb.HextileBackgroundSpecified) != 0) {
  			rfb.readFully(cbuf);
  			if (bytesPixel == 1) {
  				hextile_bg = colors[cbuf[0] & 0xFF];
  			} else {
  				hextile_bg = new Color(cbuf[2] & 0xFF, cbuf[1] & 0xFF, cbuf[0] & 0xFF);
  			}
      
  			if (rfb.rec != null) {
  				rfb.rec.write(cbuf);
  			}
  		}
  		memGraphics.setColor(hextile_bg);
  		memGraphics.fillRect(tx, ty, tw, th);

  		// Read the foreground color if specified.
  		if ((subencoding & rfb.HextileForegroundSpecified) != 0) {
  			rfb.readFully(cbuf);
  			if (bytesPixel == 1) {
  				hextile_fg = colors[cbuf[0] & 0xFF];
  			} else {
  				hextile_fg = new Color(cbuf[2] & 0xFF, cbuf[1] & 0xFF, cbuf[0] & 0xFF);
  			}
  			if (rfb.rec != null) {
  				rfb.rec.write(cbuf);
  			}
  		}

  		// Done with this tile if there is no sub-rectangles.
  		if ((subencoding & rfb.HextileAnySubrects) == 0)
  			return;

  		int nSubrects = rfb.is.readUnsignedByte();
  		int bufsize = nSubrects * 2;
  		if ((subencoding & rfb.HextileSubrectsColoured) != 0) {
  			bufsize += nSubrects * bytesPixel;
  		}
  		byte[] buf = new byte[bufsize];
  		rfb.readFully(buf);
  		if (rfb.rec != null) {
  			rfb.rec.writeByte(nSubrects);
  			rfb.rec.write(buf);
  		}

  		int b1, b2, sx, sy, sw, sh;
  		int i = 0;
  		if ((subencoding & rfb.HextileSubrectsColoured) == 0) {
  			// Sub-rectangles are all of the same color.
  			memGraphics.setColor(hextile_fg);
  			for (int j = 0; j < nSubrects; j++) {
  				b1 = buf[i++] & 0xFF;
  				b2 = buf[i++] & 0xFF;
  				sx = tx + (b1 >> 4);
  				sy = ty + (b1 & 0xf);
  				sw = (b2 >> 4) + 1;
  				sh = (b2 & 0xf) + 1;
  				memGraphics.fillRect(sx, sy, sw, sh);
  			}
  		} else if (bytesPixel == 1) {

  			// BGR233 (8-bit color) version for colored sub-rectangles.
  			for (int j = 0; j < nSubrects; j++) {
  				hextile_fg = colors[buf[i++] & 0xFF];
  				b1 = buf[i++] & 0xFF;
  				b2 = buf[i++] & 0xFF;
  				sx = tx + (b1 >> 4);
  				sy = ty + (b1 & 0xf);
  				sw = (b2 >> 4) + 1;
  				sh = (b2 & 0xf) + 1;
  				memGraphics.setColor(hextile_fg);
  				memGraphics.fillRect(sx, sy, sw, sh);
  			}

  		} else {

  			// Full-color (24-bit) version for colored sub-rectangles.
  			for (int j = 0; j < nSubrects; j++) {
  				hextile_fg = new Color(buf[i+2] & 0xFF,
			       buf[i+1] & 0xFF,
			       buf[i] & 0xFF);
  				i += 4;
  				b1 = buf[i++] & 0xFF;
  				b2 = buf[i++] & 0xFF;
  				sx = tx + (b1 >> 4);
  				sy = ty + (b1 & 0xf);
  				sw = (b2 >> 4) + 1;
  				sh = (b2 & 0xf) + 1;
  				memGraphics.setColor(hextile_fg);
  				memGraphics.fillRect(sx, sy, sw, sh);
  			}

  		}
  	}

  	//
  	// Handle a ZRLE-encoded rectangle.
  	//
  	// FIXME: Currently, session recording is not fully supported for ZRLE.
  	//

  	void handleZRLERect(int x, int y, int w, int h) throws Exception {
  		//s_logger.info("handleZRLERect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
  		if (zrleInStream == null)
  			zrleInStream = new ZlibInStream();

  		int nBytes = rfb.is.readInt();
  		if (nBytes > 64 * 1024 * 1024)
  			throw new Exception("ZRLE decoder: illegal compressed data size");

  		if (zrleBuf == null || zrleBufLen < nBytes) {
  			zrleBufLen = nBytes + 4096;
  			zrleBuf = new byte[zrleBufLen];
  		}

  		// FIXME: Do not wait for all the data before decompression.
  		rfb.readFully(zrleBuf, 0, nBytes);

  		if (rfb.rec != null) {
  			if (rfb.recordFromBeginning) {
  				rfb.rec.writeIntBE(nBytes);
  				rfb.rec.write(zrleBuf, 0, nBytes);
  			} else if (!zrleRecWarningShown) {
    	  
  				if(s_logger.isInfoEnabled()) {
  					s_logger.info("Warning: ZRLE session can be recorded" +
	                           " only from the beginning");
  					s_logger.info("Warning: Recorded file may be corrupted");
  				}
  				zrleRecWarningShown = true;
  			}
  		}

  		zrleInStream.setUnderlying(new MemInStream(zrleBuf, 0, nBytes), nBytes);

  		for (int ty = y; ty < y+h; ty += 64) {

  			int th = Math.min(y+h-ty, 64);

  			for (int tx = x; tx < x+w; tx += 64) {

  				int tw = Math.min(x+w-tx, 64);

  				int mode = zrleInStream.readU8();
  				boolean rle = (mode & 128) != 0;
  				int palSize = mode & 127;
  				int[] palette = new int[128];

  				readZrlePalette(palette, palSize);

  				if (palSize == 1) {
  					int pix = palette[0];
  					Color c = (bytesPixel == 1) ?
  							colors[pix] : new Color(0xFF000000 | pix);
  					memGraphics.setColor(c);
  					memGraphics.fillRect(tx, ty, tw, th);
  					continue;
  				}

  				if (!rle) {
  					if (palSize == 0) {
  						readZrleRawPixels(tw, th);
  					} else {
  						readZrlePackedPixels(tw, th, palette, palSize);
  					}
  				} else {
  					if (palSize == 0) {
  						readZrlePlainRLEPixels(tw, th);
  					} else {
  						readZrlePackedRLEPixels(tw, th, palette);
  					}
  				}
  				handleUpdatedZrleTile(tx, ty, tw, th);
  			}
  		}

  		zrleInStream.reset();

  		scheduleRepaint(x, y, w, h);
  	}

  	int readPixel(InStream is) throws Exception {
  		int pix;
  		if (bytesPixel == 1) {
  			pix = is.readU8();
  		} else {
  			int p1 = is.readU8();
  			int p2 = is.readU8();
  			int p3 = is.readU8();
  			pix = (p3 & 0xFF) << 16 | (p2 & 0xFF) << 8 | (p1 & 0xFF);
  		}
  		return pix;
  	}

  	void readPixels(InStream is, int[] dst, int count) throws Exception {
  		int pix;
  		if (bytesPixel == 1) {
  			byte[] buf = new byte[count];
  			is.readBytes(buf, 0, count);
  			for (int i = 0; i < count; i++) {
  				dst[i] = (int)buf[i] & 0xFF;
  			}
  		} else {
  			byte[] buf = new byte[count * 3];
  			is.readBytes(buf, 0, count * 3);
  			for (int i = 0; i < count; i++) {
  				dst[i] = ((buf[i*3+2] & 0xFF) << 16 |
                  (buf[i*3+1] & 0xFF) << 8 |
                  (buf[i*3] & 0xFF));
  			}
  		}
  	}

  	void readZrlePalette(int[] palette, int palSize) throws Exception {
  		readPixels(zrleInStream, palette, palSize);
  	}

  	void readZrleRawPixels(int tw, int th) throws Exception {
  		if (bytesPixel == 1) {
  			zrleInStream.readBytes(zrleTilePixels8, 0, tw * th);
  		} else {
  			readPixels(zrleInStream, zrleTilePixels24, tw * th); ///
  		}
  	}

  	void readZrlePackedPixels(int tw, int th, int[] palette, int palSize)
  		throws Exception {

  		int bppp = ((palSize > 16) ? 8 :
                ((palSize > 4) ? 4 : ((palSize > 2) ? 2 : 1)));
  		int ptr = 0;

  		for (int i = 0; i < th; i++) {
  			int eol = ptr + tw;
  			int b = 0;
  			int nbits = 0;

  			while (ptr < eol) {
  				if (nbits == 0) {
  					b = zrleInStream.readU8();
  					nbits = 8;
  				}
  				nbits -= bppp;
  				int index = (b >> nbits) & ((1 << bppp) - 1) & 127;
  				if (bytesPixel == 1) {
  					zrleTilePixels8[ptr++] = (byte)palette[index];
  				} else {
  					zrleTilePixels24[ptr++] = palette[index];
  				}
  			}
  		}
  	}

  	void readZrlePlainRLEPixels(int tw, int th) throws Exception {
  		int ptr = 0;
  		int end = ptr + tw * th;
  		while (ptr < end) {
  			int pix = readPixel(zrleInStream);
  			int len = 1;
  			int b;
  			do {
  				b = zrleInStream.readU8();
  				len += b;
  			} while (b == 255);

  			if (!(len <= end - ptr))
  				throw new Exception("ZRLE decoder: assertion failed" +
                            " (len <= end-ptr)");

  			if (bytesPixel == 1) {
  				while (len-- > 0) zrleTilePixels8[ptr++] = (byte)pix;
  			} else {
  				while (len-- > 0) zrleTilePixels24[ptr++] = pix;
  			}
  		}
  	}

  	void readZrlePackedRLEPixels(int tw, int th, int[] palette)
  		throws Exception {

  		int ptr = 0;
  		int end = ptr + tw * th;
  		while (ptr < end) {
  			int index = zrleInStream.readU8();
  			int len = 1;
  			if ((index & 128) != 0) {
  				int b;
  				do {
  					b = zrleInStream.readU8();
  					len += b;
  				} while (b == 255);
        
  				if (!(len <= end - ptr))
  					throw new Exception("ZRLE decoder: assertion failed" +
                              " (len <= end - ptr)");
  			}

  			index &= 127;
  			int pix = palette[index];

  			if (bytesPixel == 1) {
  				while (len-- > 0) zrleTilePixels8[ptr++] = (byte)pix;
  			} else {
  				while (len-- > 0) zrleTilePixels24[ptr++] = pix;
  			}
  		}
  	}

  	//
  	// Copy pixels from zrleTilePixels8 or zrleTilePixels24, then update.
  	//

  	void handleUpdatedZrleTile(int x, int y, int w, int h) {
  		Object src, dst;
  		if (bytesPixel == 1) {
  			src = zrleTilePixels8; dst = pixels8;
  		} else {
  			src = zrleTilePixels24; dst = pixels24;
  		}
  		int offsetSrc = 0;
  		int offsetDst = (y * rfb.framebufferWidth + x);
  		for (int j = 0; j < h; j++) {
  			System.arraycopy(src, offsetSrc, dst, offsetDst, w);
  			offsetSrc += w;
  			offsetDst += rfb.framebufferWidth;
  		}
  		handleUpdatedPixels(x, y, w, h);
  	}

  	//
  	// Handle a Zlib-encoded rectangle.
  	//

  	void handleZlibRect(int x, int y, int w, int h) throws Exception {
  		//	s_logger.info("handleZlibRect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
  		int nBytes = rfb.is.readInt();

  		if (zlibBuf == null || zlibBufLen < nBytes) {
  			zlibBufLen = nBytes * 2;
  			zlibBuf = new byte[zlibBufLen];
  		}

  		rfb.readFully(zlibBuf, 0, nBytes);

  		if (rfb.rec != null && rfb.recordFromBeginning) {
  			rfb.rec.writeIntBE(nBytes);
  			rfb.rec.write(zlibBuf, 0, nBytes);
  		}

  		if (zlibInflater == null) {
  			zlibInflater = new Inflater();
  		}
  		zlibInflater.setInput(zlibBuf, 0, nBytes);

  		if (bytesPixel == 1) {
  			for (int dy = y; dy < y + h; dy++) {
  				zlibInflater.inflate(pixels8, dy * rfb.framebufferWidth + x, w);
  				if (rfb.rec != null && !rfb.recordFromBeginning)
  					rfb.rec.write(pixels8, dy * rfb.framebufferWidth + x, w);
  			}
  		} else {
  			byte[] buf = new byte[w * 4];
  			int i, offset;
  			for (int dy = y; dy < y + h; dy++) {
  				zlibInflater.inflate(buf);
  				offset = dy * rfb.framebufferWidth + x;
  				for (i = 0; i < w; i++) {
  					pixels24[offset + i] =
  						(buf[i * 4 + 2] & 0xFF) << 16 |
  						(buf[i * 4 + 1] & 0xFF) << 8 |
  						(buf[i * 4] & 0xFF);
  				}
  				if (rfb.rec != null && !rfb.recordFromBeginning)
  					rfb.rec.write(buf);
  			}
  		}

  		handleUpdatedPixels(x, y, w, h);
  		scheduleRepaint(x, y, w, h);
  	}

  	//
  	// Handle a Tight-encoded rectangle.
  	//

  	void handleTightRect(int x, int y, int w, int h) throws Exception {
  		//s_logger.info("handleTightRect " + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
  		int comp_ctl = rfb.is.readUnsignedByte();
  		if (rfb.rec != null) {
  			if (rfb.recordFromBeginning ||
  					comp_ctl == (rfb.TightFill << 4) ||
  					comp_ctl == (rfb.TightJpeg << 4)) {
  				// Send data exactly as received.
  				rfb.rec.writeByte(comp_ctl);
  			} else {
  				// Tell the decoder to flush each of the four zlib streams.
  				rfb.rec.writeByte(comp_ctl | 0x0F);
  			}
  		}

  		// Flush zlib streams if we are told by the server to do so.
  		for (int stream_id = 0; stream_id < 4; stream_id++) {
  			if ((comp_ctl & 1) != 0 && tightInflaters[stream_id] != null) {
  				tightInflaters[stream_id] = null;
  			}
  			comp_ctl >>= 1;
  		}

  		// Check correctness of subencoding value.
  		if (comp_ctl > rfb.TightMaxSubencoding) {
  			throw new Exception("Incorrect tight subencoding: " + comp_ctl);
  		}

  		// Handle solid-color rectangles.
  		if (comp_ctl == rfb.TightFill) {

  			if (bytesPixel == 1) {
  				int idx = rfb.is.readUnsignedByte();
  				memGraphics.setColor(colors[idx]);
  				if (rfb.rec != null) {
  					rfb.rec.writeByte(idx);
  				}
  			} else {
  				byte[] buf = new byte[3];
  				rfb.readFully(buf);
  				if (rfb.rec != null) {
  					rfb.rec.write(buf);
  				}
  				Color bg = new Color(0xFF000000 | (buf[0] & 0xFF) << 16 |
  						(buf[1] & 0xFF) << 8 | (buf[2] & 0xFF));
  				memGraphics.setColor(bg);
  			}
  			memGraphics.fillRect(x, y, w, h);
  			scheduleRepaint(x, y, w, h);
  			return;

  		}

  		if (comp_ctl == rfb.TightJpeg) {

  			// Read JPEG data.
  			byte[] jpegData = new byte[rfb.readCompactLen()];
  			rfb.readFully(jpegData);
  			if (rfb.rec != null) {
  				if (!rfb.recordFromBeginning) {
  					rfb.recordCompactLen(jpegData.length);
  				}
  				rfb.rec.write(jpegData);
  			}

  			// Create an Image object from the JPEG data.
  			Image jpegImage = Toolkit.getDefaultToolkit().createImage(jpegData);

  			// Remember the rectangle where the image should be drawn.
  			jpegRect = new Rectangle(x, y, w, h);

  			// Let the imageUpdate() method do the actual drawing, here just
  			// wait until the image is fully loaded and drawn.
  			synchronized(jpegRect) {
  				Toolkit.getDefaultToolkit().prepareImage(jpegImage, -1, -1, this);
  				try {
  					// Wait no longer than three seconds.
  					jpegRect.wait(3000);
  				} catch (InterruptedException e) {
  					throw new Exception("Interrupted while decoding JPEG image");
  				}
  			}

  			// Done, jpegRect is not needed any more.
  			jpegRect = null;
  			return;
  		}

  		// Read filter id and parameters.
  		int numColors = 0, rowSize = w;
  		byte[] palette8 = new byte[2];
  		int[] palette24 = new int[256];
  		boolean useGradient = false;
  		if ((comp_ctl & rfb.TightExplicitFilter) != 0) {
  			int filter_id = rfb.is.readUnsignedByte();
  			if (rfb.rec != null) {
  				rfb.rec.writeByte(filter_id);
  			}
  			if (filter_id == rfb.TightFilterPalette) {
  				numColors = rfb.is.readUnsignedByte() + 1;
  				if (rfb.rec != null) {
  					rfb.rec.writeByte(numColors - 1);
  				}
  				if (bytesPixel == 1) {
  					if (numColors != 2) {
  						throw new Exception("Incorrect tight palette size: " + numColors);
  					}
  					rfb.readFully(palette8);
  					if (rfb.rec != null) {
  						rfb.rec.write(palette8);
  					}
  				} else {
  					byte[] buf = new byte[numColors * 3];
  					rfb.readFully(buf);
  					if (rfb.rec != null) {
  						rfb.rec.write(buf);
  					}
  					for (int i = 0; i < numColors; i++) {
  						palette24[i] = ((buf[i * 3] & 0xFF) << 16 |
    					  (buf[i * 3 + 1] & 0xFF) << 8 |
    					  (buf[i * 3 + 2] & 0xFF));
  					}
  				}
  				if (numColors == 2)
  					rowSize = (w + 7) / 8;
  			} else if (filter_id == rfb.TightFilterGradient) {
  				useGradient = true;
  			} else if (filter_id != rfb.TightFilterCopy) {
  				throw new Exception("Incorrect tight filter id: " + filter_id);
  			}
  		}
  		if (numColors == 0 && bytesPixel == 4)
  			rowSize *= 3;

  		// Read, optionally uncompress and decode data.
  		int dataSize = h * rowSize;
  		if (dataSize < rfb.TightMinToCompress) {
  			// 	Data size is small - not compressed with zlib.
  			if (numColors != 0) {
  				// Indexed colors.
  				byte[] indexedData = new byte[dataSize];
  				rfb.readFully(indexedData);
  				if (rfb.rec != null) {
  					rfb.rec.write(indexedData);
  				}
  				if (numColors == 2) {
  					// Two colors.
  					if (bytesPixel == 1) {
  						decodeMonoData(x, y, w, h, indexedData, palette8);
  					} else {
  						decodeMonoData(x, y, w, h, indexedData, palette24);
  					}
  				} else {
  					// 3..255 colors (assuming bytesPixel == 4).
  					int i = 0;
  					for (int dy = y; dy < y + h; dy++) {
  						for (int dx = x; dx < x + w; dx++) {
  							pixels24[dy * rfb.framebufferWidth + dx] =
  								palette24[indexedData[i++] & 0xFF];
  						}
  					}
  				}
  			} else if (useGradient) {
  				// "Gradient"-processed data
  				byte[] buf = new byte[w * h * 3];
  				rfb.readFully(buf);
  				if (rfb.rec != null) {
  					rfb.rec.write(buf);
  				}
  				decodeGradientData(x, y, w, h, buf);
  			} else {
  				// Raw truecolor data.
  				if (bytesPixel == 1) {
  					for (int dy = y; dy < y + h; dy++) {
  						rfb.readFully(pixels8, dy * rfb.framebufferWidth + x, w);
  						if (rfb.rec != null) {
  							rfb.rec.write(pixels8, dy * rfb.framebufferWidth + x, w);
  						}
  					}
  				} else {
  					byte[] buf = new byte[w * 3];
  					int i, offset;
  					for (int dy = y; dy < y + h; dy++) {
  						rfb.readFully(buf);
  						if (rfb.rec != null) {
  							rfb.rec.write(buf);
  						}
  						offset = dy * rfb.framebufferWidth + x;
  						for (i = 0; i < w; i++) {
  							pixels24[offset + i] =
  								(buf[i * 3] & 0xFF) << 16 |
  								(buf[i * 3 + 1] & 0xFF) << 8 |
  								(buf[i * 3 + 2] & 0xFF);
  						}
  					}
  				}
  			}
  		} else {
  			// Data was compressed with zlib.
  			int zlibDataLen = rfb.readCompactLen();
  			byte[] zlibData = new byte[zlibDataLen];
  			rfb.readFully(zlibData);
  			if (rfb.rec != null && rfb.recordFromBeginning) {
  				rfb.rec.write(zlibData);
  			}
  			int stream_id = comp_ctl & 0x03;
  			if (tightInflaters[stream_id] == null) {
  				tightInflaters[stream_id] = new Inflater();
  			}
  			Inflater myInflater = tightInflaters[stream_id];
  			myInflater.setInput(zlibData);
  			byte[] buf = new byte[dataSize];
  			myInflater.inflate(buf);
  			if (rfb.rec != null && !rfb.recordFromBeginning) {
  				rfb.recordCompressedData(buf);
  			}

  			if (numColors != 0) {
  				// Indexed colors.
  				if (numColors == 2) {
  					// Two colors.
  					if (bytesPixel == 1) {
  						decodeMonoData(x, y, w, h, buf, palette8);
  					} else {
  						decodeMonoData(x, y, w, h, buf, palette24);
  					}
  				} else {
  					// More than two colors (assuming bytesPixel == 4).
  					int i = 0;
  					for (int dy = y; dy < y + h; dy++) {
  						for (int dx = x; dx < x + w; dx++) {
  							pixels24[dy * rfb.framebufferWidth + dx] =
  								palette24[buf[i++] & 0xFF];
  						}
  					}
  				}
  			} else if (useGradient) {
  				// Compressed "Gradient"-filtered data (assuming bytesPixel == 4).
  				decodeGradientData(x, y, w, h, buf);
  			} else {
  				// Compressed truecolor data.
  				if (bytesPixel == 1) {
  					int destOffset = y * rfb.framebufferWidth + x;
  					for (int dy = 0; dy < h; dy++) {
  						System.arraycopy(buf, dy * w, pixels8, destOffset, w);
  						destOffset += rfb.framebufferWidth;
  					}
  				} else {
  					int srcOffset = 0;
  					int destOffset, i;
  					for (int dy = 0; dy < h; dy++) {
  						myInflater.inflate(buf);
  						destOffset = (y + dy) * rfb.framebufferWidth + x;
  						for (i = 0; i < w; i++) {
  							pixels24[destOffset + i] =
  								(buf[srcOffset] & 0xFF) << 16 |
  								(buf[srcOffset + 1] & 0xFF) << 8 |
  								(buf[srcOffset + 2] & 0xFF);
  							srcOffset += 3;
  						}
  					}
  				}
  			}
  		}

  		handleUpdatedPixels(x, y, w, h);
  		scheduleRepaint(x, y, w, h);
  	}

  	//
  	// Decode 1bpp-encoded bi-color rectangle (8-bit and 24-bit versions).
  	//

  	void decodeMonoData(int x, int y, int w, int h, byte[] src, byte[] palette) {

  		int dx, dy, n;
  		int i = y * rfb.framebufferWidth + x;
  		int rowBytes = (w + 7) / 8;
  		byte b;

  		for (dy = 0; dy < h; dy++) {
  			for (dx = 0; dx < w / 8; dx++) {
  				b = src[dy*rowBytes+dx];
  				for (n = 7; n >= 0; n--)
  					pixels8[i++] = palette[b >> n & 1];
  			}
  			for (n = 7; n >= 8 - w % 8; n--) {
  				pixels8[i++] = palette[src[dy*rowBytes+dx] >> n & 1];
  			}
  			i += (rfb.framebufferWidth - w);
  		}
  	}

  	void decodeMonoData(int x, int y, int w, int h, byte[] src, int[] palette) {

  		int dx, dy, n;
  		int i = y * rfb.framebufferWidth + x;
  		int rowBytes = (w + 7) / 8;
  		byte b;

  		for (dy = 0; dy < h; dy++) {
  			for (dx = 0; dx < w / 8; dx++) {
  				b = src[dy*rowBytes+dx];
  				for (n = 7; n >= 0; n--)
  					pixels24[i++] = palette[b >> n & 1];
  			}
  			for (n = 7; n >= 8 - w % 8; n--) {
  				pixels24[i++] = palette[src[dy*rowBytes+dx] >> n & 1];
  			}
  			i += (rfb.framebufferWidth - w);
  		}
  	}

  	//
  	// Decode data processed with the "Gradient" filter.
  	//

  	void decodeGradientData (int x, int y, int w, int h, byte[] buf) {

  		int dx, dy, c;
  		byte[] prevRow = new byte[w * 3];
  		byte[] thisRow = new byte[w * 3];
  		byte[] pix = new byte[3];
  		int[] est = new int[3];

  		int offset = y * rfb.framebufferWidth + x;

  		for (dy = 0; dy < h; dy++) {

  			/* First pixel in a row */
  			for (c = 0; c < 3; c++) {
  				pix[c] = (byte)(prevRow[c] + buf[dy * w * 3 + c]);
  				thisRow[c] = pix[c];
  			}
  			pixels24[offset++] =
  				(pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | (pix[2] & 0xFF);

  			/* Remaining pixels of a row */
  			for (dx = 1; dx < w; dx++) {
  				for (c = 0; c < 3; c++) {
  					est[c] = ((prevRow[dx * 3 + c] & 0xFF) + (pix[c] & 0xFF) -
    				  (prevRow[(dx-1) * 3 + c] & 0xFF));
  					if (est[c] > 0xFF) {
  						est[c] = 0xFF;
  					} else if (est[c] < 0x00) {
  						est[c] = 0x00;
  					}
  					pix[c] = (byte)(est[c] + buf[(dy * w + dx) * 3 + c]);
  					thisRow[dx * 3 + c] = pix[c];
  				}
  				pixels24[offset++] =
  					(pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | (pix[2] & 0xFF);
  			}

  			System.arraycopy(thisRow, 0, prevRow, 0, w * 3);
  			offset += (rfb.framebufferWidth - w);
  		}
  	}

  	//
  	// Display newly updated area of pixels.
  	//

  	void handleUpdatedPixels(int x, int y, int w, int h) {

  		pixelsSource.newPixels(x, y, w, h);
  		memGraphics.setClip(x, y, w, h);
  		memGraphics.drawImage(rawPixelsImage, 0, 0, null);
  		memGraphics.setClip(0, 0, rfb.framebufferWidth, rfb.framebufferHeight);
  	}

  	//
  	// Tell JVM to repaint specified desktop area.
  	//

  	void scheduleRepaint(int x, int y, int w, int h) {
  		if (rfb.framebufferWidth == scaledWidth) {
  			repaint(viewer.getDeferScreenUpdateTimeout(), x, y, w, h);
  		} else {
  			int sx = x * scalingFactor / 100;
  			int sy = y * scalingFactor / 100;
  			int sw = ((x + w) * scalingFactor + 49) / 100 - sx + 1;
  			int sh = ((y + h) * scalingFactor + 49) / 100 - sy + 1;
  			repaint(viewer.getDeferScreenUpdateTimeout(), sx, sy, sw, sh);
  		}
  	}

  	//
  	// Handle events.
  	//

  	public void keyPressed(KeyEvent evt) {
  		if (rfb != null)
  			processLocalKeyEvent(evt);
  	}
  	public void keyReleased(KeyEvent evt) {
  		if (rfb != null)
  			processLocalKeyEvent(evt);
  	}
  	public void keyTyped(KeyEvent evt) {
  		if (rfb != null)
  			evt.consume();
  	}

  	public void mousePressed(MouseEvent evt) {
  		if (rfb != null)
  			processLocalMouseEvent(evt, false);
  	}
  	public void mouseReleased(MouseEvent evt) {
  		if (rfb != null)
  			processLocalMouseEvent(evt, false);
  	}
  	public void mouseMoved(MouseEvent evt) {
  		if (rfb != null)
  			processLocalMouseEvent(evt, true);
  	}
  	public void mouseDragged(MouseEvent evt) {
  		if (rfb != null)
  			processLocalMouseEvent(evt, true);
  	}
  
  	public void sendCtrlAltDel() {
  		KeyEvent evt = new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 
  			InputEvent.CTRL_MASK | InputEvent.ALT_MASK, 127);
  		
		boolean bCloseRfb = false;
		synchronized (rfb) {
			try {
				rfb.writeKeyEvent(evt);
			} catch (IOException e) {
				// Just close the underlying stream so the main loop will error out.
				bCloseRfb = true;
			}
			rfb.notify();
		}
		
		if(bCloseRfb) {
			synchronized (viewer) {
				if (rfb != null)
					rfb.close();
			}
		}
  	}
  
  	public void processLocalKeyEvent(KeyEvent evt) {
  		if (/*viewer.rfb != null*/ viewer.getRfb() != null && rfb.inNormalProtocol) {
			if (!inputEnabled) {
				if ((evt.getKeyChar() == 'r' || evt.getKeyChar() == 'R')
						&& evt.getID() == KeyEvent.KEY_PRESSED) {
					// Request screen update.
					try {
						rfb.writeFramebufferUpdateRequest(0, 0,
							rfb.framebufferWidth, rfb.framebufferHeight,
							false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				// Input enabled.
				boolean bCloseRfb = false;
				synchronized (rfb) {
					try {
						// Convert Ctrl-Alt-Ins to Ctrl-Alt-Del
						if (evt.getKeyCode() == 155
								&& evt.getModifiers() == (InputEvent.CTRL_MASK | InputEvent.ALT_MASK)) {
							evt.setKeyCode(127);
						}
						rfb.writeKeyEvent(evt);
					} catch (IOException e) {
						// Just close the underlying stream so the main loop will error out.
						bCloseRfb = true;
					}
					rfb.notify();
				}
				
				if(bCloseRfb) {
					synchronized (viewer) {
						if (rfb != null)
							rfb.close();
					}
				}
			}
		}
		// Don't ever pass keyboard events to AWT for default processing.
		// Otherwise, pressing Tab would switch focus to ButtonPanel etc.
		evt.consume();
	}

  	public void processLocalMouseEvent(MouseEvent evt, boolean moved) {
  		if (/*viewer.rfb != null*/ viewer.getRfb() != null && rfb.inNormalProtocol) {
  			if (moved) {
  				softCursorMove(evt.getX(), evt.getY());
  			}
  			if (rfb.framebufferWidth != scaledWidth) {
  				int sx = (evt.getX() * 100 + scalingFactor/2) / scalingFactor;
  				int sy = (evt.getY() * 100 + scalingFactor/2) / scalingFactor;
  				evt.translatePoint(sx - evt.getX(), sy - evt.getY());
  			}

  			boolean bCloseRfb = false;
  			synchronized(rfb) {
  				try {
  					rfb.writePointerEvent(evt);
  				} catch (Exception e) {
  					bCloseRfb = true;
  				}
  				rfb.notify();
  			}

  			if(bCloseRfb) {
				synchronized (viewer) {
					if (rfb != null)
						rfb.close();
				}
  			}
  		}
  	}

  	//
  	// Ignored events.
  	//

  	public void mouseClicked(MouseEvent evt) {}
  	public void mouseEntered(MouseEvent evt) {}
  	public void mouseExited(MouseEvent evt) {}


  	//////////////////////////////////////////////////////////////////
  	//
  	// Handle cursor shape updates (XCursor and RichCursor encodings).
  	//

  	boolean showSoftCursor = false;

  	MemoryImageSource softCursorSource;
  	Image softCursor;

  	int cursorX = 0, cursorY = 0;
  	int cursorWidth, cursorHeight;
  	int origCursorWidth, origCursorHeight;
  	int hotX, hotY;
  	int origHotX, origHotY;

  	//
  	// Handle cursor shape update (XCursor and RichCursor encodings).
  	//

  	static int getCursorDataSize(int encodingType, int width, int height) 
  	{
  		if(width * height == 0)
  			return 0;
  		
		int bytesPerRow = (width + 7) / 8;
		int bytesMaskData = bytesPerRow * height;
		
		if (encodingType == RfbProto.EncodingXCursor) {
			return 6 + bytesMaskData * 2;
		} else {
			return width * height + bytesMaskData;
		}
  	}
  	
  	synchronized void handleCursorShapeUpdate(int encodingType,
			    int xhot, int yhot, int width, int height, byte[] captureBuffer)
  		throws IOException {

  		softCursorFree();

  		if (width * height == 0)
  			return;

  		// Ignore cursor shape data if requested by user.
  		if (/*viewer.options.ignoreCursorUpdates*/ viewer.ignoreCursorUpdate()) {
  			int bytesPerRow = (width + 7) / 8;
  			int bytesMaskData = bytesPerRow * height;

  			if (encodingType == rfb.EncodingXCursor) {
  				// rfb.is.skipBytes(6 + bytesMaskData * 2);
  				
  				rfb.is.readFully(captureBuffer);
  				
  			} else {
  				// rfb.EncodingRichCursor
  				// rfb.is.skipBytes(width * height + bytesMaskData);
  				
  				rfb.is.readFully(captureBuffer);
  			}
  			return;
  		}

  		// Decode cursor pixel data.
  		softCursorSource = decodeCursorShape(encodingType, width, height, captureBuffer);

  		// Set original (non-scaled) cursor dimensions.
  		origCursorWidth = width;
  		origCursorHeight = height;
  		origHotX = xhot;
  		origHotY = yhot;

  		// Create off-screen cursor image.
  		createSoftCursor();

  		// Show the cursor.
  		showSoftCursor = true;
  		repaint(viewer.getDeferCursorUpdateTimeout(),
    	    cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
  	}

  	//
  	// decodeCursorShape(). Decode cursor pixel data and return
  	// corresponding MemoryImageSource instance.
  	//

  	synchronized MemoryImageSource decodeCursorShape(int encodingType, int width, int height, byte[] captureBuffer)
  		throws IOException {

  		ByteArrayOutputStream bos = new ByteArrayOutputStream(captureBuffer.length);
  		
  		int bytesPerRow = (width + 7) / 8;
  		int bytesMaskData = bytesPerRow * height;

  		int[] softCursorPixels = new int[width * height];

  		if (encodingType == rfb.EncodingXCursor) {

  			// Read foreground and background colors of the cursor.
  			byte[] rgb = new byte[6];
  			rfb.readFully(rgb);
  			bos.write(rgb);
  			int[] colors = { (0xFF000000 | (rgb[3] & 0xFF) << 16 |
  					(rgb[4] & 0xFF) << 8 | (rgb[5] & 0xFF)),
  					(0xFF000000 | (rgb[0] & 0xFF) << 16 |
  							(rgb[1] & 0xFF) << 8 | (rgb[2] & 0xFF)) };

  			// Read pixel and mask data.
  			byte[] pixBuf = new byte[bytesMaskData];
  			rfb.readFully(pixBuf);
  			bos.write(pixBuf);
  			
  			byte[] maskBuf = new byte[bytesMaskData];
  			rfb.readFully(maskBuf);
  			bos.write(maskBuf);

  			// Decode pixel data into softCursorPixels[].
  			byte pixByte, maskByte;
  			int x, y, n, result;
  			int i = 0;
  			for (y = 0; y < height; y++) {
  				for (x = 0; x < width / 8; x++) {
  					pixByte = pixBuf[y * bytesPerRow + x];
  					maskByte = maskBuf[y * bytesPerRow + x];
  					for (n = 7; n >= 0; n--) {
  						if ((maskByte >> n & 1) != 0) {
  							result = colors[pixByte >> n & 1];
  						} else {
  							result = 0;	// Transparent pixel
  						}
  						softCursorPixels[i++] = result;
  					}
  				}
  				for (n = 7; n >= 8 - width % 8; n--) {
  					if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
  						result = colors[pixBuf[y * bytesPerRow + x] >> n & 1];
  					} else {
  						result = 0;		// Transparent pixel
  					}
  					softCursorPixels[i++] = result;
  				}
  			}
  		} else {
  			// encodingType == rfb.EncodingRichCursor

  			// Read pixel and mask data.
  			byte[] pixBuf = new byte[width * height * bytesPixel];
  			rfb.readFully(pixBuf);
  			bos.write(pixBuf);
  			
  			byte[] maskBuf = new byte[bytesMaskData];
  			rfb.readFully(maskBuf);
  			bos.write(maskBuf);

  			// Decode pixel data into softCursorPixels[].
  			byte pixByte, maskByte;
  			int x, y, n, result;
  			int i = 0;
  			for (y = 0; y < height; y++) {
  				for (x = 0; x < width / 8; x++) {
  					maskByte = maskBuf[y * bytesPerRow + x];
  					for (n = 7; n >= 0; n--) {
  						if ((maskByte >> n & 1) != 0) {
  							if (bytesPixel == 1) {
  								result = cm8.getRGB(pixBuf[i]);
  							} else {
  								result = 0xFF000000 |
  								(pixBuf[i * 4 + 2] & 0xFF) << 16 |
  								(pixBuf[i * 4 + 1] & 0xFF) << 8 |
  								(pixBuf[i * 4] & 0xFF);
  							}
  						} else {
  							result = 0;	// Transparent pixel
  						}
  						softCursorPixels[i++] = result;
  					}
  				}
  				for (n = 7; n >= 8 - width % 8; n--) {
  					if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
  						if (bytesPixel == 1) {
  							result = cm8.getRGB(pixBuf[i]);
  						} else {
  							result = 0xFF000000 |
  							(pixBuf[i * 4 + 2] & 0xFF) << 16 |
  							(pixBuf[i * 4 + 1] & 0xFF) << 8 |
  							(pixBuf[i * 4] & 0xFF);
  						}
  					} else {
  						result = 0;		// Transparent pixel
  					}
  					softCursorPixels[i++] = result;
  				}
  			}
  		}
  		
  		System.arraycopy(bos.toByteArray(), 0, captureBuffer, 0, captureBuffer.length);
  		return new MemoryImageSource(width, height, softCursorPixels, 0, width);
  	}

  	//
  	// createSoftCursor(). Assign softCursor new Image (scaled if necessary).
  	// Uses softCursorSource as a source for new cursor image.
  	//

  	synchronized void createSoftCursor() {

  		if (softCursorSource == null)
  			return;

/*    
    	int scaleCursor = viewer.options.scaleCursor;
*/    
  		int scaleCursor = viewer.getCursorScaleFactor();
  		if (scaleCursor == 0 || !inputEnabled)
  			scaleCursor = 100;

  		// Save original cursor coordinates.
  		int x = cursorX - hotX;
  		int y = cursorY - hotY;
  		int w = cursorWidth;
  		int h = cursorHeight;

  		cursorWidth = (origCursorWidth * scaleCursor + 50) / 100;
  		cursorHeight = (origCursorHeight * scaleCursor + 50) / 100;
  		hotX = (origHotX * scaleCursor + 50) / 100;
  		hotY = (origHotY * scaleCursor + 50) / 100;
  		softCursor = Toolkit.getDefaultToolkit().createImage(softCursorSource);

  		if (scaleCursor != 100) {
  			softCursor = softCursor.getScaledInstance(cursorWidth, cursorHeight,
						Image.SCALE_SMOOTH);
  		}

  		if (showSoftCursor) {
  			// Compute screen area to update.
  			x = Math.min(x, cursorX - hotX);
  			y = Math.min(y, cursorY - hotY);
  			w = Math.max(w, cursorWidth);
  			h = Math.max(h, cursorHeight);

  /*
      		repaint(viewer.deferCursorUpdates, x, y, w, h);
  */
  			repaint(viewer.getDeferCursorUpdateTimeout(), x, y, w, h);
  		}
  	}

  	//
  	// softCursorMove(). Moves soft cursor into a particular location.
  	//

  	synchronized void softCursorMove(int x, int y) {
  		int oldX = cursorX;
  		int oldY = cursorY;
  		cursorX = x;
  		cursorY = y;
  		if (showSoftCursor) {
  			repaint(viewer.getDeferCursorUpdateTimeout(),
  					oldX - hotX, oldY - hotY, cursorWidth, cursorHeight);
  			repaint(viewer.getDeferCursorUpdateTimeout(),
  					cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
    	
  		}
  	}

  	//
  	// softCursorFree(). Remove soft cursor, dispose resources.
  	//
  	public synchronized void softCursorFree() {
  		if (showSoftCursor) {
  			showSoftCursor = false;
  			softCursor = null;
  			softCursorSource = null;

  			repaint(viewer.getDeferCursorUpdateTimeout(),
  					cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
  		}
  	}
  
  	public void paintErrorString(String msg) {
  		if(memGraphics != null) {
	  		memGraphics.setColor(Color.BLACK);
	  		memGraphics.fillRect(0,0,memImage.getWidth(null),memImage.getHeight(null));
	  		memGraphics.setColor(Color.WHITE);
	  		memGraphics.setFont(new Font(null, Font.PLAIN, 20));
	  		FontMetrics fm = memGraphics.getFontMetrics();
	  		int width = fm.stringWidth(msg);
	  		int startx = (memImage.getWidth(null) - width) / 2;
	  		if (startx < 0) startx = 0;
	  		memGraphics.drawString(msg, startx, memImage.getHeight(null) / 2); 
	  		scheduleRepaint(0,0,memImage.getWidth(null),memImage.getHeight(null));
  		}
  	}
  	
  	public void encodeFramebufferResize(int w, int h, OutputStream os) throws IOException {
  		DataOutputStream dos;
  		if(os instanceof DataOutputStream)
  			dos = (DataOutputStream)os;
  		else
  			dos = new DataOutputStream(os);
  		
  		dos.writeByte(RfbProto.FramebufferUpdate);
  		dos.writeByte(0);		// padding
  		dos.writeShort(1);
  		dos.writeShort(0);
  		dos.writeShort(0);
  		dos.writeShort(w);
  		dos.writeShort(h);
  		dos.writeInt(RfbProto.EncodingNewFBSize);
  	}
  	
  	public void encodeCursorMove(int x, int y, OutputStream os) throws IOException {
  		DataOutputStream dos;
  		if(os instanceof DataOutputStream)
  			dos = (DataOutputStream)os;
  		else
  			dos = new DataOutputStream(os);
  		
  		dos.writeShort(x);
  		dos.writeShort(y);
  		dos.writeShort(0);
  		dos.writeShort(0);
  		dos.writeInt(RfbProto.EncodingPointerPos);
  	}
  	
  	public void encodeCursorShapeUpdate(int encodingType,
	    int xhot, int yhot, int width, int height, byte[] cursorData, OutputStream os) throws IOException {
  		
  		DataOutputStream dos;
  		if(os instanceof DataOutputStream)
  			dos = (DataOutputStream)os;
  		else
  			dos = new DataOutputStream(os);
  		
  		dos.writeShort(xhot);
  		dos.writeShort(yhot);
  		dos.writeShort(width);
  		dos.writeShort(height);
  		dos.writeInt(encodingType);
  		os.write(cursorData);
  	}
  	
  	public void encodeRawRectFrameUpdate(int x, int y, int w, int h, OutputStream os) throws IOException {
  		
  		DataOutputStream dos;
  		if(os instanceof DataOutputStream)
  			dos = (DataOutputStream)os;
  		else
  			dos = new DataOutputStream(os);
  		
  		PixelGrabber grabber = 
            new PixelGrabber(memImage, 0, 0, -1, -1, false);
 		
 		try {
			if(grabber.grabPixels()) {
				dos.writeShort(x);
				dos.writeShort(y);
				dos.writeShort(w);
				dos.writeShort(h);
				dos.writeInt(RfbProto.EncodingRaw);
				
				if (bytesPixel == 1) {
					byte[] data = (byte[])grabber.getPixels();
					for (int dy = y; dy < y + h; dy++) {
						os.write(data, dy * rfb.framebufferWidth + x, w);
					}
				} else {
					int[] data = (int[])grabber.getPixels();
					
					int i, offset;
					for (int dy = y; dy < y + h; dy++) {
						offset = dy * rfb.framebufferWidth + x;
						for (i = 0; i < w; i++) {
							int pixel = data[offset + i];
							dos.write(pixel & 0xFF);
							dos.write((pixel >> 8)& 0xFF);
							dos.write((pixel >> 16)& 0xFF);
							dos.write((pixel >> 24)& 0xFF);
						}
					}
				}
			}
		} catch (InterruptedException e) {
			s_logger.warn("Exception whiel grabbering pixel data from memImage");
		}
  	}
}

