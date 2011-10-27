package com.cloud.network.element;

import java.util.List;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;

public interface SourceNatServiceProvider extends NetworkElement {
    /**
     * Apply ip addresses to this network
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException;
}
