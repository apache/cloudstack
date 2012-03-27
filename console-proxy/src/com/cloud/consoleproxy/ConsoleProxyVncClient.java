package com.cloud.consoleproxy;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.cloud.consoleproxy.vnc.FrameBufferCanvas;
import com.cloud.consoleproxy.vnc.RfbConstants;
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
	private Thread worker;
	private boolean workerDone = false;
	
	public ConsoleProxyVncClient() {
	}
	
	public boolean isHostConnected() {
		if(client != null)
			return client.isHostConnected();
		
		return false;
	}
	
	@Override
	public boolean isFrontEndAlive() {
		if(workerDone || System.currentTimeMillis() - getClientLastFrontEndActivityTime() > ConsoleProxy.VIEWER_LINGER_SECONDS*1000)
			return false;
		return true;
	}

	@Override
	public void initClient(final String clientHostAddress, final int clientHostPort, 
			final String clientHostPassword, final String clientTag, final String ticket) {
		this.host = clientHostAddress;
		this.port = clientHostPort;
		this.passwordParam = clientHostPassword;
		this.tag = clientTag;
		this.ticket = ticket;
		
		client = new VncClient(this);
		worker = new Thread(new Runnable() {
			public void run() {
				long startTick = System.currentTimeMillis();
				while(System.currentTimeMillis() - startTick < 7000) {
					try {
						client.connectTo(clientHostAddress, clientHostPort, clientHostPassword);
					} catch (UnknownHostException e) {
						s_logger.error("Unexpected exception: ", e);
					} catch (IOException e) {
						s_logger.error("Unexpected exception: ", e);
					}
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
					
				workerDone = true;
			}
		});
		
		worker.setDaemon(true);
		worker.start();
	}
	
	@Override
	public void closeClient() {
		if(client != null)
			client.shutdown();
	}
	
	@Override
	public void onClientConnected() {
	}
	
	public void onClientClose() {
		ConsoleProxy.removeViewer(this);
	}
	
	@Override
	public void onFramebufferUpdate(int x, int y, int w, int h) {
		super.onFramebufferUpdate(x, y, w, h);
		client.requestUpdate(false);
	}

	public void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers) {
		if(client == null)
			return;
		
		updateFrontEndActivityTime();
		
		switch(event) {
		case KEY_DOWN :
			client.sendClientKeyboardEvent(RfbConstants.KEY_DOWN, code, 0);
			break;
			
		case KEY_UP :
			client.sendClientKeyboardEvent(RfbConstants.KEY_UP, code, 0);
			break;
			
		case KEY_PRESS :
			break;
			
		default :
			assert(false);
			break;
		}
	}
	
	public void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers) {
		if(client == null)
			return;
		
		updateFrontEndActivityTime();

	    int pointerMask = 0;
	    int mask = 1;
	    if(code == 2)
	    	mask = 4;
		if(event == InputEventType.MOUSE_DOWN) {
			pointerMask = mask;
		}
		
		client.sendClientMouseEvent(pointerMask, x, y, code, modifiers);
	}
	
	@Override
	protected FrameBufferCanvas getFrameBufferCavas() {
		if(client != null)
			return client.getFrameBufferCanvas();
		return null;
	}
}
