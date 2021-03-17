package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenLoadBalancerCommand extends TungstenCommand {
    private final String projectFqn;
    private final String publicNetworkUuid;
    private final String loadBalancerName;
    private final String loadBalancerHealthMonitorName;
    private final String loadBalancerVmiName;
    private final String fipName;
    private final String fiName;

    public DeleteTungstenLoadBalancerCommand(final String projectFqn, final String publicNetworkUuid,
        final String loadBalancerName, final String loadBalancerHealthMonitorName, final String loadBalancerVmiName,
        final String fipName, final String fiName) {
        this.projectFqn = projectFqn;
        this.publicNetworkUuid = publicNetworkUuid;
        this.loadBalancerName = loadBalancerName;
        this.loadBalancerHealthMonitorName = loadBalancerHealthMonitorName;
        this.loadBalancerVmiName = loadBalancerVmiName;
        this.fipName = fipName;
        this.fiName = fiName;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getLoadBalancerHealthMonitorName() {
        return loadBalancerHealthMonitorName;
    }

    public String getLoadBalancerVmiName() {
        return loadBalancerVmiName;
    }

    public String getFipName() {
        return fipName;
    }

    public String getFiName() {
        return fiName;
    }
}
