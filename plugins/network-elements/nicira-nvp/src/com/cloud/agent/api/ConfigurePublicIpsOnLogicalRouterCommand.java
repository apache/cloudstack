package com.cloud.agent.api;

import java.util.List;

public class ConfigurePublicIpsOnLogicalRouterCommand extends Command {
	
	private String logicalRouterUuid;
	private String l3GatewayServiceUuid;
	private List<String> publicCidrs;
	
	public ConfigurePublicIpsOnLogicalRouterCommand(String logicalRouterUuid,
			String l3GatewayServiceUuid,
			List<String> publicCidrs) {
		super();
		this.logicalRouterUuid = logicalRouterUuid;
		this.publicCidrs = publicCidrs;
		this.l3GatewayServiceUuid = l3GatewayServiceUuid;
	}

	public String getLogicalRouterUuid() {
		return logicalRouterUuid;
	}

	public void setLogicalRouterUuid(String logicalRouterUuid) {
		this.logicalRouterUuid = logicalRouterUuid;
	}
	
	public String getL3GatewayServiceUuid() {
		return l3GatewayServiceUuid;
	}

	public void setL3GatewayServiceUuid(String l3GatewayServiceUuid) {
		this.l3GatewayServiceUuid = l3GatewayServiceUuid;
	}

	public List<String> getPublicCidrs() {
		return publicCidrs;
	}

	public void setPublicCidrs(List<String> publicCidrs) {
		this.publicCidrs = publicCidrs;
	}

	@Override
	public boolean executeInSequence() {
		// TODO Auto-generated method stub
		return false;
	}

}
