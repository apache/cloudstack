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

import com.cloud.console.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConsoleProxyJarHandler implements HttpHandler {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyJarHandler.class);
	
	public void handle(HttpExchange t) throws IOException {
		try {
	        Thread.currentThread().setName("JAR Thread " + 
	        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
			s_logger.debug("JARHandler " + t.getRequestURI());
			doHandle(t);
		} catch (Exception e) {
			s_logger.error(e.toString(), e);
			String response = "Not found";
			t.sendResponseHeaders(404, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (Throwable e) {
			s_logger.error(e.toString(), e);
		} finally {
			t.close();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void doHandle(HttpExchange t) throws Exception {
		String path = t.getRequestURI().getPath();

		s_logger.info("Get JAR request for " + path);
		int i = path.indexOf("/", 1);
		String filepath = path.substring(i + 1);
		i = path.lastIndexOf(".");
		String extension = (i == -1) ? "" : path.substring(i + 1);
		if (!extension.equals("jar")) {
			throw new IllegalArgumentException();
		}
		File f = new File (ConsoleProxy.jarDir + filepath);
		long lastModified = f.lastModified();
		String ifModifiedSince = t.getRequestHeaders().getFirst("If-Modified-Since");
		if (ifModifiedSince != null) {
			long d = Date.parse(ifModifiedSince);
//			s_logger.info(Logger.INFO, "ifModified=" + d + " lastModified =" + lastModified);
			// Give it 1 second grace period to account for errors introduced by
			// date parsing and printing
			if (d + 1000 >= lastModified) {
				Headers hds = t.getResponseHeaders();
				hds.set("Content-Type", "application/java-archive");
				t.sendResponseHeaders(304, -1);
				s_logger.info("Sent 304 JAR file has not been " +
						"modified since " + ifModifiedSince);
//				s_logger.info("Req=" + t.getRequestHeaders().entrySet());
//				s_logger.info("Resp=" + hds.entrySet());
				return;
			}
		}
		long length = f.length();
		FileInputStream fis = new FileInputStream(f);
		Headers hds = t.getResponseHeaders();
		hds.set("Content-Type", "application/java-archive");
		hds.set("Last-Modified", new Date(lastModified).toGMTString());
//		s_logger.info("Req=" + t.getRequestHeaders().entrySet());
//		s_logger.info("Resp=" + hds.entrySet());
		
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
		s_logger.info("Sent JAR file " + path);
	}
}
