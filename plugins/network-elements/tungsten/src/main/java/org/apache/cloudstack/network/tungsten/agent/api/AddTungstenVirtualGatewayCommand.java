package org.apache.cloudstack.network.tungsten.agent.api;

public class AddTungstenVirtualGatewayCommand extends TungstenCommand {
    private final String inf;
    private final String subnet;
    private final String route;
    private final String vrf;
    private final String netnsName;
    private final String gateway;

    public AddTungstenVirtualGatewayCommand(final String inf, final String subnet, final String route, final String vrf,
        final String netnsName, final String gateway) {
        this.inf = inf;
        this.subnet = subnet;
        this.route = route;
        this.vrf = vrf;
        this.netnsName = netnsName;
        this.gateway = gateway;
    }

    public String getInf() {
        return inf;
    }

    public String getSubnet() {
        return subnet;
    }

    public String getRoute() {
        return route;
    }

    public String getVrf() {
        return vrf;
    }

    public String getNetnsName() {
        return netnsName;
    }

    public String getGateway() {
        return gateway;
    }
}
