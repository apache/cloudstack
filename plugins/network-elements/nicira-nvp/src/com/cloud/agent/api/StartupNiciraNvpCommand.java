package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupNiciraNvpCommand extends StartupCommand {

    public StartupNiciraNvpCommand() {
        super(Host.Type.L2Networking);
    }

}
