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
    enum State {
        Allocated,  // Indicates the network configuration is in allocated but not setup.
        Setup,      // Indicates the network configuration is setup.
        InUse;      // Indicates the network configuration is in use.
    }
    
    /**
     * @return id of the network profile.  Null means the network profile is not from the database.
     */
    Long getId();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    String getCidr();

    long getDataCenterId();
    
    long getNetworkOfferingId();
    
    State getState();
}
