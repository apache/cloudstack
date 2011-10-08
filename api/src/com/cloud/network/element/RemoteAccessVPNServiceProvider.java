package com.cloud.network.element;

import java.util.List;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;

public interface RemoteAccessVPNServiceProvider extends NetworkElement {
    String[] applyVpnUsers(RemoteAccessVpn vpn, List<? extends VpnUser> users) throws ResourceUnavailableException;

    boolean startVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException;
    
    boolean stopVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException;
    
    boolean isRemoteAccessVPNServiceProvider();
}
