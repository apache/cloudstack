package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupExternalLoadBalancerCommand extends StartupCommand {
    public StartupExternalLoadBalancerCommand() {
        super(Host.Type.ExternalLoadBalancer);
    }

}
