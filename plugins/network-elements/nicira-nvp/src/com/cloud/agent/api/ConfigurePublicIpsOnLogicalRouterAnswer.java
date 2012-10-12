package com.cloud.agent.api;

public class ConfigurePublicIpsOnLogicalRouterAnswer extends Answer {

	public ConfigurePublicIpsOnLogicalRouterAnswer(Command command,
			boolean success, String details) {
		super(command, success, details);
	}

	public ConfigurePublicIpsOnLogicalRouterAnswer(Command command, Exception e) {
		super(command, e);
	}

}
