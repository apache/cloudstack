package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenLoadBalancerListenerCommand extends TungstenCommand {
    private final String projectFqn;
    private final String loadBalancerListenerName;

    public DeleteTungstenLoadBalancerListenerCommand(final String projectFqn, final String loadBalancerListenerName) {
        this.projectFqn = projectFqn;
        this.loadBalancerListenerName = loadBalancerListenerName;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getLoadBalancerListenerName() {
        return loadBalancerListenerName;
    }
}
