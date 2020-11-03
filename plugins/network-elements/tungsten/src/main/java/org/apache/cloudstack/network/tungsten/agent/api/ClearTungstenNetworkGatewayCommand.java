package org.apache.cloudstack.network.tungsten.agent.api;

public class ClearTungstenNetworkGatewayCommand extends TungstenCommand {
    private final String projectUuid;
    private final String routerName;
    private final long vnId;

    public ClearTungstenNetworkGatewayCommand(final String projectUuid, final String routerName, final long vnId) {
        this.projectUuid = projectUuid;
        this.routerName = routerName;
        this.vnId = vnId;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getRouterName() {
        return routerName;
    }

    public long getVnId() {
        return vnId;
    }
}
