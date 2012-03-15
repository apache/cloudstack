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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/*
public class ConsoleProxyStatus {
	ArrayList<ConsoleProxyConnection> connections;
	public ConsoleProxyStatus() {
	}
	public void setConnections(Hashtable<String, ConsoleProxyViewer> connMap) {
		ArrayList<ConsoleProxyConnection> conns = new ArrayList<ConsoleProxyConnection>();
		Enumeration<String> e = connMap.keys();
	    while (e.hasMoreElements()) {
	    	synchronized (connMap) {
		         String key = e.nextElement();
		         ConsoleProxyViewer viewer = connMap.get(key);
		         ConsoleProxyConnection conn = new ConsoleProxyConnection();
		         conn.id = viewer.id;
		         conn.clientInfo = viewer.clientStreamInfo;
		         conn.host = viewer.host;
		         conn.port = viewer.port;
		         conn.tag = viewer.getTag();
		         conn.createTime = viewer.createTime;
		         conn.lastUsedTime = viewer.lastUsedTime;
		         conns.add(conn);
	    	}
	    }
	    connections = conns;
	}
	public static class ConsoleProxyConnection {
		public int id;
		public String clientInfo;
		public String host;
		public int port;
		public String tag;
		public long createTime;
		public long lastUsedTime;
		
		public ConsoleProxyConnection() {
		}
	}
}
*/