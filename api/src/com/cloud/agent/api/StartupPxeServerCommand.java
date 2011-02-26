package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupPxeServerCommand extends StartupCommand {
	public StartupPxeServerCommand() {
		super(Host.Type.PxeServer);
	}
}
