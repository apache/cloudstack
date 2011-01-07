package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsDeleteFlowCommand extends Command {
	String vmName;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public String getVmName() {
		return vmName;
	}
	
	public OvsDeleteFlowCommand(String vmName) {
		this.vmName = vmName;
	}
}
