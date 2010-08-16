/**
 * 
 */
package com.cloud.vm;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;

public class NetworkCharacteristics {
    long id;
    BroadcastDomainType type;
    String ip4Address;
    String netmask;
    String gateway;
    Mode mode;
    String[] dns;
    
    public BroadcastDomainType getType() {
        return type;
    }

    public String[] getDns() {
        return dns;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public Mode getMode() {
        return mode;
    }
    
    public long getNetworkId() {
        return id;
    }

    public NetworkCharacteristics() {
    }

    public NetworkCharacteristics(long id, BroadcastDomainType type, String ip4Address, String netmask, String gateway, Mode mode, String[] dns) {
        this.id = id;
        this.type = type;
        this.ip4Address = ip4Address;
        this.netmask = netmask;
        this.gateway = gateway;
        this.mode = mode;
        this.dns = dns;
    }
}
