package org.apache.cloudstack.agent.api;

import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;

public class StartupNsxCommand extends StartupCommand {

    public StartupNsxCommand() {
        super(Host.Type.L2Networking);
    }
}
