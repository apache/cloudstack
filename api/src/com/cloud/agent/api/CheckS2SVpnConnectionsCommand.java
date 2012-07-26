package com.cloud.agent.api;

import java.util.List;

import com.cloud.agent.api.routing.NetworkElementCommand;

public class CheckS2SVpnConnectionsCommand extends NetworkElementCommand {
    List<String> vpnIps;
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    public CheckS2SVpnConnectionsCommand(List<String> vpnIps) {
        super();
        this.vpnIps = vpnIps;
    }
    
    public List<String> getVpnIps() {
        return vpnIps;
    }
}
