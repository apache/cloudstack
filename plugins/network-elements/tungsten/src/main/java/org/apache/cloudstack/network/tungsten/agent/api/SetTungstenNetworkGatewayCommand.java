package org.apache.cloudstack.network.tungsten.agent.api;

public class SetTungstenNetworkGatewayCommand extends TungstenCommand {
    private final String projectUuid;
    private final String routerName;
    private final long vnId;
    private final String vnUuid;
    private final String vnGatewayIp;

    public SetTungstenNetworkGatewayCommand(String projectUuid, final String routerName, final long vnId,
        final String vnUuid, final String vnGatewayIp) {
        this.projectUuid = projectUuid;
        this.routerName = routerName;
        this.vnId = vnId;
        this.vnUuid = vnUuid;
        this.vnGatewayIp = vnGatewayIp;
    }

    public String getRouterName() {
        return routerName;
    }

    public long getVnId() {
        return vnId;
    }

    public String getVnUuid() {
        return vnUuid;
    }

    public String getVnGatewayIp() {
        return vnGatewayIp;
    }

    public String getProjectUuid() {
        return projectUuid;
    }
}
