package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Command;

public class SetupTungstenVRouterCommand extends Command {
    private final String inf;
    private final String subnet;
    private final String route;
    private final String vrf;
    private final String gateway;

    public SetupTungstenVRouterCommand(final String inf, final String subnet, final String route, final String vrf,
        final String gateway) {
        this.inf = inf;
        this.subnet = subnet;
        this.route = route;
        this.vrf = vrf;
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

    public String getGateway() {
        return gateway;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
