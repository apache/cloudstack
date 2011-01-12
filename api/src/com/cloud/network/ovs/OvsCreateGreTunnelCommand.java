package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsCreateGreTunnelCommand extends Command {
	String remoteIp;
	String key;
	long from;
	long to;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public OvsCreateGreTunnelCommand(String remoteIp, String key, long from, long to) {
		this.remoteIp = remoteIp;
		this.key = key;
		this.from = from;
		this.to = to;
	}
	
	public String getRemoteIp() {
		return remoteIp;
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
}
