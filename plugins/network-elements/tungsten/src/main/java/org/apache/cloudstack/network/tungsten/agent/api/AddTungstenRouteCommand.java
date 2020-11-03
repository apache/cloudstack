package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.List;

public class AddTungstenRouteCommand extends TungstenCommand {
    private final String inf;
    private final List<String> subnetList;
    private final List<String> routeList;
    private final String vrf;

    public AddTungstenRouteCommand(final String inf, final List<String> subnetList, final List<String> routeList,
        final String vrf) {
        this.inf = inf;
        this.subnetList = subnetList;
        this.routeList = routeList;
        this.vrf = vrf;
    }

    public String getInf() {
        return inf;
    }

    public List<String> getSubnetList() {
        return subnetList;
    }

    public List<String> getRouteList() {
        return routeList;
    }

    public String getVrf() {
        return vrf;
    }
}
