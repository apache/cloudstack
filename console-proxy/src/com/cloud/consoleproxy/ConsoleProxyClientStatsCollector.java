package com.cloud.consoleproxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author Kelven Yang
 * ConsoleProxyClientStatsCollector collects client stats for console proxy agent to report
 * to external management software
 */
public class ConsoleProxyClientStatsCollector {
	
	ArrayList<ConsoleProxyConnection> connections;
	
	public ConsoleProxyClientStatsCollector() {
	}
	
	public ConsoleProxyClientStatsCollector(Hashtable<String, ConsoleProxyClient> connMap) {
		setConnections(connMap);
	}
	
	public String getStatsReport() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	private void setConnections(Hashtable<String, ConsoleProxyClient> connMap) {
		
		ArrayList<ConsoleProxyConnection> conns = new ArrayList<ConsoleProxyConnection>();
		Enumeration<String> e = connMap.keys();
	    while (e.hasMoreElements()) {
	    	synchronized (connMap) {
	    		String key = e.nextElement();
		        ConsoleProxyClient client = connMap.get(key);
		         
		        ConsoleProxyConnection conn = new ConsoleProxyConnection();
		         
		        conn.id = client.getClientId();
		        conn.clientInfo = "";
		        conn.host = client.getClientHostAddress();
		        conn.port = client.getClientHostPort();
		        conn.tag = client.getClientTag();
		        conn.createTime = client.getClientCreateTime();
		        conn.lastUsedTime = client.getClientLastFrontEndActivityTime();
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
