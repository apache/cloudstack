package com.cloud.network.vpn;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.user.Account;

/**
 * This class describe the behaviour of a single vpn server provider (EG: L2TP, Wireguard, OpenVPN)
 */
public interface RemoteAccessVpnProvider {

    String GetName();

    RemoteAccessVpn createRemoteAccessVpn(long vpnServerAddressId, String ipRange, boolean openFirewall, Boolean forDisplay, Integer listenPort, String implementationData) throws NetworkRuleConflictException;
    RemoteAccessVpn startRemoteAccessVpn(long vpnServerId, boolean openFirewall) throws ResourceUnavailableException;

    boolean destroyRemoteAccessVpn(long vpnId, Account caller, boolean forceCleanup) throws ResourceUnavailableException;



    //boolean destroyRemoteAccessVpnForIp(long ipId, Account caller, boolean forceCleanup) throws ResourceUnavailableException;
}
