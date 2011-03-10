package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class InvestigateDhcpEntryCommand extends Command {
	String mac;
	String ip;
	String state;
	
	public String getIp() {
		return ip;
	}
	
	public String getMac() {
		return mac;
	}
	
	public String getState() {
		return state;
	}
	
	public InvestigateDhcpEntryCommand(String ip, String mac, String state) {
		this.ip = ip;
		this.mac = mac;
		this.state = state;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

}
