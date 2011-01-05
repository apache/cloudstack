package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsSetTagAndFlowCommand extends Command {
	String vlans;
	String vmName;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}
	
	public String getVlans() {
		return vlans;
	}
	
	public String getVmName() {
		return vmName;
	}
	
	public OvsSetTagAndFlowCommand(String vmName, String vlans) {
		this.vmName = vmName;
		this.vlans = vlans;
	}
}
