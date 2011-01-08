package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsCreateGreTunnelAnswer extends Answer {
	String hostIp;
	String remoteIp;
	String bridge;
	String key;
	
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
}
