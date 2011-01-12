package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsCreateGreTunnelAnswer extends Answer {
	String hostIp;
	String remoteIp;
	String bridge;
	String key;
	long from;
	long to;
	int port;
	
	public OvsCreateGreTunnelAnswer(Command cmd, boolean success, String details) {
		super(cmd, success, details);
	}
	
	public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
			String details, String hostIp, String bridge) {
		super(cmd, success, details);
		OvsCreateGreTunnelCommand c = (OvsCreateGreTunnelCommand)cmd;
		this.hostIp = hostIp;
		this.bridge = bridge;
		this.remoteIp = c.getRemoteIp();
		this.key = c.getKey();
		this.port = -1;
		this.from = c.getFrom();
		this.to = c.getTo();
	}
	
	public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
			String details, String hostIp, String bridge, int port) {
		this(cmd, success, details, hostIp, bridge);
		this.port = port;
	}
	
	public String getHostIp() {
		return hostIp;
	}
	
	public String getRemoteIp() {
		return remoteIp;
	}
	
	public String getBridge() {
		return bridge;
	}
	
	public String getKey() {
		return key;
	}
	
	public long getFrom() {
		return from;
	}
	
	public long getTo() {
		return to;
	}
	
	public int getPort() {
		return port;
	}
}
