package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class InvestigateDhcpEntryAnswer extends Answer {
	String mac;
	String ip;
	String state;
	
	public String getMac() {
		return mac;
	}
	
	public String getIp() {
		return ip;
	}
	
	public String getState() {
		return state;
	}
	
	public InvestigateDhcpEntryAnswer(Command cmd, boolean success, String details) {
		super(cmd, success, details);
		InvestigateDhcpEntryCommand icmd = (InvestigateDhcpEntryCommand)cmd;
		mac = icmd.getMac();
		ip = icmd.getIp();
		state = icmd.getState();
	}
}
