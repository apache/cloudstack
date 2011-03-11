package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class LinMinProvisionDoneCommand extends Command {
	String mac;
	
	public LinMinProvisionDoneCommand(String mac) {
		this.mac = mac;
	}
	
	public String getMac() {
		return mac;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

}
