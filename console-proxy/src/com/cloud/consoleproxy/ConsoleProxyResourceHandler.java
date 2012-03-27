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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.cloud.consoleproxy.util.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConsoleProxyResourceHandler implements HttpHandler {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyResourceHandler.class);

	static Map<String, String> s_mimeTypes;
	static {
		s_mimeTypes = new HashMap<String, String>();
		s_mimeTypes.put("jar", "application/java-archive");
		s_mimeTypes.put("js", "text/javascript");
		s_mimeTypes.put("css", "text/css");
		s_mimeTypes.put("jpg", "image/jpeg");
		s_mimeTypes.put("html", "text/html");
		s_mimeTypes.put("htm", "text/html");
		s_mimeTypes.put("log", "text/plain");
	}
	
	static Map<String, String> s_validResourceFolders;
	static {
		s_validResourceFolders = new HashMap<String, String>();
		s_validResourceFolders.put("applet", "");
		s_validResourceFolders.put("logs", "");
		s_validResourceFolders.put("images", "");
		s_validResourceFolders.put("js", "");
		s_validResourceFolders.put("css", "");
		s_validResourceFolders.put("html", "");
	}
	
	public ConsoleProxyResourceHandler() {
	}
	
	public void handle(HttpExchange t) throws IOException {
		try {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("Resource Handler " + t.getRequestURI());
	        
	        long startTick = System.currentTimeMillis();
	        
	        doHandle(t);
	        
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug(t.getRequestURI() + " Process time " + (System.currentTimeMillis() - startTick) + " ms");
		} catch (IOException e) {
			throw e;
		} catch(Throwable e) {
			s_logger.error("Unexpected exception, ", e);
			t.sendResponseHeaders(500, -1);		// server error
		} finally {
			t.close();
		}
	}
	
	@SuppressWarnings("deprecation")
	private void doHandle(HttpExchange t) throws Exception {
		String path = t.getRequestURI().getPath();

		if(s_logger.isInfoEnabled())
			s_logger.info("Get resource request for " + path);
		
		int i = path.indexOf("/", 1);
		String filepath = path.substring(i + 1);
		i = path.lastIndexOf(".");
		String extension = (i == -1) ? "" : path.substring(i + 1);
		String contentType = getContentType(extension);

		if(!validatePath(filepath)) {
			if(s_logger.isInfoEnabled())
				s_logger.info("Resource access is forbidden, uri: " + path);
			
			t.sendResponseHeaders(403, -1);		// forbidden
			return;
		}
		
		File f = new File ("./" + filepath);
		if(f.exists()) {
			long lastModified = f.lastModified();
			String ifModifiedSince = t.getRequestHeaders().getFirst("If-Modified-Since");
			if (ifModifiedSince != null) {
				long d = Date.parse(ifModifiedSince);
				if (d + 1000 >= lastModified) {
					Headers hds = t.getResponseHeaders();
					hds.set("Content-Type", contentType);
					t.sendResponseHeaders(304, -1);
					
					if(s_logger.isInfoEnabled())
						s_logger.info("Sent 304 file has not been " +
								"modified since " + ifModifiedSince);
					return;
				}
			}
			
			long length = f.length();
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", contentType);
			hds.set("Last-Modified", new Date(lastModified).toGMTString());
			t.sendResponseHeaders(200, length);
			responseFileContent(t, f);
			
			if(s_logger.isInfoEnabled())
				s_logger.info("Sent file " + path + " with content type " + contentType);
		} else {
			if(s_logger.isInfoEnabled())
				s_logger.info("file does not exist" + path);
			t.sendResponseHeaders(404, -1);
		}
	}
	
	private static String getContentType(String extension) {
		String key = extension.toLowerCase();
		if(s_mimeTypes.containsKey(key)) {
			return s_mimeTypes.get(key);
		}
		return "application/octet-stream"; 
	}
	
	private static void responseFileContent(HttpExchange t, File f) throws Exception {
		OutputStream os = t.getResponseBody();
		FileInputStream fis = new FileInputStream(f);
		while (true) {
			byte[] b = new byte[8192];
			int n = fis.read(b);
			if (n < 0) {
				break;
			}
			os.write(b, 0, n);
		}
		fis.close();
		os.close();
	}
	
	private static boolean validatePath(String path) {
		int i = path.indexOf("/");
		if(i == -1) {
			if(s_logger.isInfoEnabled())
				s_logger.info("Invalid resource path: can not start at resource root");
			return false;
		}
		
		if(path.contains("..")) {
			if(s_logger.isInfoEnabled())
				s_logger.info("Invalid resource path: contains relative up-level navigation");
			
			return false;
		}
		
		return isValidResourceFolder(path.substring(0, i));
	}
	
	private static boolean isValidResourceFolder(String name) {
		return s_validResourceFolders.containsKey(name); 
	}
}
