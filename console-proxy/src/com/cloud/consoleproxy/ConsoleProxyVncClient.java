package com.cloud.consoleproxy;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.console.TileInfo;
import com.cloud.console.TileTracker;
import com.cloud.consoleproxy.vnc.VncClientListener;

public class ConsoleProxyVncClient implements VncClientListener {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyVncClient.class);
	
	private TileTracker tracker;
	
	boolean dirtyFlag = false;
	private Object tileDirtyEvent = new Object();
	private AjaxFIFOImageCache ajaxImageCache = new AjaxFIFOImageCache(2);
	
	@Override
	public void onFramebufferSizeChange(int w, int h) {
		// TODO
	}

	@Override
	public void onFramebufferUpdate(int x, int y, int w, int h) {
		if(s_logger.isTraceEnabled())
			s_logger.trace("Frame buffer update {" + x + "," + y + "," + w + "," + h + "}");
		tracker.invalidate(new Rectangle(x, y, w, h));
		
		signalTileDirtyEvent();
	}
	
	private void signalTileDirtyEvent() {
		synchronized(tileDirtyEvent) {
			dirtyFlag = true;
			tileDirtyEvent.notifyAll();
		}
	}
/*	
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
*/	
}
