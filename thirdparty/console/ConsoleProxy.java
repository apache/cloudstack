//
//  Copyright (C) 2008 VMOps, Inc.  All Rights Reserved.
//

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.*;

public class ConsoleProxy {

	static Hashtable<String, ConsoleViewer> connectionMap = new Hashtable<String, ConsoleViewer>();
	static int tcpListenPort = 5999;
	static int httpListenPort = 8000;
	static int httpCmdListenPort = 8001;
	static String jarDir = "../applet/";
	static boolean compressServerMessage = true;
	static int viewerLinger = 180;
	
	public static void main(String[] argv) {
		System.setProperty("java.awt.headless", "true");

		InputStream confs = ConsoleProxy.class.getResourceAsStream("vmops.properties");
		
		if (confs == null) {
			Logger.log(Logger.INFO, "Can't load vmops.properties from classpath, will use default configuration");
		} else {
			Properties conf = new Properties();
			try {
				conf.load(confs);
				String s = conf.getProperty("tcpListenPort");
				if (s!=null) {
					tcpListenPort = Integer.parseInt(s);
					Logger.log(Logger.INFO, "Setting tcpListenPort=" + s);
				}
				s = conf.getProperty("httpListenPort");
				if (s!=null) {
					httpListenPort = Integer.parseInt(s);
					Logger.log(Logger.INFO, "Setting httpListenPort=" + s);
				}
				s = conf.getProperty("httpCmdListenPort");
				if (s!=null) {
					httpCmdListenPort = Integer.parseInt(s);
					Logger.log(Logger.INFO, "Setting httpCmdListenPort=" + s);
				}
				s = conf.getProperty("jarDir");
				if (s!=null) {
					jarDir = s;
					Logger.log(Logger.INFO, "Setting jarDir=" + s);
				}
				s = conf.getProperty("viewerLinger");
				if (s!=null) {
					viewerLinger = Integer.parseInt(s);
					Logger.log(Logger.INFO, "Setting viewerLinger=" + s);
				}
				s = conf.getProperty("compressServerMessage");
				if (s!=null) {
					compressServerMessage = Boolean.parseBoolean(s);
					Logger.log(Logger.INFO, "Setting compressServerMessage=" + s);
				}

			} catch (Exception e) {
				Logger.log(Logger.ERROR, e.toString(), e);
				System.exit(1);
			}
		}

		SSLContext sslContext = null;
		
		try {
			char[] passphrase = "vmops.com".toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("../certs/realhostip.keystore"), passphrase);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, passphrase);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			   
			HttpsServer server = HttpsServer.create(new InetSocketAddress(httpListenPort), 2);
		    Logger.log(Logger.INFO, "Listening for HTTP on port " + httpListenPort);
			server.createContext("/getscreen", new ScreenHandler());
			server.createContext("/getjar/", new JARHandler());
			server.setExecutor(new ThreadExecutor()); // creates a default executor
		    server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
		        public void configure (HttpsParameters params) {

		        // get the remote address if needed
		        InetSocketAddress remote = params.getClientAddress();

		        SSLContext c = getSSLContext();

		        // get the default parameters
		        SSLParameters sslparams = c.getDefaultSSLParameters();

		        params.setSSLParameters(sslparams);
		        // statement above could throw IAE if any params invalid.
		        // eg. if app has a UI and parameters supplied by a user.

		        }
		    });
			server.start();
			
			HttpServer cmdServer = HttpServer.create(new InetSocketAddress(httpCmdListenPort), 2);
		    Logger.log(Logger.INFO, "Listening for HTTP CMDs on port " + httpCmdListenPort);
			cmdServer.createContext("/cmd", new CmdHandler());
			cmdServer.setExecutor(new ThreadExecutor()); // creates a default executor
			cmdServer.start();
			
		} catch (Exception ioe) {
			Logger.log(Logger.ERROR, ioe.toString(), ioe);
			System.exit(1);
		}
		
		ViewerGCThread cthread = new ViewerGCThread(connectionMap);
		cthread.setName("Viewer GC Thread");
		cthread.start();
		
		SSLServerSocket srvSock = null;
		try {
	         SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
	         srvSock = (SSLServerSocket) ssf.createServerSocket(tcpListenPort);
		    Logger.log(Logger.INFO, "Listening for TCP on port " + tcpListenPort);
		} catch (IOException ioe) {
			Logger.log(Logger.ERROR, ioe.toString(), ioe);
			System.exit(1);
		}

		while (true) {
		    Socket conn = null;
		    try {
		        conn = srvSock.accept();
		        String srcinfo = conn.getInetAddress().getHostAddress() + ":" + conn.getPort();
		        Logger.log(Logger.INFO, "Accepted connection from " + srcinfo);
		        conn.setSoLinger(false,0);
		        WorkerThread worker = new WorkerThread(conn);
		        worker.setName("Proxy Thread " + worker.getId() + " <" + srcinfo);
		        worker.start();
		    } catch (IOException ioe2) {
				Logger.log(Logger.ERROR, ioe2.toString(), ioe2);
				try {
				    if (conn != null) {
				        conn.close();
				    }
				} catch (IOException ioe) {}
		    } catch (Throwable e) {
		    	// Something really bad happened
		    	// Terminate the program
		    	Logger.log(Logger.ERROR, e.toString(), e);
		    	System.exit(1);
		    }
		}
	}
	

	static ConsoleViewer createViewer() {
		ConsoleViewer viewer = new ConsoleViewer();
		viewer.inAnApplet = false;
		viewer.inSeparateFrame = true;
		viewer.inProxyMode = true;
		viewer.compressServerMessage = compressServerMessage;
		return viewer;
	}
	
	static void initViewer(ConsoleViewer viewer, String host, int port, String sid) {			
		viewer.host = host;
		viewer.port = port;
		viewer.passwordParam = sid;
		
		viewer.init();
	}
	
	static ConsoleViewer getVncViewer(String host, int port, String sid) throws Exception {
		synchronized (connectionMap) {
			ConsoleViewer viewer = connectionMap.get(host + ":" + port);
//			Logger.log(Logger.INFO, "view lookup " + host + ":" + port + " = " + viewer);

			if (viewer == null) {
				viewer = createViewer();
				initViewer(viewer, host, port, sid);
				connectionMap.put(host + ":" + port, viewer);
				Logger.log(Logger.INFO, "Added viewer object " + viewer);
			} else if (!viewer.rfbThread.isAlive()) {
				Logger.log(Logger.INFO, "The rfb thread died, reinitializing the viewer " + 
						viewer);
				initViewer(viewer, host, port, sid);
			} else if (!sid.equals(viewer.passwordParam)) {
				throw new AuthenticationException ("Cannot use the existing viewer " + 
						viewer + ": bad sid");
			}
			if (viewer.status == ConsoleViewer.STATUS_NORMAL_OPERATION) {
				// Do not update lastUsedTime if the viewer is in the process of starting up
				// or if it failed to authenticate.
				viewer.lastUsedTime = System.currentTimeMillis();
			}
			return viewer;
		}
	}
	
	static void waitForViewerToStart(ConsoleViewer viewer) throws Exception {
		if (viewer.status == ConsoleViewer.STATUS_NORMAL_OPERATION) {
			return;
		}

		Long startTime = System.currentTimeMillis();
		int delay = 500;
		
		while (System.currentTimeMillis() < startTime + 30000 &&
				viewer.status != ConsoleViewer.STATUS_NORMAL_OPERATION) {
			if (viewer.status == ConsoleViewer.STATUS_AUTHENTICATION_FAILURE) {
				throw new Exception ("Authentication failure");
			}
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// ignore
			}
			delay = (int)((float)delay * 1.5);
		}
		
		if (viewer.status != ConsoleViewer.STATUS_NORMAL_OPERATION) {
			throw new Exception ("Cannot start VncViewer");
		}
		
		Logger.log(Logger.INFO, "Waited " + 
				(System.currentTimeMillis() - startTime) + "ms for VncViewer to start");

	}

	static class ThreadExecutor implements Executor {
	     public void execute(Runnable r) {
	         new Thread(r).start();
	     }
	 }
	
	static class CmdHandler implements HttpHandler {
		
		public void handle(HttpExchange t) throws IOException {
			try {
		        Thread.currentThread().setName("Cmd Thread " + 
		        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
				Logger.log(Logger.DEBUG, "CmdHandler " + t.getRequestURI());
				doHandle(t);
			} catch (Exception e) {
				Logger.log(Logger.ERROR, e.toString(), e);
				String response = "Not found";
				t.sendResponseHeaders(404, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (Throwable e) {
				Logger.log(Logger.ERROR, e.toString(), e);
			} finally {
				t.close();
			}
		}
		
		public void doHandle(HttpExchange t) throws Exception {
			String path = t.getRequestURI().getPath();
			int i = path.indexOf("/", 1);
			String cmd = path.substring(i + 1);
			Logger.log(Logger.INFO, "Get CMD request for " + cmd);
			if (cmd.equals("getstatus")) {
				ConsoleProxyStatus status = new ConsoleProxyStatus();
				status.setConnections(connectionMap);
				Headers hds = t.getResponseHeaders();
				hds.set("Content-Type", "text/plain");
				t.sendResponseHeaders(200, 0);
				OutputStreamWriter os = new OutputStreamWriter(t.getResponseBody());
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				gson.toJson(status, os);
				os.close();
			}
		}
	}

	static class JARHandler implements HttpHandler {
		
		public void handle(HttpExchange t) throws IOException {
			try {
		        Thread.currentThread().setName("JAR Thread " + 
		        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
				Logger.log(Logger.DEBUG, "JARHandler " + t.getRequestURI());
				doHandle(t);
			} catch (Exception e) {
				Logger.log(Logger.ERROR, e.toString(), e);
				String response = "Not found";
				t.sendResponseHeaders(404, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (Throwable e) {
				Logger.log(Logger.ERROR, e.toString(), e);
			} finally {
				t.close();
			}
		}
		
		public void doHandle(HttpExchange t) throws Exception {
			String path = t.getRequestURI().getPath();

			Logger.log(Logger.INFO, "Get JAR request for " + path);
			int i = path.indexOf("/", 1);
			String filepath = path.substring(i + 1);
			i = path.lastIndexOf(".");
			String extension = (i == -1) ? "" : path.substring(i + 1);
			if (!extension.equals("jar")) {
				throw new IllegalArgumentException();
			}
			File f = new File (jarDir + filepath);
			long lastModified = f.lastModified();
			String ifModifiedSince = t.getRequestHeaders().getFirst("If-Modified-Since");
			if (ifModifiedSince != null) {
				long d = Date.parse(ifModifiedSince);
//				Logger.log(Logger.INFO, "ifModified=" + d + " lastModified =" + lastModified);
				// Give it 1 second grace period to account for errors introduced by
				// date parsing and printing
				if (d + 1000 >= lastModified) {
					Headers hds = t.getResponseHeaders();
					hds.set("Content-Type", "application/java-archive");
					t.sendResponseHeaders(304, -1);
					Logger.log(Logger.INFO, "Sent 304 JAR file has not been " +
							"modified since " + ifModifiedSince);
//					Logger.log(Logger.INFO, "Req=" + t.getRequestHeaders().entrySet());
//					Logger.log(Logger.INFO, "Resp=" + hds.entrySet());
					return;
				}
			}
			long length = f.length();
			FileInputStream fis = new FileInputStream(f);
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", "application/java-archive");
			hds.set("Last-Modified", new Date(lastModified).toGMTString());
//			Logger.log(Logger.INFO, "Req=" + t.getRequestHeaders().entrySet());
//			Logger.log(Logger.INFO, "Resp=" + hds.entrySet());
			
			t.sendResponseHeaders(200, length);
			OutputStream os = t.getResponseBody();
			while (true) {
				byte[] b = new byte[8192];
				int n = fis.read(b);
				if (n < 0) {
					break;
				}
				os.write(b, 0, n);
			}
			os.close();
			fis.close();
			Logger.log(Logger.INFO, "Sent JAR file " + path);
		}
	}

	static class ScreenHandler implements HttpHandler {
		
		public ScreenHandler() {
		}

		public void handle(HttpExchange t) throws IOException {
			try {
		        Thread.currentThread().setName("JPG Thread " + 
		        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
				Logger.log(Logger.DEBUG, "ScreenHandler " + t.getRequestURI());
				doHandle(t);
			} catch (IllegalArgumentException e) {
				String response = "Bad query string";
				Logger.log(Logger.ERROR, response);
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (Throwable e) {
				Logger.log(Logger.ERROR, e.toString());
				// Send back a dummy image
				File f = new File ("./cannotconnect.jpg");
				long length = f.length();
				FileInputStream fis = new FileInputStream(f);
				Headers hds = t.getResponseHeaders();
				hds.set("Content-Type", "image/jpeg");
				hds.set("Cache-Control", "no-cache");
				hds.set("Cache-Control", "no-store");				
				t.sendResponseHeaders(200, length);
				OutputStream os = t.getResponseBody();
				try {
					while (true) {
						byte[] b = new byte[8192];
						int n = fis.read(b);
						if (n < 0) {
							break;
						}
						os.write(b, 0, n);
					}
				} finally {
					os.close();
					fis.close();
				}
				Logger.log(Logger.ERROR, "Cannot get console, sent error JPG response");
				return;
			} finally {
				t.close();
			}
		}
		
		void doHandle(HttpExchange t) throws Exception,
				IllegalArgumentException {
			String queries = t.getRequestURI().getQuery();
			Map<String, String> queryMap = getQueryMap(queries);
			int width = 0;
			int height = 0;
			int port = 0;
			String ws = queryMap.get("w");
			String hs = queryMap.get("h");
			String host = queryMap.get("host");
			String portStr = queryMap.get("port");
			String sid = queryMap.get("sid");
			if (ws == null || hs == null || host == null || portStr == null || sid == null ) {
				throw new IllegalArgumentException();
			}
			try {
				width = Integer.parseInt(ws);
				height = Integer.parseInt(hs);
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}

			ConsoleViewer viewer = getVncViewer(host, port, sid);
			
			if (viewer.status != ConsoleViewer.STATUS_NORMAL_OPERATION) {
				// Send back a dummy image
				File f = new File ("./notready.jpg");
				long length = f.length();
				FileInputStream fis = new FileInputStream(f);
				Headers hds = t.getResponseHeaders();
				hds.set("Content-Type", "image/jpeg");
				hds.set("Cache-Control", "no-cache");
				hds.set("Cache-Control", "no-store");				
				t.sendResponseHeaders(200, length);
				OutputStream os = t.getResponseBody();
				try {
					while (true) {
						byte[] b = new byte[8192];
						int n = fis.read(b);
						if (n < 0) {
							break;
						}
						os.write(b, 0, n);
					}
				} finally {
					os.close();
					fis.close();
				}
				Logger.log(Logger.INFO, "Console not ready, sent dummy JPG response");
				return;
			}
/*			
			if (viewer.status == ConsoleViewer.STATUS_AUTHENTICATION_FAILURE) {
				String response = "Authentication failed";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} else if (viewer.vc == null || viewer.vc.memImage == null) {
				String response = "Server not ready";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} else 
*/
			{
				Image scaledImage = viewer.vc.memImage.getScaledInstance(width,
						height, Image.SCALE_DEFAULT);
				BufferedImage bufferedImage = new BufferedImage(width, height,
						BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D bufImageGraphics = bufferedImage.createGraphics();
				bufImageGraphics.drawImage(scaledImage, 0, 0, null);
				ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
				javax.imageio.ImageIO.write(bufferedImage, "jpg", bos);
				byte[] bs = bos.toByteArray();
				Headers hds = t.getResponseHeaders();
				hds.set("Content-Type", "image/jpeg");
				hds.set("Cache-Control", "no-cache");
				hds.set("Cache-Control", "no-store");
				t.sendResponseHeaders(200, bs.length);
				OutputStream os = t.getResponseBody();
				os.write(bs);
				os.close();
			}
		}

		public static Map<String, String> getQueryMap(String query) {
			String[] params = query.split("&");
			Map<String, String> map = new HashMap<String, String>();
			for (String param : params) {
				String name = param.split("=")[0];
				String value = param.split("=")[1];
				map.put(name, value);
			}
			return map;
		}
	}

    static private class WorkerThread extends Thread {
		Socket clientSocket = null;
		DataInputStream clientIns = null;
		OutputStream clientOuts = null;

		public WorkerThread(Socket client) {
			clientSocket = client;
		}

		synchronized void cleanup() {
			try {
				if (clientSocket != null) {
					Logger.log(Logger.INFO, "Closing connection to "
							+ clientSocket.getInetAddress());
					clientSocket.close();
					clientSocket = null;
				}
			} catch (IOException ioe) {
			}
			try {
				if (clientIns != null) {
					clientIns.close();
					clientIns = null;
				}
			} catch (IOException ioe) {
			}
			try {
				if (clientOuts != null) {
					clientOuts.close();
					clientOuts = null;
				}
			} catch (IOException ioe) {
			}
		}

		public void run() {
			try {
				String srcinfo = clientSocket.getInetAddress().getHostAddress() +
					":" + clientSocket.getPort();
				clientIns = new DataInputStream(new BufferedInputStream(
						clientSocket.getInputStream()));
//				clientOuts = new GZIPOutputStream(clientSocket.getOutputStream(), 65536);
				clientOuts = clientSocket.getOutputStream();
				clientOuts.write("RFB 000.000\000".getBytes("US-ASCII"));
				clientOuts.flush();
				int b1 = clientIns.read();
				int b2 = clientIns.read();
				if (b1 != 'V' || b2 != 'M') {
					throw new Exception ("Bad header");
				}
				byte[] proxyInfo = new byte[clientIns.read()];
				clientIns.readFully(proxyInfo);
				String proxyString = new String(proxyInfo, "US-ASCII");
				StringTokenizer stk = new StringTokenizer(proxyString, ":\n");
				String host = stk.nextToken();
				int port = Integer.parseInt(stk.nextToken());
				String sid = stk.nextToken();
				ConsoleViewer viewer = getVncViewer(host, port, sid);
				
				waitForViewerToStart(viewer);
				
				handleClientSession(viewer, srcinfo);
			} catch (Exception ioe) {
				Logger.log(Logger.INFO, "Exception encountered when establishing client session: " + ioe);
				Logger.log(Logger.DEBUG, ioe.toString(), ioe);
			} finally {
				cleanup();
			}
		}
		
		void writeServer (ConsoleViewer viewer, byte[] b, int off, int len) {
			synchronized (viewer) {
				if (!viewer.rfb.closed()) {
					try {
						// We lock the viewer to avoid race condition when connecting one 
						// client forces the current client to disconnect.
						viewer.rfb.os.write(b, off, len);
						viewer.rfb.os.flush();
					} catch (IOException e) {
						// Swallow the exception because we want the client connection to sustain
						// even when server connection is severed and reestablished.
						Logger.log(Logger.INFO, "Ignore exception when writing to server " + e);
						Logger.log(Logger.DEBUG, e.toString(), e);
						viewer.rfb.close();
					}
				} else {
					Logger.log(Logger.INFO, "Dropping client event because server connection is closed ");
				}
			}
		}
		
		void writeClientU16 (int n) throws IOException {
			byte[] b = new byte[2];
		    b[0] = (byte) ((n >> 8) & 0xff);
		    b[1] = (byte) (n & 0xff);
		    clientOuts.write(b);
		}
		
		void writeClientU32 (int n) throws IOException {
			byte[] b = new byte[4];
		    b[0] = (byte) ((n >> 24) & 0xff);
		    b[1] = (byte) ((n >> 16) & 0xff);
		    b[2] = (byte) ((n >> 8) & 0xff);
		    b[3] = (byte) (n & 0xff);
		    clientOuts.write(b);
		}
		
		String RFB_VERSION_STRING = "RFB 003.008\n";
		
		void handleClientSession(ConsoleViewer viewer, String srcinfo) throws Exception {
		    Logger.log(Logger.INFO, "Start to handle client session");
			// Exchange version with client
			clientOuts.write(RFB_VERSION_STRING.getBytes("US-ASCII"));
			clientOuts.flush();
			byte[] clientVersion = new byte[12];
			clientIns.readFully(clientVersion);
			if (!RFB_VERSION_STRING.equals(new String(clientVersion, "US-ASCII"))) {
				throw new Exception("Bad client version");
			}
			// Send security type -- no authentication needed
			byte[] serverSecurity = new byte[2];
			serverSecurity[0] = 1;
			serverSecurity[1] = 1;
			clientOuts.write(serverSecurity);
			clientOuts.flush();
			int clientSecurity = clientIns.read();
			if (clientSecurity != 1) {
				throw new Exception("Unsupported client security type " + clientSecurity);
			}
			byte[] serverSecResp = new byte[4];
			serverSecResp[0] = serverSecResp[1] = serverSecResp[2] = serverSecResp[3] = 0;
			clientOuts.write(serverSecResp);
			clientOuts.flush();
	
			// Receive and ignore client init
			clientIns.read();
		    
		    Logger.log(Logger.INFO, "Sending ServerInit w=" + viewer.rfb.framebufferWidth + 
		    		" h=" + viewer.rfb.framebufferHeight + 
		    		" bits=" + viewer.rfb.bitsPerPixel +
		    		" depth=" + viewer.rfb.depth +
		    		" name=" + viewer.rfb.desktopName);
			// Send serverInit
		    writeClientU16(viewer.rfb.framebufferWidth);
		    writeClientU16(viewer.rfb.framebufferHeight);
		    clientOuts.write(viewer.rfb.bitsPerPixel);
		    clientOuts.write(viewer.rfb.depth);
		    clientOuts.write(viewer.rfb.bigEndian ? 1 : 0);
		    clientOuts.write(viewer.rfb.trueColour ? 1 : 0);
		    writeClientU16(viewer.rfb.redMax);
		    writeClientU16(viewer.rfb.greenMax);
		    writeClientU16(viewer.rfb.blueMax);
		    clientOuts.write(viewer.rfb.redShift);
		    clientOuts.write(viewer.rfb.greenShift);
		    clientOuts.write(viewer.rfb.blueShift);
		    byte[] pad = new byte[3];
		    clientOuts.write(pad);
		    writeClientU32(viewer.rfb.desktopName.length());
		    clientOuts.write(viewer.rfb.desktopName.getBytes("US-ASCII"));
			clientOuts.flush();
			
		    // Lock the viewer to avoid race condition
		    synchronized (viewer) {
		    	if (viewer.clientStream != null) {
		    		Logger.log(Logger.INFO, "Disconnecting client link stream " + 
		    				viewer.clientStream.hashCode() + " from " + 
		    				viewer.clientStreamInfo);
		    		viewer.clientStream.close();
		    	}
		    	viewer.clientStream = clientOuts;
		    	viewer.clientStreamInfo = srcinfo;
		    	viewer.lastUsedTime = System.currentTimeMillis();

	    		Logger.log(Logger.INFO, "Setting client link stream " + 
	    				viewer.clientStream.hashCode() + " from " + srcinfo);
		    }

			try { 
				while (true) {
					byte[] b = new byte[512];
					int nbytes = 0;
					int msgType = clientIns.read();
					b[0] = (byte)msgType;
					switch (msgType) {
					case RfbProto.SetPixelFormat:
						clientIns.readFully(b, 1, 19);
						nbytes = 20;
						break;
					case RfbProto.SetEncodings:
						clientIns.read(); // padding
						b[1] = 0;
						int n = clientIns.readUnsignedShort();
						if (n > (512 - 4)/4) {
							throw new Exception ("Too many client encodings");
						}
						b[2] = (byte) ((n >> 8) & 0xff);
						b[3] = (byte) (n & 0xff);
						clientIns.readFully(b, 4, n * 4);
						nbytes = n * 4 + 4;
						break;
					case RfbProto.FramebufferUpdateRequest:
						clientIns.readFully(b, 1, 9);
						if (true) {
							int i = b[1];
							int x = ((0xff & b[2]) << 8) + b[3];
							int y = ((0xff & b[4]) << 8) + b[5];
							int w = ((0xff & b[6]) << 8) + b[7];
							int h = ((0xff & b[8]) << 8) + b[9];
							Logger.log(Logger.DEBUG, "Client FramebudderUpdateRequest inc="
									+ i + " x=" + x + " y=" + y + " w=" + w + " h=" + h);
						}
						nbytes = 10;
						break;
					case RfbProto.KeyboardEvent:
						clientIns.readFully(b, 1, 7);
						nbytes = 8;
						break;
					case RfbProto.PointerEvent:
						clientIns.readFully(b, 1, 5);
						nbytes = 6;
						break;
					case RfbProto.VMOpsClientCustom:
						clientIns.read(); // read and ignore, used to track liveliness
											// of the client
						break;
					default:
						throw new Exception("Bad client event type: " + msgType);
					}
				    Logger.log(Logger.DEBUG, "C->S type=" + msgType + " size=" + nbytes);
					writeServer(viewer, b, 0, nbytes);
				}
			} finally {
				viewer.lastUsedTime = System.currentTimeMillis();
			}
			
		}
	}
    static class ViewerGCThread extends Thread {
    	Hashtable<String, ConsoleViewer> connMap;
    	public ViewerGCThread(Hashtable<String, ConsoleViewer> connMap) {
    		this.connMap = connMap;
    	}
    	
    	public void run() {
    		while (true) {
    			Logger.log(Logger.INFO, "connMap=" + connMap);
    			Enumeration<String> e = connMap.keys();
    		    while (e.hasMoreElements()) {
    		    	String key;
    		    	ConsoleViewer viewer;
    		    	
    		    	synchronized (connMap) {
	    		        key  = e.nextElement();
	    		        viewer  = connMap.get(key);
    		    	}
	    		         
	    		    long seconds_unused = 
	    		        (System.currentTimeMillis() - viewer.lastUsedTime) / 1000;
	    		         
	    		    if (seconds_unused > viewerLinger / 2 && viewer.clientStream != null) {
	    		    	Logger.log(Logger.INFO, "Pinging client for " + viewer + 
	    		    			" which has not been used for " + seconds_unused + "sec");
	    		    	byte[] bs = new byte[2];
	    		        bs[0] = (byte)250;
	    		        bs[1] = 3;
	    		        viewer.vc.writeToClientStream(bs);
	    		    }
	    		    if (seconds_unused < viewerLinger) {
	    		      	continue;
	    		    }
    		    	synchronized (connMap) {
    		    		connMap.remove(key);
    		    	}
	    		    // close the server connection
	    		    Logger.log(Logger.INFO, "Dropping " + viewer + 
	    		        		 " which has not been used for " + 
	    		        		 seconds_unused + " seconds");
	    		    viewer.dropMe = true;
	    		    synchronized (viewer) {
		    		    if (viewer.clientStream != null) {
		    		    	try {
		    		    		viewer.clientStream.close();
		    		    	} catch (IOException ioe) {
		    		    		// ignored
		    		    	}
				    		viewer.clientStream = null;
				    		viewer.clientStreamInfo = null;
		    		    }
	    		        if (viewer.rfb != null) {
	    		        	viewer.rfb.close();
	    		        }
    		    	}
    		    }
    			try {
    				Thread.sleep(30000);
    			} catch (InterruptedException exp) {
    				// Ignore
    			}
    		}
    	}
    }
}
