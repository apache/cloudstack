/**
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.consoleproxy;


import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.cloud.console.AuthenticationException;
import com.cloud.console.ConsoleCanvas;
import com.cloud.console.ConsoleCanvas2;
import com.cloud.console.ITileScanListener;
import com.cloud.console.Region;
import com.cloud.console.RfbProto;
import com.cloud.console.RfbProtoAdapter;
import com.cloud.console.RfbViewer;
import com.cloud.console.TileInfo;
import com.cloud.console.TileTracker;

public class ConsoleProxyViewer implements java.lang.Runnable, RfbViewer, RfbProtoAdapter, ITileScanListener {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyViewer.class);
	
	public final static int STATUS_ERROR = -1;
	public final static int STATUS_UNINITIALIZED = 0;
	public final static int STATUS_CONNECTING = 1;
	public final static int STATUS_INITIALIZING = 2;
	public final static int STATUS_NORMAL_OPERATION = 3;
	public final static int STATUS_AUTHENTICATION_FAILURE = 100;
	
	public final static int SHIFT_KEY_MASK = 64;
	public final static int CTRL_KEY_MASK = 128;
	public final static int META_KEY_MASK = 256;
	public final static int ALT_KEY_MASK = 512;
	
	int id = getNextId();
	boolean compressServerMessage = false;
	long createTime = System.currentTimeMillis();
	long lastUsedTime = System.currentTimeMillis();
	int status;
	boolean dropMe = false;
	boolean viewerInReuse = false; 
	
	String host;
	int port;
	String tag = "";
	
	RfbProto rfb;
	Thread rfbThread;
	OutputStream clientStream;
	String clientStreamInfo;
	String passwordParam;
	
	ViewerOptions options;
	Frame vncFrame;
	ConsoleCanvas vc;
	Container vncContainer;
	
	boolean ajaxViewer = false;
	long ajaxSessionId = 0;
	TileTracker tracker;
	Object tileDirtyEvent;
	boolean dirtyFlag = false;
	boolean justCreated = true;
	AjaxFIFOImageCache ajaxImageCache = new AjaxFIFOImageCache(2);
	
	String cursorUpdatesDef;
	String eightBitColorsDef;
	
	int deferScreenUpdates;
	int deferCursorUpdates;
	int deferUpdateRequests;
	
	int[] encodingsSaved;
	int nEncodingsSaved;

	boolean framebufferResized = false;
	int resizedFramebufferWidth;
	int resizedFramebufferHeight;
	
	boolean cursorMoved = false;
	int lastCursorPosX;
	int lastCursorPosY;
	
	boolean cursorShapeChanged = false;
	int lastCursorShapeEncodingType;
	int lastCursorShapeHotX;
	int lastCursorShapeHotY;
	int lastCursorShapeWidth;
	int lastCursorShapeHeight;
	byte[] lastCursorShapeData;
	
	static int id_count = 1;
	synchronized static int getNextId() {
		return id_count++;
	}

	public void init() {
		initProxy();
	}
	
	private void initProxy() {
		options = new ViewerOptions();
		options.viewOnly = true;

		cursorUpdatesDef = null;
		eightBitColorsDef = null;
		
		tracker = new TileTracker();
		tracker.initTracking(64, 64, 800, 600);

		if(rfbThread != null) {
			if(rfbThread.isAlive()) {
				dropMe = true;
				viewerInReuse = true;
				if(rfb != null)
					rfb.close();
				
				try {
					rfbThread.join();
				} catch (InterruptedException e) {
					s_logger.warn("InterruptedException while waiting for RFB thread to exit");
				}
				viewerInReuse = false;
			}
		}
		
		dropMe = false;
		rfbThread = new Thread(this);
		rfbThread.setName("RFB Thread " + rfbThread.getId() + " >" + host + ":" + port);
		rfbThread.start();

		tileDirtyEvent = new Object();
	}
	
	public synchronized boolean justCreated() {
		if(justCreated) {
			justCreated = false;
			return true;
		}
		return false;
	}
	
	public boolean isDropped() {
		return dropMe;
	}
	
	public void run() {
		createCanvas(0, 0);

		int retries = 0;
		while (!dropMe) {
			try {
				s_logger.info("Connecting to VNC server");
				status = STATUS_CONNECTING;
				connectAndAuthenticate();
				retries = 0; // reset the retry count
				status = STATUS_INITIALIZING;
				doProtocolInitialisation();
				vc.rfb = rfb;
				vc.setPixelFormat();

				// if we have a client current connected, when we have reconnected to the server and
				// received a new ServerInit info (in doProtocolInitialisation()), we will
				// convert it into frame buffer size change to make sure following on updates
				// don't fall out of range
				//
				if(clientStream != null) {
					// 128 bytes will be enough for this single PDU
					s_logger.info("Send init framebuffer size (" + rfb.framebufferWidth + ", " + rfb.framebufferHeight + ")");
					
					ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
					try {
						vc.encodeFramebufferResize(rfb.framebufferWidth, rfb.framebufferHeight, bos);
					} catch(IOException e) {
					}
					writeToClientStream(bos.toByteArray());
				}
				
				vc.rfb.writeFramebufferUpdateRequest(0, 0,
						vc.rfb.framebufferWidth, vc.rfb.framebufferHeight,
						true);
				status = STATUS_NORMAL_OPERATION;
				vc.processNormalProtocol();
			} catch (AuthenticationException e) {
				status = STATUS_AUTHENTICATION_FAILURE;
				String msg = e.getMessage();
				s_logger.warn("Authentication exception, msg: " + msg + "sid: " + this.passwordParam);
			} catch(OutOfMemoryError e) {
				s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
				System.exit(1);
			} catch (Exception e) {
				status = STATUS_ERROR;
				s_logger.error("Unexpected exception ", e);
			} finally {
				// String oldName = Thread.currentThread().getName();
				encodingsSaved = null;
				nEncodingsSaved = 0;
				
				s_logger.info("Close current RFB");
				synchronized (this) {
					if (rfb != null) {
						rfb.close();
					}
				}
			}
			if (dropMe) {
				break;
			}
			if (status == STATUS_AUTHENTICATION_FAILURE) {
				break;
			} else {
				retries++;
				if(retries > ConsoleProxy.reconnectMaxRetry) {
					s_logger.info("Exception caught, retry has reached to maximum : " + retries + ", will give up and disconnect client");
					break;
				}
				
				s_logger.info("Exception caught, retrying in 1 second, current retry:" + retries);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignored
				}
			}
		}
		
		// make sure we remove it from the management map upon main thread termination
		dropMe = true;
		
		// if we are reusing the viewer object, we shouldn't remove it from the map
		// this can also prevent deadlock in initProxy() while initProxy tries to join
		// the thread, as initProxy() is called with ConsoleProxy.connectionMap being locked
		// while CoonsoleProxy.removeViewer() here will attempt to lock it from another thread
		if(!viewerInReuse)		
			ConsoleProxy.removeViewer(this);
		s_logger.info("RFB thread terminating");
	}
	
	void connectAndAuthenticate() throws Exception {
		s_logger.info("Initializing...");
		
		s_logger.info("Ensure ip route towards host " + host);
		ConsoleProxy.ensureRoute(host);		
		
		s_logger.info("Connecting to " + host + ", port " + port + "...");
		rfb = new RfbProto(host, port, this);
		s_logger.info("Connected to server");

		rfb.readVersionMsg();
		s_logger.info("RFB server supports protocol version "
				+ rfb.serverMajor + "." + rfb.serverMinor);

		rfb.writeVersionMsg();
		s_logger.info("Using RFB protocol version " + rfb.clientMajor
				+ "." + rfb.clientMinor);

		int secType = rfb.negotiateSecurity();
		int authType;
		if (secType == RfbProto.SecTypeTight) {
			s_logger.info("Enabling TightVNC protocol extensions");
			rfb.initCapabilities();
			rfb.setupTunneling();
			authType = rfb.negotiateAuthenticationTight();
		} else {
			authType = secType;
		}

		switch (authType) {
		case RfbProto.AuthNone:
			s_logger.info("No authentication needed");
			rfb.authenticateNone();
			break;
		case RfbProto.AuthVNC:
			s_logger.info("Performing standard VNC authentication");
			if (passwordParam != null) {
				rfb.authenticateVNC(passwordParam);
			} else {
				throw new AuthenticationException("Bad password");
			}
			break;
		default:
			throw new Exception("Unknown authentication scheme " + authType);
		}
	}
	
	static void authenticationExternally(String host, String port, String tag, String sid, String ticket) throws AuthenticationException {
/*		
		if(ConsoleProxy.management_host != null) {
			try {
				boolean success = false;
				URL url = new URL(ConsoleProxy.management_host + "/console?cmd=auth&vm=" + getTag() + "&sid=" + passwordParam);
				
				URLConnection conn = url.openConnection();
				
				// setting TIMEOUTs to avoid possible waiting until death situations
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);
				
		        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		        String inputLine;
		        if ((inputLine = in.readLine()) != null) { 
		            if(inputLine.equals("success"))
		            	success = true;
		        }
		        in.close();
		        
		        if(!success) {
		        	if(s_logger.isInfoEnabled())
		        		s_logger.info("External authenticator failed authencation request for vm " + getTag() + " with sid " + passwordParam);
		        	
					throw new AuthenticationException("Unable to contact external authentication source " + ConsoleProxy.management_host);
		        }
			} catch (MalformedURLException e) {
				s_logger.error("Unexpected exception " + e.getMessage(), e);
			} catch(IOException e) {
				s_logger.error("Unable to contact external authentication source due to " + e.getMessage(), e);
				throw new AuthenticationException("Unable to contact external authentication source " + ConsoleProxy.management_host);
			}
		} else {
			s_logger.warn("No external authentication source being setup.");
		}
*/
		if(!ConsoleProxy.authenticateConsoleAccess(host, port, tag, sid, ticket)) {
    		s_logger.warn("External authenticator failed authencation request for vm " + tag + " with sid " + sid);
        	
			throw new AuthenticationException("External authenticator failed request for vm " + tag + " with sid " + sid);
		}
	}
	
	void doProtocolInitialisation() throws IOException {
		rfb.writeClientInit();
		rfb.readServerInit();
		setEncodings();
	}
	
	void setEncodings() {
		setEncodings(false);
	}
	
	void setEncodings(boolean autoSelectOnly) {
		if (options == null || rfb == null || !rfb.inNormalProtocol)
			return;

		options.preferredEncoding = RfbProto.EncodingHextile;
		
		int preferredEncoding = options.preferredEncoding;
		if (preferredEncoding == -1) {
			long kbitsPerSecond = rfb.kbitsPerSecond();
			if (nEncodingsSaved < 1) {
				// Choose Tight or ZRLE encoding for the very first update.
				// Logger.log(Logger.INFO, "Using Tight/ZRLE encodings");
				preferredEncoding = RfbProto.EncodingTight;
			} else if (kbitsPerSecond > 2000
					&& encodingsSaved[0] != RfbProto.EncodingHextile) {
				// Switch to Hextile if the connection speed is above 2Mbps.
				s_logger.info("Throughput " + kbitsPerSecond
						+ " kbit/s - changing to Hextile encoding");
				preferredEncoding = RfbProto.EncodingHextile;
			} else if (kbitsPerSecond < 1000
					&& encodingsSaved[0] != RfbProto.EncodingTight) {
				// Switch to Tight/ZRLE if the connection speed is below 1Mbps.
				s_logger.info("Throughput " + kbitsPerSecond
						+ " kbit/s - changing to Tight/ZRLE encodings");
				preferredEncoding = RfbProto.EncodingTight;
			} else {
				// Don't change the encoder.
				if (autoSelectOnly)
					return;
				preferredEncoding = encodingsSaved[0];
			}
		} else {
			// Auto encoder selection is not enabled.
			if (autoSelectOnly)
				return;
		}

		int[] encodings = new int[20];
		int nEncodings = 0;

		encodings[nEncodings++] = preferredEncoding;
		if (options.useCopyRect) {
			encodings[nEncodings++] = RfbProto.EncodingCopyRect;
		}

		if (preferredEncoding != RfbProto.EncodingTight) {
			encodings[nEncodings++] = RfbProto.EncodingTight;
		}
		if (preferredEncoding != RfbProto.EncodingZRLE) {
			encodings[nEncodings++] = RfbProto.EncodingZRLE;
		}
		if (preferredEncoding != RfbProto.EncodingHextile) {
			encodings[nEncodings++] = RfbProto.EncodingHextile;
		}
		if (preferredEncoding != RfbProto.EncodingZlib) {
			encodings[nEncodings++] = RfbProto.EncodingZlib;
		}
		if (preferredEncoding != RfbProto.EncodingCoRRE) {
			encodings[nEncodings++] = RfbProto.EncodingCoRRE;
		}
		if (preferredEncoding != RfbProto.EncodingRRE) {
			encodings[nEncodings++] = RfbProto.EncodingRRE;
		}

		if (options.compressLevel >= 0 && options.compressLevel <= 9) {
			encodings[nEncodings++] = RfbProto.EncodingCompressLevel0
					+ options.compressLevel;
		}
		if (options.jpegQuality >= 0 && options.jpegQuality <= 9) {
			encodings[nEncodings++] = RfbProto.EncodingQualityLevel0
					+ options.jpegQuality;
		}

		if (options.requestCursorUpdates) {
			encodings[nEncodings++] = RfbProto.EncodingXCursor;
			encodings[nEncodings++] = RfbProto.EncodingRichCursor;
			if (!options.ignoreCursorUpdates)
				encodings[nEncodings++] = RfbProto.EncodingPointerPos;
		}

		encodings[nEncodings++] = RfbProto.EncodingLastRect;
		encodings[nEncodings++] = RfbProto.EncodingNewFBSize;

		boolean encodingsWereChanged = false;
		if (nEncodings != nEncodingsSaved) {
			encodingsWereChanged = true;
		} else {
			for (int i = 0; i < nEncodings; i++) {
				if (encodings[i] != encodingsSaved[i]) {
					encodingsWereChanged = true;
					break;
				}
			}
		}

		if (encodingsWereChanged) {
			try {
				rfb.writeSetEncodings(encodings, nEncodings);
				if (vc != null) {
					vc.softCursorFree();
				}
			} catch (Exception e) {
				s_logger.error(e.toString(), e);
			}
			encodingsSaved = encodings;
			nEncodingsSaved = nEncodings;
		}
	}

	protected void startRecording() throws IOException {
	}
	
	protected void stopRecording() throws IOException {
	}
	
	void createCanvas(int maxWidth, int maxHeight) {
		vc = new ConsoleCanvas2(this, maxWidth, maxHeight);
		
	    if (!options.viewOnly)
	        vc.enableInput(true);
	}
	
	synchronized void writeToClientStream(byte[] bs) {
		// writeToClientStream swallows exceptions to make sure problems writing
		// to client stream do not impact the main loop
		if (clientStream != null) {
			try {
				lastUsedTime = System.currentTimeMillis();
				synchronized (clientStream) {
					clientStream.write(bs);
					clientStream.flush();
				}
			} catch (IOException e) {
				if(s_logger.isDebugEnabled()) {
					s_logger.debug("Writing to client stream failed, reason: " + e.getMessage());
				}
				try {
					clientStream.close();
				} catch (IOException ioe){
					// ignore
				}
				clientStream = null;
				clientStreamInfo = null;
			  }
		}
	}
	
	//
	// Implement RfbViewer interface
	//
	public boolean isProxy() {
		return true;
	}
	
	public boolean hasClientConnection() {
		// always return false if the viewer is AJAX viewer
		if(ajaxViewer)
			return false;
		
		return clientStream != null;
	}
	
	public RfbProto getRfb() {
		return rfb;
	}
	
	public Dimension getScreenSize() {
		return (new Frame()).getToolkit().getScreenSize();
		// return vncFrame.getToolkit().getScreenSize();
	}
	
	public Dimension getFrameSize() {
		// return vncFrame.getSize();
		return getScreenSize();
	}
	
	public int getScalingFactor() {
		return options.scalingFactor;
	}
	
	public int getCursorScaleFactor() {
		return options.scaleCursor;
	}
	
	public boolean ignoreCursorUpdate() {
		return options.ignoreCursorUpdates;
	}
	
	public int getDeferCursorUpdateTimeout() {
		return 0;
		// return deferCursorUpdates; 
	}
	
	public int getDeferScreenUpdateTimeout() {
		return 0;
		// return deferScreenUpdates;
	}
	
	public int getDeferUpdateRequestTimeout() {
		return 0;
		//return deferUpdateRequests;
	}
	
	public int setPixelFormat(RfbProto rfb) throws IOException {
		if (options.eightBitColors) {
	        rfb.writeSetPixelFormat(8, 8, false, true, 7, 7, 3, 0, 3, 6);
	        return 1;
		} else {
			rfb.writeSetPixelFormat(32, 24, false, true, 255, 255, 255, 16, 8, 0);
			return 4;
		}
	}
	
	public void onInputEnabled(boolean enable) {
		// do nothing in proxy viewer
	}
	
	public void onFramebufferSizeChange(int w, int h) {
		tracker.resize(vc.scaledWidth, vc.scaledHeight);

		synchronized(this) {
			framebufferResized = true;
			resizedFramebufferWidth = w;
			resizedFramebufferHeight = h;
		}
		
		signalTileDirtyEvent();
	}
	
	public void onFramebufferUpdate(int x, int y, int w, int h) {
		if(s_logger.isTraceEnabled())
			s_logger.trace("Frame buffer update {" + x + "," + y + "," + w + "," + h + "}");
		tracker.invalidate(new Rectangle(x, y, w, h));
		
		signalTileDirtyEvent();
	}
	
	public void onFramebufferCursorMove(int x, int y) {
		synchronized(this) {
			cursorMoved = true;
			lastCursorPosX = x;
			lastCursorPosY = y;
		}
		
		signalTileDirtyEvent();
	}
	
	public void onFramebufferCursorShapeChange(int encodingType,
		    int xhot, int yhot, int width, int height, byte[] cursorData) {

		synchronized(this) {
			cursorShapeChanged = true;
			
			lastCursorShapeEncodingType = encodingType;
			lastCursorShapeHotX = xhot;
			lastCursorShapeHotY = yhot;
			lastCursorShapeWidth = width;
			lastCursorShapeHeight = height;
			lastCursorShapeData = cursorData;
		}
		
		signalTileDirtyEvent();
	}
	
	public void onDesktopResize() {
		if(vncFrame != null)
			vncFrame.pack();
	}
	
	public void onFrameResize(Dimension newSize) {
		if(vncFrame != null)
			vncFrame.setSize(newSize);
	}
	
	public void onDisconnectMessage() {
		// do nothing in viewer mode
	}
	
	public void onBellMessage() {
		Toolkit.getDefaultToolkit().beep();
	}
	
	public void onPreProtocolProcess(byte[] bs) throws IOException {
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Send " + (bs != null ? bs.length  : 0) + " bytes (original) to client");

		if (!ajaxViewer && bs != null && clientStream != null) {
			if(s_logger.isInfoEnabled())
				s_logger.info("getSplit got " + bs.length + " bytes");
			if (compressServerMessage && bs.length > 10000) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(256000);
				GZIPOutputStream gos = new GZIPOutputStream(bos, 65536);
				gos.write(bs);
				gos.finish();
				byte[] nbs = bos.toByteArray();
				gos.close();
				int n = nbs.length;
				
				if(s_logger.isInfoEnabled())
					s_logger.info("Compressed " + bs.length + "=>" + n);
				
				byte[] b = new byte[6];
				b[0] = (byte) 250;
				b[1] = 2;
				b[2] = (byte) ((n >> 24) & 0xff);
				b[3] = (byte) ((n >> 16) & 0xff);
				b[4] = (byte) ((n >> 8) & 0xff);
				b[5] = (byte) (n & 0xff);
				
				// make sure two seperated writes completed atomically
				synchronized(clientStream) {
					writeToClientStream(b);
					writeToClientStream(nbs);
				}
			} else {
				if(s_logger.isInfoEnabled())
					s_logger.info("Send uncompressed " + bs.length + " bytes to client");
				
				writeToClientStream(bs);
			}
		} else {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Client is not connected, ignore forwarding " + (bs != null ? bs.length : 0) + " bytes to client");
		}
		
	    rfb.sis.setSplit();
	}
	
	public boolean onPostFrameBufferUpdateProcess(boolean cursorPosReceived) throws IOException {
		boolean fullUpdateNeeded = false;
		
		// Defer framebuffer update request if necessary. But wake up
		// immediately on keyboard or mouse event. Also, don't sleep
		// if there is some data to receive, or if the last update
		// included a PointerPos message.
		if (deferUpdateRequests > 0 && rfb.is.available() == 0 && !cursorPosReceived) {
		  synchronized(vc.rfb) {
		    try {
		      vc.rfb.wait(deferUpdateRequests);
		    } catch (InterruptedException e) {
		    }
		  }
		}

		// Before requesting framebuffer update, check if the pixel
		// format should be changed. If it should, request full update
		// instead of an incremental one.
		if (options.eightBitColors != (vc.bytesPixel == 1)) {
			vc.setPixelFormat();
			fullUpdateNeeded = true;
		}

		return fullUpdateNeeded;
	}
	
	public void onProtocolProcessException(IOException e) {
		byte[] bs = new byte[2];
		bs[0] = (byte)250;
		bs[1] = 1;
		writeToClientStream(bs);
	}
	
	public Socket createConnection(String host, int port) throws IOException {
		Socket sock = new Socket();
		sock.setSoTimeout(ConsoleProxy.readTimeoutSeconds*1000);
		sock.setKeepAlive(true);
		sock.connect(new InetSocketAddress(host, port), 30000);
		return sock;
	}
	
	public void writeInit(OutputStream os) throws IOException {
	    if (options.shareDesktop) {
	        os.write(1);
	    } else {
	    	os.write(0);
	    }
	}
	
	public void swapMouseButton(Integer[] masks) {
	    if (options.reverseMouseButtons2And3) {
	    	Integer temp = masks[1];
	    	masks[1] = masks[0];
	    	masks[0] = temp;
	      }
	}
	
	public boolean onTileChange(Rectangle rowMergedRect, int row, int col) {
		// currently we don't do scan-based client update
		return true;
	}
	
	public void onRegionChange(List<Region> regionList) {
		// obsolute
	}
	
	private void signalTileDirtyEvent() {
		synchronized(tileDirtyEvent) {
			dirtyFlag = true;
			tileDirtyEvent.notifyAll();
		}
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	//
	// AJAX Image manipulation 
	//
	public void copyTile(Graphics2D g, int x, int y, Rectangle rc) {
		if(vc != null && vc.memImage != null) {
			synchronized(vc.memImage) {
				g.drawImage(vc.memImage, x, y, x + rc.width, y + rc.height, 
					rc.x, rc.y, rc.x + rc.width, rc.y + rc.height, null);
			}
		}
	}
	
	public byte[] getFrameBufferJpeg() {
		int width = 800;
		int height = 600;
		if(vc != null) {
			width = vc.scaledWidth;
			height = vc.scaledHeight;
		}
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("getFrameBufferJpeg, w: " + width + ", h: " + height);
		
		BufferedImage bufferedImage = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		if(vc != null && vc.memImage != null) {
			synchronized(vc.memImage) {
				Graphics2D g = bufferedImage.createGraphics();
				g.drawImage(vc.memImage, 0, 0, width, height, 0, 0, width, height, null);
			}
		}
		
		byte[] imgBits = null;
		try {
			imgBits = jpegFromImage(bufferedImage);
		} catch (IOException e) {
		}
		return imgBits;
	}
	
	public byte[] getTilesMergedJpeg(List<TileInfo> tileList, int tileWidth, int tileHeight) {
		
		int width = Math.max(tileWidth, tileWidth*tileList.size());
		BufferedImage bufferedImage = new BufferedImage(width, tileHeight,
			BufferedImage.TYPE_3BYTE_BGR);
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Create merged image, w: " + width + ", h: " + tileHeight);
		
		if(vc != null && vc.memImage != null) {
			synchronized(vc.memImage) {
				Graphics2D g = bufferedImage.createGraphics();
				int i = 0;
				for(TileInfo tile : tileList) {
					Rectangle rc = tile.getTileRect();
					
					if(s_logger.isTraceEnabled())
						s_logger.trace("Merge tile into jpeg from (" + rc.x + "," + rc.y + "," + (rc.x + rc.width) + "," + (rc.y + rc.height) + ") to (" + i*tileWidth + ",0)" );
					
					g.drawImage(vc.memImage, i*tileWidth, 0, i*tileWidth + rc.width, rc.height, 
						rc.x, rc.y, rc.x + rc.width, rc.y + rc.height, null);
					
					i++;
				}
			}
		}
		
		byte[] imgBits = null;
		try {
			imgBits = jpegFromImage(bufferedImage);
			
			if(s_logger.isTraceEnabled())
				s_logger.trace("Merge jpeg image size: " + imgBits.length + ", tiles: " + tileList.size());
		} catch (IOException e) {
		}
		return imgBits;
	}
	
	public byte[] jpegFromImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(128000);
		javax.imageio.ImageIO.write(image, "jpg", bos);
		
		byte[] jpegBits = bos.toByteArray();
		bos.close();
		return jpegBits;
	}
	
	private String prepareAjaxImage(List<TileInfo> tiles, boolean init) {
		byte[] imgBits;
		if(init)
			imgBits = getFrameBufferJpeg();
		else 
			imgBits = getTilesMergedJpeg(tiles, tracker.getTileWidth(), tracker.getTileHeight());
		
		if(imgBits == null) {
			s_logger.warn("Unable to generate jpeg image");
		} else {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Generated jpeg image size: " + imgBits.length);
		}
		
		int key = ajaxImageCache.putImage(imgBits);
		StringBuffer sb = new StringBuffer("/ajaximg?host=");
		sb.append(host).append("&port=").append(port).append("&sid=").append(passwordParam);
		sb.append("&key=").append(key).append("&ts=").append(System.currentTimeMillis());
		return sb.toString(); 
	}
	
	private String prepareAjaxSession(boolean init) {
		StringBuffer sb = new StringBuffer();
		
		if(init)
			ajaxSessionId++;
		
		sb.append("/ajax?host=").append(host).append("&port=").append(port);
		sb.append("&sid=").append(passwordParam).append("&sess=").append(ajaxSessionId);
		return sb.toString();
	}

	public String onAjaxClientKickoff() {
		return "onKickoff();";
	}
	
	private boolean waitForViewerReady() {
		long startTick = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTick < 5000) {
			if(this.status == ConsoleProxyViewer.STATUS_NORMAL_OPERATION)
				return true;
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}
	
	private String onAjaxClientConnectFailed() {
		return "<html><head></head><body><div id=\"main_panel\" tabindex=\"1\"><p>" + 
			"Unable to start console session as connection is refused by the machine you are accessing" +
			"</p></div></body></html>";
	}
	
	public String onAjaxClientStart(String title, List<String> languages, String guest) {
		if(!waitForViewerReady())
			return onAjaxClientConnectFailed();

		// make sure we switch to AJAX view on start
		setAjaxViewer(true);
			
		int tileWidth = tracker.getTileWidth();
		int tileHeight = tracker.getTileHeight();
		int width = tracker.getTrackWidth();
		int height = tracker.getTrackHeight();
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Ajax client start, frame buffer w: " + width + ", " + height);
		
		synchronized(this) {
			if(framebufferResized) {
				framebufferResized = false;
			}
		}
		
		int retry = 0;
		if(justCreated()) {
			tracker.initCoverageTest();
			
			try {
				rfb.writeFramebufferUpdateRequest(0, 0, tracker.getTrackWidth(), tracker.getTrackHeight(), false);
			
				while(!tracker.hasFullCoverage() && retry < 10) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					retry++;
				}
			} catch (IOException e1) {
				s_logger.warn("Connection was broken ");
			}
		}
		
		List<TileInfo> tiles = tracker.scan(true);
		String imgUrl = prepareAjaxImage(tiles, true);
		String updateUrl = prepareAjaxSession(true);
		
		StringBuffer sbTileSequence = new StringBuffer();
		int i = 0;
		for(TileInfo tile : tiles) {
			sbTileSequence.append("[").append(tile.getRow()).append(",").append(tile.getCol()).append("]");
			if(i < tiles.size() - 1)
				sbTileSequence.append(",");
			
			i++;
		}

/*		
		SimpleHash model = new SimpleHash();
		model.put("tileSequence", sbTileSequence.toString());
		model.put("imgUrl", imgUrl);
		model.put("updateUrl", updateUrl);
		model.put("width", String.valueOf(width));
		model.put("height", String.valueOf(height));
		model.put("tileWidth", String.valueOf(tileWidth));
		model.put("tileHeight", String.valueOf(tileHeight));
		model.put("title", title);
		model.put("rawKeyboard", ConsoleProxy.keyboardType == ConsoleProxy.KEYBOARD_RAW ? "true" : "false");
		
		StringWriter writer = new StringWriter();
		try {
			ConsoleProxy.processTemplate("viewer.ftl", model, writer);
		} catch (IOException e) {
			s_logger.warn("Unexpected exception in processing template.", e);
		} catch (TemplateException e) {
			s_logger.warn("Unexpected exception in processing template.", e);
		}
		StringBuffer sb = writer.getBuffer();
		if(s_logger.isTraceEnabled())
			s_logger.trace("onAjaxClientStart response: " + sb.toString());
		return sb.toString();
	*/
		return getAjaxViewerPageContent(sbTileSequence.toString(), imgUrl, 
				updateUrl, width, height, tileWidth, tileHeight, title, 
				ConsoleProxy.keyboardType == ConsoleProxy.KEYBOARD_RAW, languages, guest);
	}
	
	private String getAjaxViewerPageContent(String tileSequence, String imgUrl, String updateUrl, int width,
		int height, int tileWidth, int tileHeight, String title, boolean rawKeyboard, List<String> languages, String guest) {

		StringBuffer sbLanguages = new StringBuffer("");
		if(languages != null) {
			for(String lang : languages) {
				if(sbLanguages.length() > 0) {
					sbLanguages.append(",");
				}
				sbLanguages.append(lang);
			}
		}
		
		boolean linuxGuest = true;
		if(guest != null && guest.equalsIgnoreCase("windows"))
			linuxGuest = false;
		
		String[] content = new String[] {
			"<html>",
			"<head>",
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/resource/js/jquery.js\"></script>",
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/resource/js/cloud.logger.js\"></script>",
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/resource/js/ajaxviewer.js\"></script>",
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/resource/js/handler.js\"></script>",
			"<link rel=\"stylesheet\" type=\"text/css\" href=\"/resource/css/ajaxviewer.css\"></link>",
			"<link rel=\"stylesheet\" type=\"text/css\" href=\"/resource/css/logger.css\"></link>",
			"<title>" + title + "</title>",
			"</head>",
			"<body>",
			"<div id=\"toolbar\">",
			"<ul>",
				"<li>", 
					"<a href=\"#\" cmd=\"sendCtrlAltDel\">", 
						"<span><img align=\"left\" src=\"/resource/images/cad.gif\" alt=\"Ctrl-Alt-Del\" />Ctrl-Alt-Del</span>", 
					"</a>", 
				"</li>",
				"<li>", 
					"<a href=\"#\" cmd=\"sendCtrlEsc\">", 
						"<span><img align=\"left\" src=\"/resource/images/winlog.png\" alt=\"Ctrl-Esc\" style=\"width:16px;height:16px\"/>Ctrl-Esc</span>",
					"</a>", 
				"</li>",
				
				"<li class=\"pulldown\">", 
					"<a href=\"#\">", 
						"<span><img align=\"left\" src=\"/resource/images/winlog.png\" alt=\"Keyboard\" style=\"width:16px;height:16px\"/>Keyboard</span>",
					"</a>", 
					"<ul>",
		    			"<li><a href=\"#\" cmd=\"keyboard_us\"><span>Standard (US) keyboard</span></a></li>",
		    			"<li><a href=\"#\" cmd=\"keyboard_jp\"><span>Japanese keyboard</span></a></li>",
					"</ul>",
				"</li>",
			"</ul>",
			"<span id=\"light\" class=\"dark\" cmd=\"toggle_logwin\"></span>", 
			"</div>",
			"<div id=\"main_panel\" tabindex=\"1\"></div>",
			"<script language=\"javascript\">",
			"var acceptLanguages = '" + sbLanguages.toString() + "';",
			"var tileMap = [ " + tileSequence + " ];",
			"var ajaxViewer = new AjaxViewer('main_panel', '" + imgUrl + "', '" + updateUrl + "', tileMap, ", 
				String.valueOf(width) + ", " + String.valueOf(height) + ", " + String.valueOf(tileWidth) + ", " + String.valueOf(tileHeight) + ");",

			"$(function() {",
				"ajaxViewer.start();",
			"});",

			"</script>",
			"</body>",
			"</html>"	
		};
		
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < content.length; i++)
			sb.append(content[i]);
		
		return sb.toString();
	}
	
	public String onAjaxClientDisconnected() {
		return "onDisconnect();";
	}
	
	public String onAjaxClientUpdate() {
		if(!waitForViewerReady())
			return onAjaxClientDisconnected();
		
		synchronized(tileDirtyEvent) {
			if(!dirtyFlag) {
				try {
					tileDirtyEvent.wait(3000);
				} catch(InterruptedException e) {
				}
			}
		}
		
		boolean doResize = false;
		synchronized(this) {
			if(framebufferResized) {
				framebufferResized = false;
				doResize = true;
			}
		}
		
		List<TileInfo> tiles;
		
		if(doResize)
			tiles = tracker.scan(true);
		else
			tiles = tracker.scan(false);
		dirtyFlag = false;
		
		String imgUrl = prepareAjaxImage(tiles, false);
		StringBuffer sbTileSequence = new StringBuffer();
		int i = 0;
		for(TileInfo tile : tiles) {
			sbTileSequence.append("[").append(tile.getRow()).append(",").append(tile.getCol()).append("]");
			if(i < tiles.size() - 1)
				sbTileSequence.append(",");
			
			i++;
		}

/*		
		SimpleHash model = new SimpleHash();
		model.put("tileSequence", sbTileSequence.toString());
		model.put("resized", doResize);
		model.put("imgUrl", imgUrl);
		model.put("width", String.valueOf(resizedFramebufferWidth));
		model.put("height", String.valueOf(resizedFramebufferHeight));
		model.put("tileWidth", String.valueOf(tracker.getTileWidth()));
		model.put("tileHeight", String.valueOf(tracker.getTileHeight()));
		
		StringWriter writer = new StringWriter();
		try {
			ConsoleProxy.processTemplate("viewer-update.ftl", model, writer);
		} catch (IOException e) {
			s_logger.warn("Unexpected exception in processing template.", e);
		} catch (TemplateException e) {
			s_logger.warn("Unexpected exception in processing template.", e);
		}
		StringBuffer sb = writer.getBuffer();
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("onAjaxClientUpdate response: " + sb.toString());
		
		return sb.toString();
*/
		return getAjaxViewerUpdatePageContent(sbTileSequence.toString(), imgUrl, doResize, resizedFramebufferWidth,
			resizedFramebufferHeight, tracker.getTileWidth(), tracker.getTileHeight());
	}
	
	private String getAjaxViewerUpdatePageContent(String tileSequence, String imgUrl, boolean resized, int width,
		int height, int tileWidth, int tileHeight) {
		
		String[] content = new String[] {
			"tileMap = [ " + tileSequence + " ];",
			resized ? "ajaxViewer.resize('main_panel', " + width + ", " + height + " , " + tileWidth + ", " + tileHeight + ");" : "", 
			"ajaxViewer.refresh('" + imgUrl + "', tileMap, false);"
		};
		
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < content.length; i++)
			sb.append(content[i]);
		
		return sb.toString();
	}
	
	
	public long getAjaxSessionId() {
		return this.ajaxSessionId;
	}
	
	public AjaxFIFOImageCache getAjaxImageCache() {
		return ajaxImageCache;
	}
	
	public boolean isAjaxViewer() {
		return ajaxViewer;
	}

	public synchronized void setAjaxViewer(boolean ajaxViewer) {
		if(this.ajaxViewer != ajaxViewer) {
			if(this.ajaxViewer) {
				// previous session was AJAX session
				this.ajaxSessionId++;		// increase the session id so that it will disconnect existing AJAX viewer
			} else {
				// close java client session
				if(clientStream != null) {
					byte[] bs = new byte[2];
					bs[0] = (byte)250;
					bs[1] = 1;
					writeToClientStream(bs);
					
					try {
						clientStream.close();
					} catch (IOException e) {
					}
					clientStream = null;
				}
			}
			this.ajaxViewer = ajaxViewer;
		}
	}
	
	public void writeServer(byte[] b, int off, int len) {
		synchronized (this) {
			if (!rfb.closed()) {
				try {
					// We lock the viewer to avoid race condition when connecting one 
					// client forces the current client to disconnect.
					rfb.os.write(b, off, len);
					rfb.os.flush();
				} catch (IOException e) {
					// Swallow the exception because we want the client connection to sustain
					// even when server connection is severed and reestablished.
					s_logger.info("Ignore exception when writing to server: " + e);
					rfb.close();
				}
			} else {
				s_logger.info("Dropping client event because server connection is closed ");
			}
		}
	}
	
	public void sendClientMouseEvent(int event, int x, int y, int code, int modifiers) {
		if(code == 2)
			modifiers |= MouseEvent.BUTTON3_MASK;
		else
			modifiers |= MouseEvent.BUTTON1_MASK;

		int id = 0; 
		if(event == 1)
			id = MouseEvent.MOUSE_MOVED;
		else if(event == 2)
			id = MouseEvent.MOUSE_PRESSED;
		else if(event == 3)
			id = MouseEvent.MOUSE_RELEASED;
		else if(event == 8)
			id = MouseEvent.MOUSE_PRESSED;

		long curTicks = System.currentTimeMillis();
		MouseEvent mouseEvent = new MouseEvent(vc, id,
			curTicks, modifiers, x, y, 1, false);
		
		synchronized (this) {
			if (rfb != null && !rfb.closed()) {
				try {
					rfb.writePointerEvent(mouseEvent);
					if(event == 8) {
						if(s_logger.isTraceEnabled())
							s_logger.trace("Replay mouse double click event at " + x + "," + y);

						mouseEvent = new MouseEvent(vc, MouseEvent.MOUSE_RELEASED,
								curTicks, modifiers, x, y, 1, false);
						rfb.writePointerEvent(mouseEvent);
						
						mouseEvent = new MouseEvent(vc, MouseEvent.MOUSE_PRESSED,
								curTicks, modifiers, x, y, 1, false);
						rfb.writePointerEvent(mouseEvent);
						
						mouseEvent = new MouseEvent(vc, MouseEvent.MOUSE_RELEASED,
								curTicks, modifiers, x, y, 1, false);
						rfb.writePointerEvent(mouseEvent);
					}
				} catch (IOException e) {
					s_logger.warn("Exception while sending mouse event. ", e);
				}
			}
		}
	}

	public void sendClientRawKeyboardEvent(int event, int code, int modifiers) {
		switch(event) {
		case 4 : 	// Key press
			break;

		case 5 :	// Key down
		    writeRawKeyboardEvent(code, true);
			break;
			
		case 6 :	// Key Up
            writeRawKeyboardEvent(code, false);
			break;
		}
	}
	
	private void writeRawKeyboardEvent(int keysym, boolean down) {
        synchronized (this) {
            if (rfb != null && !rfb.closed()) {
                try {
                    rfb.writeKeyEvent(keysym, down);
                    rfb.flushEventBuffer();
                } catch (IOException e) {
                    s_logger.warn("Exception while sending keyboard event. ", e);
                }
            }
        }
	}
}
