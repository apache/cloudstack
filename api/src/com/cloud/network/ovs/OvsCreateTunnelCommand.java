package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsCreateTunnelCommand extends Command {
	String key;
	String remoteIp;
	Long from;
	Long to;
	long account;
	
	//for debug info
	String fromIp;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}
	
	public OvsCreateTunnelCommand(String remoteIp, String key, Long from, Long to, long account, String fromIp) {
	    this.remoteIp = remoteIp;
	    this.key = key;
	    this.from = from;
	    this.to = to;
	    this.account = account;
	    this.fromIp = fromIp;
	}
	
	public String getKey() {
	    return key;
	}
	
	public String getRemoteIp() {
	    return remoteIp;
	}
	
	public Long getFrom() {
	    return from;
	}
	
	public Long getTo() {
	    return to;
	}
	
	public long getAccount() {
	    return account;
	}
	
	public String getFromIp() {
	    return fromIp;
	}

}
