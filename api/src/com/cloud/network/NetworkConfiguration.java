/**
 * 
 */
package com.cloud.network;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;

/**
 * A NetworkProfile defines the specifics of a network
 * owned by an account. 
 */
public interface NetworkConfiguration {
    
    /**
     * @return id of the network profile.  Null means the network profile is not from the database.
     */
    Long getId();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    String getCidr();

    void setCidr(String cidr);
    
    long getNetworkOfferingId();
}
