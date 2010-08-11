//
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2002 Constantin Kaplinsky.  All Rights Reserved.
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

//
// VncViewer.java - the VNC viewer applet.  This class mainly just sets up the
// user interface, leaving it to the VncCanvas to do the actual rendering of
// a VNC desktop.
//

package com.cloud.consoleviewer;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.net.*;
import java.util.Date;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.cloud.console.AuthenticationException;
import com.cloud.console.ConsoleCanvas;
import com.cloud.console.ConsoleCanvas2;
import com.cloud.console.Logger;
import com.cloud.console.RfbProto;
import com.cloud.console.RfbProtoAdapter;
import com.cloud.console.RfbViewer;

@SuppressWarnings("deprecation")
public class ConsoleViewer implements java.lang.Runnable, RfbViewer, RfbProtoAdapter {
	private static final Logger s_logger = Logger.getLogger(ConsoleViewer.class);

	int id = getNextId();
	boolean compressServerMessage = false;
	long createTime = System.currentTimeMillis();
	long lastUsedTime = System.currentTimeMillis();

	boolean dropMe = false;

	OutputStream clientStream;
	String clientStreamInfo;

	int status;
	public final static int STATUS_ERROR = -1;
	public final static int STATUS_UNINITIALIZED = 0;
	public final static int STATUS_CONNECTING = 1;
	public final static int STATUS_INITIALIZING = 2;
	public final static int STATUS_NORMAL_OPERATION = 3;
	public final static int STATUS_AUTHENTICATION_FAILURE = 100;
	
	public final static int DEFAULT_READ_TIMEOUT_SECONDS = 45;

	boolean inAnApplet = true;
	boolean inSeparateFrame = false;
	boolean inProxyMode = false;

	String[] mainArgs;

	RfbProto rfb;
	Thread rfbThread;

	ConsoleApplet applet;
	Frame vncFrame;
	Container vncContainer;
	ScrollPane desktopScrollPane;
	GridBagLayout gridbag;
	ButtonPanel buttonPanel;
	Label connStatusLabel;
	ConsoleCanvas vc;
	OptionsFrame options;
	ClipboardFrame clipboard;
	private RecordingFrame rec;

	// Control session recording.
	Object recordingSync;
	String sessionFileName;
	boolean recordingActive;
	boolean recordingStatusChanged;
	String cursorUpdatesDef;
	String eightBitColorsDef;

	// Variables read from parameter values.
	String socketFactory;
	String host;
	int port;
	String proxyHost;
	int proxyPort;
	String passwordParam;
	boolean showControls;
	boolean offerRelogin;
	boolean showOfflineDesktop;
	int deferScreenUpdates;
	int deferCursorUpdates;
	int deferUpdateRequests;

	static int id_count = 1;
	synchronized static int getNextId() {
		return id_count++;
	}
	
	//
	// init()
	//

	public void init() {
		/*
		if (inProxyMode) {
			initProxy();
			return;
		}
		*/
		readParameters();

		if (inSeparateFrame) {
			vncFrame = new Frame("VMOps ConsoleViewer");
			if (!inAnApplet) {
				// vncFrame.add("Center", this);
			}
			vncContainer = vncFrame;
		} else {
			vncContainer = applet;
		}

		recordingSync = new Object();

		options = new OptionsFrame(this);
		// clipboard = new ClipboardFrame(this);
		// if (RecordingFrame.checkSecurity())
		// rec = new RecordingFrame(this);

		sessionFileName = null;
		recordingActive = false;
		recordingStatusChanged = false;
		cursorUpdatesDef = null;
		eightBitColorsDef = null;

		if (inSeparateFrame)
			vncFrame.addWindowListener(applet);
		rfbThread = new Thread(this);
		rfbThread.start();
	}

	/*
	private void initProxy() {
		recordingSync = new Object();

		options = new OptionsFrame(this);
		options.viewOnly = true;

		sessionFileName = null;
		recordingActive = false;
		recordingStatusChanged = false;
		cursorUpdatesDef = null;
		eightBitColorsDef = null;

		rfbThread = new Thread(this);
		rfbThread.setName("RFB Thread " + rfbThread.getId() + " >" + host + ":"
				+ port);

		rfbThread.start();
	}
	*/

	public void update(Graphics g) {
	}

	//
	// run() - executed by the rfbThread to deal with the RFB socket.
	//

	public void run() {
		/*
		if (inProxyMode) {
			runProxy();
			return;
		}
		*/
		
		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(
		        new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor transparentCursor =
		        Toolkit.getDefaultToolkit().createCustomCursor
		             (image, new Point(0, 0), "invisibleCursor");
		vncContainer.setCursor(transparentCursor);


		gridbag = new GridBagLayout();
		vncContainer.setLayout(gridbag);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		//gbc.anchor = GridBagConstraints.CENTER;

		if (showControls) {
			buttonPanel = new ButtonPanel(this);
			gridbag.setConstraints(buttonPanel, gbc);
			vncContainer.add(buttonPanel);
		}

		// FIXME: Use auto-scaling not only in a separate frame.
		if (options.autoScale && inSeparateFrame) {
			Dimension screenSize;
			try {
				screenSize = vncContainer.getToolkit().getScreenSize();
			} catch (Exception e) {
				screenSize = new Dimension(0, 0);
			}
			createCanvas(screenSize.width - 32, screenSize.height - 32);
		} else {
			createCanvas(0, 0);
		}
		
	    if (!options.viewOnly)
	        vc.enableInput(true);

		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		if (inSeparateFrame) {

			// Create a panel which itself is resizeable and can hold
			// non-resizeable VncCanvas component at the top left corner.
			Panel canvasPanel = new Panel();
			canvasPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			canvasPanel.add(vc);

			// Create a ScrollPane which will hold a panel with VncCanvas
			// inside.
			desktopScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			gbc.fill = GridBagConstraints.BOTH;
			gridbag.setConstraints(desktopScrollPane, gbc);
			desktopScrollPane.add(canvasPanel);
			// Finally, add our ScrollPane to the Frame window.
			vncFrame.add(desktopScrollPane);
			// vncFrame.setTitle(rfb.desktopName);
			vncFrame.pack();
			vc.resizeDesktopFrame();
		} else {
			gridbag.setConstraints(vc, gbc);
			applet.add(vc);
			applet.validate();
		}

		if (showControls)
			buttonPanel.enableButtons();
		moveFocusToDesktop();

		try {
			connectAndAuthenticate();
			doProtocolInitialisation();
			vc.rfb = rfb;
			vc.setPixelFormat();
			vc.rfb.writeFramebufferUpdateRequest(0, 0, vc.rfb.framebufferWidth,
					vc.rfb.framebufferHeight, false);
			vc.processNormalProtocol();
			// We should never get here, but just in case
			showMessage("Disconnected from server");
		} catch (NoRouteToHostException e) {
			fatalError("Network error: no route to server: " + host, e);
		} catch (UnknownHostException e) {
			fatalError("Network error: server name unknown: " + host, e);
		} catch (ConnectException e) {
			fatalError("Network error: could not connect to server: " + host
					+ ":" + port, e);
		} catch (EOFException e) {
			fatalError("Network error: remote side closed connection", e);
		} catch (IOException e) {
			fatalError(e.getMessage(), e);
		} catch (Exception e) {
			fatalError(e.getMessage(), e);
		} finally {
			encodingsSaved = null;
			nEncodingsSaved = 0;
			synchronized (this) {
				if (rfb != null) {
					rfb.close();
				}
			}
		}
		
		s_logger.info("Client RFB thread terminated");
	}

	/*
	public void runProxy() {
		createCanvas(0, 0);

		int delay = 0;
		while (!dropMe) {
			try {
				status = STATUS_CONNECTING;
				connectAndAuthenticate();
				delay = 0; // reset the delay interval
				status = STATUS_INITIALIZING;
				doProtocolInitialisation();
				vc.rfb = rfb;
				vc.setPixelFormat();
				vc.rfb.writeFramebufferUpdateRequest(0, 0,
						vc.rfb.framebufferWidth, vc.rfb.framebufferHeight,
						false);
				status = STATUS_NORMAL_OPERATION;
				vc.processNormalProtocol();
			} catch (AuthenticationException e) {
				status = STATUS_AUTHENTICATION_FAILURE;
				String msg = e.getMessage();
				s_logger.info(msg);
			} catch (Exception e) {
				status = STATUS_ERROR;
				s_logger.info(e.toString());
			} finally {
				String oldName = Thread.currentThread().getName();
				encodingsSaved = null;
				nEncodingsSaved = 0;
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
				s_logger.info("Exception caught, retrying in "
						+ delay + "ms");
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					// ignored
				}
				delay = (int) ((float) (delay + 700) * 1.5);
				if (delay > 3000) {
					break;
				}
			}
		}
		s_logger.info("RFB thread terminating");
	}
	*/

	//
	// Create a VncCanvas instance.
	//

	void createCanvas(int maxWidth, int maxHeight) {
		/*
		 * // Determine if Java 2D API is available and use a special // version
		 * of VncCanvas if it is present. vc = null; try { // This throws
		 * ClassNotFoundException if there is no Java 2D API. Class cl =
		 * Class.forName("java.awt.Graphics2D"); // If we could load Graphics2D
		 * class, then we can use VncCanvas2D. cl = Class.forName("VncCanvas2");
		 * Class[] argClasses = { this.getClass(), Integer.TYPE, Integer.TYPE };
		 * java.lang.reflect.Constructor cstr = cl.getConstructor(argClasses);
		 * Object[] argObjects = { this, new Integer(maxWidth), new
		 * Integer(maxHeight) }; vc = (VncCanvas)cstr.newInstance(argObjects); }
		 * catch (Exception e) { Logger.log(Logger.INFO,
		 * "Warning: Java 2D API is not available"); }
		 * 
		 * // If we failed to create VncCanvas2D, use old VncCanvas. if (vc ==
		 * null)
		 */
		vc = new ConsoleCanvas2(this, maxWidth, maxHeight);
	}

	/*
	 * // // Process RFB socket messages. // If the rfbThread is being stopped,
	 * ignore any exceptions, // otherwise rethrow the exception so it can be
	 * handled. //
	 * 
	 * void processNormalProtocol() throws Exception { try {
	 * vc.processNormalProtocol(); } catch (Exception e) { if (rfbThread ==
	 * null) { Logger.log(Logger.INFO, "Ignoring RFB socket exceptions" +
	 * " because applet is stopping"); } else { throw e; } } }
	 */
	//
	// Connect to the RFB server and authenticate the user.
	//
	void connectAndAuthenticate() throws Exception {
		showConnectionStatus("Initializing...");
		if (!inProxyMode) {
			if (inSeparateFrame) {
				vncFrame.pack();
				vncFrame.show();
			} else {
				applet.validate();
			}
		}

		if (proxyHost != null) {
			showConnectionStatus("Connecting to " + proxyHost + ", port "
					+ proxyPort + "...");
			rfb = new RfbProto(proxyHost, proxyPort, this);
			rfb.readProxyVersion();
			rfb.writeProxyString(host, port,
					(passwordParam != null) ? passwordParam : "");
		} else {
			showConnectionStatus("Connecting to " + host + ", port " + port
					+ "...");
			rfb = new RfbProto(host, port, this);
		}
		showConnectionStatus("Connected to server");

		rfb.readVersionMsg();
		showConnectionStatus("RFB server supports protocol version "
				+ rfb.serverMajor + "." + rfb.serverMinor);

		rfb.writeVersionMsg();
		showConnectionStatus("Using RFB protocol version " + rfb.clientMajor
				+ "." + rfb.clientMinor);

		int secType = rfb.negotiateSecurity();
		int authType;
		if (secType == RfbProto.SecTypeTight) {
			showConnectionStatus("Enabling TightVNC protocol extensions");
			rfb.initCapabilities();
			rfb.setupTunneling();
			authType = rfb.negotiateAuthenticationTight();
		} else {
			authType = secType;
		}

		switch (authType) {
		case RfbProto.AuthNone:
			showConnectionStatus("No authentication needed");
			rfb.authenticateNone();
			break;
		case RfbProto.AuthVNC:
			showConnectionStatus("Performing standard VNC authentication");
			if (passwordParam != null) {
				rfb.authenticateVNC(passwordParam);
			} else {
				throw new AuthenticationException("Bad password");
				/*
				 * String pw = askPassword(); rfb.authenticateVNC(pw);
				 */
			}
			break;
		default:
			throw new Exception("Unknown authentication scheme " + authType);
		}
	}

	//
	// Show a message describing the connection status.
	// To hide the connection status label, use (msg == null).
	//

	void showConnectionStatus(String msg) {
		/*
		if (inProxyMode) {
			if (msg != null) {
				s_logger.info(msg);
			}
			return;
		}
		*/
		
		/*
		 * if (msg == null) { if (vncContainer.isAncestorOf(connStatusLabel)) {
		 * vncContainer.remove(connStatusLabel); } return; }
		 * 
		 * Logger.log(Logger.INFO, msg);
		 * 
		 * if (connStatusLabel == null) { connStatusLabel = new Label("Status: "
		 * + msg); connStatusLabel.setFont(new Font("Helvetica", Font.PLAIN,
		 * 12)); } else { connStatusLabel.setText("Status: " + msg); }
		 * 
		 * if (!vncContainer.isAncestorOf(connStatusLabel)) { GridBagConstraints
		 * gbc = new GridBagConstraints(); gbc.gridwidth =
		 * GridBagConstraints.REMAINDER; gbc.fill =
		 * GridBagConstraints.HORIZONTAL; gbc.anchor =
		 * GridBagConstraints.NORTHWEST; gbc.weightx = 1.0; gbc.weighty = 1.0;
		 * gbc.insets = new Insets(20, 30, 20, 30);
		 * gridbag.setConstraints(connStatusLabel, gbc);
		 * vncContainer.add(connStatusLabel); }
		 * 
		 * if (inSeparateFrame) { vncFrame.pack(); } else { validate(); }
		 */
	}

	//
	// Show an authentication panel.
	//
/*
	String askPassword() throws Exception {
		showConnectionStatus(null);

		AuthPanel authPanel = new AuthPanel(this);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.ipadx = 100;
		gbc.ipady = 50;
		gridbag.setConstraints(authPanel, gbc);
		vncContainer.add(authPanel);

		if (inSeparateFrame) {
			vncFrame.pack();
		} else {
			applet.validate();
		}

		authPanel.moveFocusToDefaultField();
		String pw = authPanel.getPassword();
		vncContainer.remove(authPanel);

		return pw;
	}
*/
	//
	// Do the rest of the protocol initialisation.
	//

	void doProtocolInitialisation() throws IOException {
		rfb.writeClientInit();
		rfb.readServerInit();
		/*
		 * Logger.log(Logger.INFO, "Desktop name is " + rfb.desktopName);
		 * Logger.log(Logger.INFO, "Desktop size is " + rfb.framebufferWidth +
		 * " x " + rfb.framebufferHeight);
		 */
		setEncodings();

		showConnectionStatus(null);
	}

	//
	// Send current encoding list to the RFB server.
	//

	int[] encodingsSaved;
	int nEncodingsSaved;

	void setEncodings() {
		setEncodings(false);
	}

	void autoSelectEncodings() {
		setEncodings(true);
	}

	void setEncodings(boolean autoSelectOnly) {
		if (options == null || rfb == null || !rfb.inNormalProtocol)
			return;

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

	//
	// setCutText() - send the given cut text to the RFB server.
	//

	void setCutText(String text) {
		try {
			if (rfb != null && rfb.inNormalProtocol) {
				rfb.writeClientCutText(text);
			}
		} catch (Exception e) {
			s_logger.error(e.toString(), e);
		}
	}

	//
	// Order change in session recording status. To stop recording, pass
	// null in place of the fname argument.
	//

	void setRecordingStatus(String fname) {
		synchronized (recordingSync) {
			sessionFileName = fname;
			recordingStatusChanged = true;
		}
	}

	//
	// Start or stop session recording. Returns true if this method call
	// causes recording of a new session.
	//

	boolean checkRecordingStatus() throws IOException {
		synchronized (recordingSync) {
			if (recordingStatusChanged) {
				recordingStatusChanged = false;
				if (sessionFileName != null) {
					startRecording();
					return true;
				} else {
					stopRecording();
				}
			}
		}
		return false;
	}

	//
	// Start session recording.
	//

	protected void startRecording() throws IOException {
		/*
		 * synchronized(recordingSync) {
		 * 
		 * if (!recordingActive) { // Save settings to restore them after
		 * recording the session. cursorUpdatesDef =
		 * options.choices[options.cursorUpdatesIndex].getSelectedItem();
		 * eightBitColorsDef =
		 * options.choices[options.eightBitColorsIndex].getSelectedItem(); //
		 * Set options to values suitable for recording.
		 * options.choices[options.cursorUpdatesIndex].select("Disable");
		 * options.choices[options.cursorUpdatesIndex].setEnabled(false);
		 * options.setEncodings();
		 * options.choices[options.eightBitColorsIndex].select("No");
		 * options.choices[options.eightBitColorsIndex].setEnabled(false);
		 * options.setColorFormat(); } else { rfb.closeSession(); }
		 * 
		 * Logger.log(Logger.INFO, "Recording the session in " +
		 * sessionFileName); rfb.startSession(sessionFileName); recordingActive
		 * = true; }
		 */
	}

	//
	// Stop session recording.
	//

	protected void stopRecording() throws IOException {
		/*
		 * synchronized(recordingSync) { if (recordingActive) { // Restore
		 * options.
		 * options.choices[options.cursorUpdatesIndex].select(cursorUpdatesDef);
		 * options.choices[options.cursorUpdatesIndex].setEnabled(true);
		 * options.setEncodings();
		 * options.choices[options.eightBitColorsIndex].select
		 * (eightBitColorsDef);
		 * options.choices[options.eightBitColorsIndex].setEnabled(true);
		 * options.setColorFormat();
		 * 
		 * rfb.closeSession(); Logger.log(Logger.INFO,
		 * "Session recording stopped."); } sessionFileName = null;
		 * recordingActive = false; }
		 */
	}

	//
	// readParameters() - read parameters from the html source or from the
	// command line. On the command line, the arguments are just a sequence of
	// param_name/param_value pairs where the names and values correspond to
	// those expected in the html applet tag source.
	//

	void readParameters() {
		String str;
		host = readParameter("HOST", true);

		str = readParameter("PORT", true);
		port = Integer.parseInt(str);

		proxyHost = readParameter("PROXYHOST", false);
		if (proxyHost != null) {
			str = readParameter("PROXYPORT", true);
			proxyPort = Integer.parseInt(str);
		}

		// Read "ENCPASSWORD" or "PASSWORD" parameter if specified.
		readPasswordParameters();

		if (inAnApplet) {
			str = readParameter("Open New Window", false);
			if (str != null && str.equalsIgnoreCase("Yes"))
				inSeparateFrame = true;
		}

		// "Show Controls" set to "No" disables button panel.
		showControls = false;
		str = readParameter("Show Controls", false);
		if (str != null && str.equalsIgnoreCase("No"))
			showControls = false;

		// "Offer Relogin" set to "No" disables "Login again" and "Close
		// window" buttons under error messages in applet mode.
		offerRelogin = false;
		str = readParameter("Offer Relogin", false);
		if (str != null && str.equalsIgnoreCase("No"))
			offerRelogin = false;

		// Do we continue showing desktop on remote disconnect?
		showOfflineDesktop = false;
		str = readParameter("Show Offline Desktop", false);
		if (str != null && str.equalsIgnoreCase("Yes"))
			showOfflineDesktop = true;

		// Fine tuning options.
		deferScreenUpdates = readIntParameter("Defer screen updates", 20);
		deferCursorUpdates = readIntParameter("Defer cursor updates", 10);
		deferUpdateRequests = readIntParameter("Defer update requests", 50);

		// SocketFactory.
		socketFactory = readParameter("SocketFactory", false);
	}

	//
	// Read password parameters.
	//

	private void readPasswordParameters() {
		String str = readParameter("SID", false);
		if (str != null)
			passwordParam = str;
	}

	public String readParameter(String name, boolean required) {
		if (inAnApplet) {
			String s = applet.getParameter(name);
			// s_logger.info("getParameter " + name + " = " + s);
			if ((s == null) && required) {
				fatalError(name + " parameter not specified");
			}
			return s;
		}

		for (int i = 0; i < mainArgs.length; i += 2) {
			if (mainArgs[i].equalsIgnoreCase(name)) {
				try {
					return mainArgs[i + 1];
				} catch (Exception e) {
					if (required) {
						fatalError(name + " parameter not specified");
					}
					return null;
				}
			}
		}
		if (required) {
			fatalError(name + " parameter not specified");
		}
		return null;
	}

	int readIntParameter(String name, int defaultValue) {
		String str = readParameter(name, false);
		int result = defaultValue;
		if (str != null) {
			try {
				result = Integer.parseInt(str);
			} catch (NumberFormatException e) {
			}
		}
		return result;
	}

	//
	// moveFocusToDesktop() - move keyboard focus either to VncCanvas.
	//

	void moveFocusToDesktop() {
		if (vncContainer != null) {
			if (vc != null && vncContainer.isAncestorOf(vc))
				vc.requestFocus();
		}
	}

	//
	// disconnect() - close connection to server.
	//

	synchronized public void disconnect() {
		s_logger.info("Disconnect");

		if (rfb != null)
			rfb.close();
		
		// options.dispose();
		// clipboard.dispose();
		if (rec != null)
			rec.dispose();

		if (inAnApplet) {
			showMessage("Disconnected");
		} else {
			System.exit(0);
		}
	}

	//
	// fatalError() - print out a fatal error message.
	// FIXME: Do we really need two versions of the fatalError() method?
	//

	synchronized public void fatalError(String str) {
		s_logger.info(str);

		if (inAnApplet) {
			// vncContainer null, applet not inited,
			// can not present the error to the user.
			Thread.currentThread().stop();
		} else {
			System.exit(1);
		}
	}

	synchronized public void fatalError(String str, Exception e) {
		/*
		 * if (rfb != null && rfb.closed()) { // Not necessary to show error
		 * message if the error was caused // by I/O problems after the
		 * rfb.close() method call. Logger.log(Logger.INFO,
		 * "RFB thread finished"); return; }
		 */
		if (str == null) {
			str = "";
		}
		s_logger.info(str, e);
		showMessage(str);
	}

	//
	// Show message text and optionally "Relogin" and "Close" buttons.
	//

	void showMessage(String msg) {
		if (vc != null && vc.memGraphics != null) {
			vc.paintErrorString(msg);
			return;
		} else {
			if(applet != null)
				applet.paintErrorString(msg);
		}

		s_logger.info(msg);

		/*
		 * vncContainer.removeAll();
		 * 
		 * Logger.log(Logger.INFO, "showMessage " + msg);
		 * 
		 * Label errLabel = new Label(msg, Label.CENTER); errLabel.setFont(new
		 * Font("Helvetica", Font.PLAIN, 12));
		 * 
		 * if (offerRelogin) {
		 * 
		 * Panel gridPanel = new Panel(new GridLayout(0, 1)); Panel outerPanel =
		 * new Panel(new FlowLayout(FlowLayout.LEFT));
		 * outerPanel.add(gridPanel); vncContainer.setLayout(new
		 * FlowLayout(FlowLayout.LEFT, 30, 16)); vncContainer.add(outerPanel);
		 * Panel textPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
		 * textPanel.add(errLabel); gridPanel.add(textPanel); gridPanel.add(new
		 * ReloginPanel(this));
		 * 
		 * } else {
		 * 
		 * vncContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 30));
		 * vncContainer.add(errLabel);
		 * 
		 * }
		 * 
		 * if (inSeparateFrame) { vncFrame.pack(); } else { validate(); }
		 */
	}

	//
	// Stop the applet.
	// Main applet thread will terminate on first exception
	// after seeing that rfb has been closed.
	//

	public void stop() {
		s_logger.info("Stopping applet");
		synchronized (this) {
			if (rfb != null) {
				rfb.close();
			}
		}
		
		s_logger.info("Join RFB thread");
		try {
			rfbThread.join(1000);
		} catch (InterruptedException e) {
		}
		s_logger.info("Applet stopped");
	}

	//
	// This method is called before the applet is destroyed.
	//

	public void destroy() {
		s_logger.info("Destroying applet");

		vncContainer.removeAll();
		// options.dispose();
		// clipboard.dispose();
		if (rec != null)
			rec.dispose();
		synchronized (this) {
			if (rfb != null)
				rfb.close();
		}
		if (inSeparateFrame)
			vncFrame.dispose();
	}

	//
	// Start/stop receiving mouse events.
	//

	public void enableInput(boolean enable) {
		vc.enableInput(enable);
	}

	//
	// Close application properly on window close event.
	//

	public void windowClosing(WindowEvent evt) {
		s_logger.info("Closing window");
		if (rfb != null)
			disconnect();

		vncContainer.hide();

		if (!inAnApplet) {
			System.exit(0);
		}
	}

	public String toString() {
		return ("{ConsoleViewer-"
				+ host
				+ ":"
				+ port
				+ (" created=\"" + new Date(createTime)) + "\""
				+ (" lastused=\"" + new Date(lastUsedTime)) + "\""
				+ (clientStream != null ? (" client=" + clientStreamInfo) : "") + "}");
	}

	//
	// Implement RfbViewer interface
	//
	public boolean isProxy() {
		return false;
	}
	
	public boolean hasClientConnection() {
		return false;
	}
	
	public RfbProto getRfb() {
		return rfb;
	}
	
	public Dimension getScreenSize() {
		return vncFrame.getToolkit().getScreenSize();
	}
	
	public Dimension getFrameSize() {
		return vncFrame.getSize();
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
		return deferCursorUpdates; 
	}
	
	public int getDeferScreenUpdateTimeout() {
		return deferScreenUpdates;
	}
	
	public int getDeferUpdateRequestTimeout() {
		return deferUpdateRequests;
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
	
	public void onCanvasInit() {
	    if (!options.viewOnly)
	        vc.enableInput(true);
	}
		
	public void onInputEnabled(boolean enable) {
		if(enable) {
	      if (showControls) {
	    	  buttonPanel.enableRemoteAccessControls(true);
	      }
		} else {
	      if (showControls) {
	    	  buttonPanel.enableRemoteAccessControls(false);
	      }
		}
	}
	
	public void onFramebufferSizeChange(int w, int h) {
    	// Update the size of desktop containers.
    	if (inSeparateFrame) {
    		if (desktopScrollPane != null)
    			vc.resizeDesktopFrame();
		} else {
			vc.setSize(vc.scaledWidth, vc.scaledHeight);
		}
    	moveFocusToDesktop();
    	
  	  	try {
			rfb.writeFramebufferUpdateRequest(0, 0, rfb.framebufferWidth,
				rfb.framebufferHeight, true);
		} catch (IOException e) {
			s_logger.warn("Exception when sending frame buffer update request", e);
		}
	}
	
	public void onFramebufferUpdate(int x, int y, int w, int h) {
	}
	
	public void onFramebufferCursorMove(int x, int y) {
	}
	
	public void onFramebufferCursorShapeChange(int encodingType,
	    int xhot, int yhot, int width, int height, byte[] cursorData) {
	}
	
	public void onDesktopResize() {
	    Insets insets = desktopScrollPane.getInsets();
	    desktopScrollPane.setSize(vc.scaledWidth +
					     2 * Math.min(insets.left, insets.right),
					     vc.scaledHeight +
					     2 * Math.min(insets.top, insets.bottom));

	    vncFrame.pack();
	}
	
	public void onFrameResize(Dimension newSize) {
	    vncFrame.setSize(newSize);
    	desktopScrollPane.doLayout();
	}
	
	public void onDisconnectMessage() {
		// do nothing in viewer mode
	}
	
	public void onBellMessage() {
		Toolkit.getDefaultToolkit().beep();
	}
	
	public void onPreProtocolProcess(byte[] bs) throws IOException {
		// do nothing in viwer mode
	}
	
	public boolean onPostFrameBufferUpdateProcess(boolean cursorPosReceived) throws IOException {
		boolean fullUpdateNeeded = false;
		if (checkRecordingStatus())
			fullUpdateNeeded = true;

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
		// do nothing in viewer mode
	}
	
	public Socket createConnection(String host, int port) throws IOException {
		
		SocketFactory socketFactory = SSLSocketFactory.getDefault();
        Socket sock = socketFactory.createSocket(host, port);
        sock.setSoTimeout(DEFAULT_READ_TIMEOUT_SECONDS*1000);
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
}
