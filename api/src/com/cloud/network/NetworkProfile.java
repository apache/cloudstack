/**
 * 
 */
package com.cloud.network;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.user.OwnedBy;

/**
 * A NetworkProfile defines the specifics of a network
 * owned by an account. 
 */
public interface NetworkProfile extends OwnedBy {
    
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
