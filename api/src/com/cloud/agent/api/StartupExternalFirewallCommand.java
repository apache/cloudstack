package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupExternalFirewallCommand extends StartupCommand {
    
    public StartupExternalFirewallCommand() {
        super(Host.Type.ExternalFirewall);
    }
}
