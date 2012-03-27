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
import java.net.InetSocketAddress;

import javax.net.ssl.SSLServerSocket;

import com.cloud.consoleproxy.util.Logger;
import com.sun.net.httpserver.HttpServer;

public class ConsoleProxyBaseServerFactoryImpl implements ConsoleProxyServerFactory {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyBaseServerFactoryImpl.class);
	
	@Override
	public void init(byte[] ksBits, String ksPassword) {
	}
	
	@Override
	public HttpServer createHttpServerInstance(int port) throws IOException {
		if(s_logger.isInfoEnabled())
			s_logger.info("create HTTP server instance at port: " + port);
		return HttpServer.create(new InetSocketAddress(port), 5);
	}
	
	@Override
	public SSLServerSocket createSSLServerSocket(int port) throws IOException {
		if(s_logger.isInfoEnabled())
			s_logger.info("SSL server socket is not supported in ConsoleProxyBaseServerFactoryImpl");
		
		return null;
	}
}
