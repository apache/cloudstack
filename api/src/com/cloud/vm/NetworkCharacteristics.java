/**
 * 
 */
package com.cloud.vm;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;

public class NetworkCharacteristics {
    long id;
    BroadcastDomainType type;
    String cidr;
    Mode mode;
    long vmId;
    
    public BroadcastDomainType getType() {
        return type;
    }

    public Mode getMode() {
        return mode;
    }
    
    public long getNetworkId() {
        return id;
    }
    
    public long getVirtualMachineId() {
        return vmId;
    }

    public NetworkCharacteristics() {
    }

    public NetworkCharacteristics(long id, BroadcastDomainType type, String cidr, Mode mode, long vmId) {
        this.id = id;
        this.type = type;
        this.cidr = cidr;
        this.mode = mode;
        this.vmId = vmId;
    }
}
