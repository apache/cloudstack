package com.cloud.consoleproxy;

import java.awt.Image;
import java.awt.Rectangle;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.consoleproxy.util.TileInfo;
import com.cloud.consoleproxy.util.TileTracker;
import com.cloud.consoleproxy.vnc.FrameBufferCanvas;

/**
 * 
 * @author Kelven Yang
 * 
 * ConsoleProxyClientBase implements a base console client class that can be extended to realize
 * an instance of specialized console protocol implementation, such as VNC or RDP
 * 
 * It mainly implements the features needed by front-end AJAX viewer
 * 
 */
public abstract class ConsoleProxyClientBase implements ConsoleProxyClient, ConsoleProxyClientListener {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyClientBase.class);
	
	private static int s_nextClientId = 0;
	protected int clientId = getNextClientId();
	
	protected long ajaxSessionId = 0;
	
	protected boolean dirtyFlag = false;
	protected Object tileDirtyEvent = new Object();
	protected TileTracker tracker;
	protected AjaxFIFOImageCache ajaxImageCache = new AjaxFIFOImageCache(2);
	
	protected String host;
	protected int port;
	protected String passwordParam;
	protected String tag = "";
	protected String ticket = "";
	protected long createTime = System.currentTimeMillis();
	protected long lastFrontEndActivityTime = System.currentTimeMillis();

	protected boolean framebufferResized = false;
	protected int resizedFramebufferWidth;
	protected int resizedFramebufferHeight;

	public ConsoleProxyClientBase() {
		tracker = new TileTracker();
		tracker.initTracking(64, 64, 800, 600);
	}

	//
	// interface ConsoleProxyClient
	//
	@Override
	public int getClientId() {
		return clientId;
	}
	
	public abstract boolean isHostConnected();
	public abstract boolean isFrontEndAlive();

	@Override
	public long getAjaxSessionId() {
		return this.ajaxSessionId;
	}
	
	@Override
	public AjaxFIFOImageCache getAjaxImageCache() {
		return ajaxImageCache;
	}
	
	public Image getClientScaledImage(int width, int height) {
		FrameBufferCanvas canvas = getFrameBufferCavas();
		if(canvas != null)
			return canvas.getFrameBufferScaledImage(width, height);
		
		return null;
	}
	
	public abstract void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers);
	public abstract void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers);
	
	@Override
	public long getClientCreateTime() {
		return createTime;
	}
	
	@Override
	public long getClientLastFrontEndActivityTime() {
		return lastFrontEndActivityTime;
	}
	
	@Override
	public String getClientHostAddress() {
		return host;
	}
	
	@Override
	public int getClientHostPort() {
		return port;
	}
	
	@Override
	public String getClientHostPassword() {
		return passwordParam;
	}
	
	@Override
	public String getClientTag() {
		return tag;
	}

	@Override
	public abstract void initClient(String clientHostAddress, int clientHostPort, 
		String clientHostPassword, String clientTag, String ticket);
		
	@Override
	public abstract void closeClient();
	
	//
	// interface FrameBufferEventListener
	//
	@Override
	public void onFramebufferSizeChange(int w, int h) {
		tracker.resize(w, h);

		synchronized(this) {
			framebufferResized = true;
			resizedFramebufferWidth = w;
			resizedFramebufferHeight = h;
		}
		
		signalTileDirtyEvent();
	}

	@Override
	public void onFramebufferUpdate(int x, int y, int w, int h) {
		if(s_logger.isTraceEnabled())
			s_logger.trace("Frame buffer update {" + x + "," + y + "," + w + "," + h + "}");
		tracker.invalidate(new Rectangle(x, y, w, h));
		
		signalTileDirtyEvent();
	}
	
	//
	// AJAX Image manipulation 
	//
	public byte[] getFrameBufferJpeg() {
		FrameBufferCanvas canvas = getFrameBufferCavas();
		if(canvas != null)
			return canvas.getFrameBufferJpeg();
		
		return null;
	}
	
	public byte[] getTilesMergedJpeg(List<TileInfo> tileList, int tileWidth, int tileHeight) {
		FrameBufferCanvas canvas = getFrameBufferCavas();
		if(canvas != null)
			return canvas.getTilesMergedJpeg(tileList, tileWidth, tileHeight);
		return null;
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
		if(init) {
			synchronized(this) {
				ajaxSessionId++;
			}
		}

		StringBuffer sb = new StringBuffer();
		
		sb.append("/ajax?host=").append(host).append("&port=").append(port);
		sb.append("&sid=").append(passwordParam).append("&sess=").append(ajaxSessionId);
		return sb.toString();
	}

	@Override
	public String onAjaxClientKickoff() {
		return "onKickoff();";
	}
	
	private boolean waitForViewerReady() {
		long startTick = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTick < 5000) {
			if(getFrameBufferCavas() != null)
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

	@Override
	public String onAjaxClientStart(String title, List<String> languages, String guest) {
		updateFrontEndActivityTime();
		
		if(!waitForViewerReady())
			return onAjaxClientConnectFailed();
		
		synchronized(this) {
			ajaxSessionId++;
			framebufferResized = false;
		}
		
		int tileWidth = tracker.getTileWidth();
		int tileHeight = tracker.getTileHeight();
		int width = tracker.getTrackWidth();
		int height = tracker.getTrackHeight();
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Ajax client start, frame buffer w: " + width + ", " + height);
		
		int retry = 0;
		tracker.initCoverageTest();
		while(!tracker.hasFullCoverage() && retry < 10) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			retry++;
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

		return getAjaxViewerPageContent(sbTileSequence.toString(), imgUrl, 
			updateUrl, width, height, tileWidth, tileHeight, title, 
			ConsoleProxy.keyboardType == ConsoleProxy.KEYBOARD_RAW, 
			languages, guest);
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

	@Override
	public String onAjaxClientUpdate() {
		updateFrontEndActivityTime();
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

		return getAjaxViewerUpdatePageContent(sbTileSequence.toString(), imgUrl, doResize, 
			resizedFramebufferWidth, resizedFramebufferHeight, 
			tracker.getTileWidth(), tracker.getTileHeight());
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
	
	//
	// Helpers
	//
	private synchronized static int getNextClientId() {
		return ++s_nextClientId;
	}
	
	private void signalTileDirtyEvent() {
		synchronized(tileDirtyEvent) {
			dirtyFlag = true;
			tileDirtyEvent.notifyAll();
		}
	}
	
	public void updateFrontEndActivityTime() {
		lastFrontEndActivityTime = System.currentTimeMillis(); 
	}

	protected abstract FrameBufferCanvas getFrameBufferCavas();
}
