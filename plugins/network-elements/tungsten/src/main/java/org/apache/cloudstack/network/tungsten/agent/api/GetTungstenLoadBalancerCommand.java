package org.apache.cloudstack.network.tungsten.agent.api;

public class GetTungstenLoadBalancerCommand extends TungstenCommand {
    private final String projectFqn;
    private final String lbName;

    public GetTungstenLoadBalancerCommand(final String projectFqn, final String lbName) {
        this.projectFqn = projectFqn;
        this.lbName = lbName;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getLbName() {
        return lbName;
    }
}
