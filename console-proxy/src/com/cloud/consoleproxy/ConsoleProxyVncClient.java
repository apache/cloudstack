package com.cloud.consoleproxy;

import org.apache.log4j.Logger;

import com.cloud.consoleproxy.vnc.FrameBufferCanvas;
import com.cloud.consoleproxy.vnc.VncClient;

/**
 * 
 * @author Kelven Yang
 * ConsoleProxyVncClient bridges a VNC engine with the front-end AJAX viewer
 * 
 */
public class ConsoleProxyVncClient extends ConsoleProxyClientBase {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyVncClient.class);
	
	private VncClient client;

	public ConsoleProxyVncClient() {
	}

	public void initClient(String clientHostAddress, int clientHostPort, 
		String clientHostPassword, String clientTag) {
		// TODO
	}
	
	public void closeClient() {
		// TODO
	}
	
	protected FrameBufferCanvas getFrameBufferCavas() {
		if(client != null)
			return client.getFrameBufferCanvas();
		return null;
	}
}
