/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;

import com.cloud.console.Logger;
import com.cloud.console.RfbProto;

public class ConsoleProxyClientHandler extends Thread {
/*	
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyClientHandler.class);
	
	Socket clientSocket = null;
	DataInputStream clientIns = null;
	OutputStream clientOuts = null;

	public ConsoleProxyClientHandler(Socket client) {
		clientSocket = client;
	}

	synchronized void cleanup() {
		try {
			if (clientSocket != null) {
				s_logger.info("Closing connection to "
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
//			clientOuts = new GZIPOutputStream(clientSocket.getOutputStream(), 65536);
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
			ConsoleProxyViewer viewer = ConsoleProxy.getVncViewer(host, port, sid, "", "");
			
			ConsoleProxy.waitForViewerToStart(viewer);
			
			handleClientSession(viewer, srcinfo);
		} catch (Exception ioe) {
			if(s_logger.isDebugEnabled())
				s_logger.debug(ioe.toString());
		} finally {
			cleanup();
		}
	}
	
	void writeServer (ConsoleProxyViewer viewer, byte[] b, int off, int len) {
		viewer.writeServer(b, off, len);
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
	
	void handleClientSession(ConsoleProxyViewer viewer, String srcinfo) throws Exception {
	    s_logger.info("Start to handle client session");
	    
	    viewer.setAjaxViewer(false);
	    
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
	    
	    s_logger.info("Sending ServerInit w=" + viewer.rfb.framebufferWidth + 
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
	    		s_logger.info("Disconnecting client link stream " + 
	    				viewer.clientStream.hashCode() + " from " + 
	    				viewer.clientStreamInfo);
	    		viewer.clientStream.close();
	    	}
	    	viewer.clientStream = clientOuts;
	    	viewer.clientStreamInfo = srcinfo;
	    	viewer.lastUsedTime = System.currentTimeMillis();

    		s_logger.info("Setting client link stream " + 
    				viewer.clientStream.hashCode() + " from " + srcinfo);
	    }

		try { 
			while (!viewer.isDropped()) {
				byte[] b = new byte[512];
				int nbytes = 0;
				int msgType = clientIns.read();
				b[0] = (byte)msgType;
				switch (msgType) {
				case RfbProto.SetPixelFormat:
					clientIns.readFully(b, 1, 19);
					nbytes = 20;
					
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S RFB message SetPixelFormat, size=" + nbytes);
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
					
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S RFB message SetEncodings, size=" + nbytes);
					break;
					
				case RfbProto.FramebufferUpdateRequest:
					clientIns.readFully(b, 1, 9);
					nbytes = 10;
					
					if(s_logger.isDebugEnabled()) {
						int i = b[1];
						int x = ((0xff & b[2]) << 8) + b[3];
						int y = ((0xff & b[4]) << 8) + b[5];
						int w = ((0xff & b[6]) << 8) + b[7];
						int h = ((0xff & b[8]) << 8) + b[9];
						
						s_logger.debug("C->S RFB message FramebufferUpdateRequest, size=" + nbytes + " x=" + x 
							+" y=" + y + " w=" + w + " h=" + h);
					}
					break;
					
				case RfbProto.KeyboardEvent:
					clientIns.readFully(b, 1, 7);
					nbytes = 8;
					
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S RFB message KeyboardEvent, size=" + nbytes);
					break;
				case RfbProto.PointerEvent:
					clientIns.readFully(b, 1, 5);
					nbytes = 6;
					
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S RFB message PointerEvent, size=" + nbytes);
					break;
					
				case RfbProto.VMOpsClientCustom:
					clientIns.read(); // read and ignore, used to track liveliness
									// of the client
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S RFB message VMOpsClientCustom");
					break;
					
				default:
					if(s_logger.isDebugEnabled())
						s_logger.debug("C->S unknown message type: " + msgType + ", size=" + nbytes);
					throw new Exception("Bad client event type: " + msgType);
				}
				writeServer(viewer, b, 0, nbytes);
			}
		} finally {
			viewer.lastUsedTime = System.currentTimeMillis();
		}
	}
*/	
}
