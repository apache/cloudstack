package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupExternalDhcpCommand extends StartupCommand {
	public StartupExternalDhcpCommand() {
		super(Host.Type.ExternalDhcp);
	}
}
