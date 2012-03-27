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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.cloud.consoleproxy.util.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConsoleProxyAjaxImageHandler implements HttpHandler {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyAjaxImageHandler.class);

	public void handle(HttpExchange t) throws IOException {
		try {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("AjaxImageHandler " + t.getRequestURI());
	        
	        long startTick = System.currentTimeMillis();
	        
	        doHandle(t);
	        
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug(t.getRequestURI() + "Process time " + (System.currentTimeMillis() - startTick) + " ms");
		} catch (IOException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			s_logger.warn("Exception, ", e);
			t.sendResponseHeaders(400, -1);		// bad request
		} catch(OutOfMemoryError e) {
			s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
			System.exit(1);
		} catch(Throwable e) {
			s_logger.error("Unexpected exception, ", e);
			t.sendResponseHeaders(500, -1);		// server error
		} finally {
			t.close();
		}
	}

	private void doHandle(HttpExchange t) throws Exception, IllegalArgumentException {
		String queries = t.getRequestURI().getQuery();
		Map<String, String> queryMap = getQueryMap(queries);
		
		String host = queryMap.get("host");
		String portStr = queryMap.get("port");
		String sid = queryMap.get("sid");
		String tag = queryMap.get("tag");
		String ticket = queryMap.get("ticket");
		String keyStr = queryMap.get("key");
		int key = 0;
		
		if(tag == null)
			tag = "";
		
		int port;
		if(host == null || portStr == null || sid == null)
			throw new IllegalArgumentException();
		
		try {
			port = Integer.parseInt(portStr);
		} catch (NumberFormatException e) {
			s_logger.warn("Invalid numeric parameter in query string: " + portStr);
			throw new IllegalArgumentException(e);
		}
		
		try {
			key = Integer.parseInt(keyStr);
		} catch (NumberFormatException e) {
			s_logger.warn("Invalid numeric parameter in query string: " + keyStr);
			throw new IllegalArgumentException(e);
		}

		ConsoleProxyClient viewer = ConsoleProxy.getVncViewer(host, port, sid, tag, ticket);
		byte[] img = viewer.getAjaxImageCache().getImage(key);
		if(img != null) {
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", "image/jpeg");
			t.sendResponseHeaders(200, img.length);
			
			OutputStream os = t.getResponseBody();
			try {
				os.write(img, 0, img.length);
			} finally {
				os.close();
			}
		} else {
			if(s_logger.isInfoEnabled())
				s_logger.info("Image has already been swept out, key: " + key);
			t.sendResponseHeaders(404, -1);
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
