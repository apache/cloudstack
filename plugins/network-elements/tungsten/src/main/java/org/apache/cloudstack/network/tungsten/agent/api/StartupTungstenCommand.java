package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;

public class StartupTungstenCommand extends StartupCommand {
    public StartupTungstenCommand() {
        super(Host.Type.L2Networking);
    }
}
