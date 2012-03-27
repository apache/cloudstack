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
import java.io.OutputStreamWriter;

import com.cloud.consoleproxy.util.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConsoleProxyCmdHandler implements HttpHandler {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyCmdHandler.class);
	
	public void handle(HttpExchange t) throws IOException {
		try {
	        Thread.currentThread().setName("Cmd Thread " + 
	        		Thread.currentThread().getId() + " " + t.getRemoteAddress());
			s_logger.info("CmdHandler " + t.getRequestURI());
			doHandle(t);
		} catch (Exception e) {
			s_logger.error(e.toString(), e);
			String response = "Not found";
			t.sendResponseHeaders(404, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch(OutOfMemoryError e) {
			s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
			System.exit(1);
		} catch (Throwable e) {
			s_logger.error(e.toString(), e);
		} finally {
			t.close();
		}
	}
	
	public void doHandle(HttpExchange t) throws Exception {
		String path = t.getRequestURI().getPath();
		int i = path.indexOf("/", 1);
		String cmd = path.substring(i + 1);
		s_logger.info("Get CMD request for " + cmd);
		if (cmd.equals("getstatus")) {
			ConsoleProxyClientStatsCollector statsCollector = ConsoleProxy.getStatsCollector();
			
			Headers hds = t.getResponseHeaders();
			hds.set("Content-Type", "text/plain");
			t.sendResponseHeaders(200, 0);
			OutputStreamWriter os = new OutputStreamWriter(t.getResponseBody());
			statsCollector.getStatsReport(os);
			os.close();
		}
	}
}
