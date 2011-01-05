package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsCreateGreTunnelCommand extends Command {
	String remoteIp;
	String key;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public OvsCreateGreTunnelCommand(String remoteIp, String key) {
		this.remoteIp = remoteIp;
		this.key = key;
	}
	
	public String getRemoteIp() {
		return remoteIp;
	}
	
	public String getKey() {
		return key;
	}
}
