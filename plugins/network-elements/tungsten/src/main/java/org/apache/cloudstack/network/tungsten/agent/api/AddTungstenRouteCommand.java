package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.List;

public class AddTungstenRouteCommand extends TungstenCommand {
    private final String inf;
    private final List<String> subnetList;
    private final List<String> routeList;
    private final String vrf;
    private final String host;

    public AddTungstenRouteCommand(final String inf, final List<String> subnetList, final List<String> routeList,
        final String vrf, final String host) {
        this.inf = inf;
        this.subnetList = subnetList;
        this.routeList = routeList;
        this.vrf = vrf;
        this.host = host;
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

    public String getHost() {
        return host;
    }
}
