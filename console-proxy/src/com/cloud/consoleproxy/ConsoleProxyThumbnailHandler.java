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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.cloud.console.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConsoleProxyThumbnailHandler implements HttpHandler {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyThumbnailHandler.class);
	
	public ConsoleProxyThumbnailHandler() {
	}

	public void handle(HttpExchange t) throws IOException {
		try {
	        Thread.currentThread().setName("JPG Thread " + 
	        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
	        
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("ScreenHandler " + t.getRequestURI());
	        
	        long startTick = System.currentTimeMillis();
			doHandle(t);
			
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug(t.getRequestURI() + "Process time " + (System.currentTimeMillis() - startTick) + " ms");
		} catch (IllegalArgumentException e) {
			String response = "Bad query string";
			s_logger.error(response + ", request URI : " + t.getRequestURI());
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (Throwable e) {
			s_logger.error("Unexpected exception while handing thumbnail request, ", e);
			
			String queries = t.getRequestURI().getQuery();
			Map<String, String> queryMap = getQueryMap(queries);
			int width = 0;
			int height = 0;
			String ws = queryMap.get("w");
			String hs = queryMap.get("h");
			try {
				width = Integer.parseInt(ws);
				height = Integer.parseInt(hs);
			} catch (NumberFormatException ex) {
			}
			width = Math.min(width, 800);
			height = Math.min(height, 600);
			
			BufferedImage img = generateTextImage(width, height, "Cannot Connect");
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
			javax.imageio.ImageIO.write(img, "jpg", bos);
			byte[] bs = bos.toByteArray();
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", "image/jpeg");
			hds.set("Cache-Control", "no-cache");
			hds.set("Cache-Control", "no-store");
			t.sendResponseHeaders(200, bs.length);
			OutputStream os = t.getResponseBody();
			os.write(bs);
			os.close();
			
/*			
			// Send back a dummy image
			File f = new File ("./images/cannotconnect.jpg");
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
*/			
			s_logger.error("Cannot get console, sent error JPG response for " + t.getRequestURI());
			return;
		} finally {
			t.close();
		}
	}
	
	private void doHandle(HttpExchange t) throws Exception,
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
		String tag = queryMap.get("tag");
		if(tag == null)
			tag = "";
		
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

		ConsoleProxyViewer viewer = ConsoleProxy.getVncViewer(host, port, sid, tag);
		
		if (viewer.status != ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
			// use generated image instead of static
			BufferedImage img = generateTextImage(width, height, "Connecting");
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
			javax.imageio.ImageIO.write(img, "jpg", bos);
			byte[] bs = bos.toByteArray();
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", "image/jpeg");
			hds.set("Cache-Control", "no-cache");
			hds.set("Cache-Control", "no-store");
			t.sendResponseHeaders(200, bs.length);
			OutputStream os = t.getResponseBody();
			os.write(bs);
			os.close();
			
/*			
			// Send back a dummy image
			File f = new File ("./images/notready.jpg");
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
*/			
			if(s_logger.isInfoEnabled())
				s_logger.info("Console not ready, sent dummy JPG response, viewer status : " + viewer.status);
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
	
	public static BufferedImage generateTextImage(int w, int h, String text) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, h);
		g.setColor(Color.WHITE);
		try {
			g.setFont(new Font(null, Font.PLAIN, 12));
	  		FontMetrics fm = g.getFontMetrics();
	  		int textWidth = fm.stringWidth(text);
	  		int startx = (w-textWidth) / 2;
	  		if(startx < 0)
	  			startx = 0;
	  		g.drawString(text, startx, h/2);
		} catch (Throwable e) {
			s_logger.warn("Problem in generating text to thumnail image, return blank image");
		}
		return img;
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
