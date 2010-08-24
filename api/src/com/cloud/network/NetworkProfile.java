/**
 * 
 */
package com.cloud.network;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.user.OwnedBy;

public interface NetworkProfile extends OwnedBy {
    
    long getId();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    String getCidr();

    void setCidr(String cidr);
}
