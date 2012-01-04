package com.cloud.network.element;

import java.util.List;
import java.util.Set;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.PublicIpAddress;

public interface IpDeployer {
    /**
     * Apply ip addresses to this network
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException;
}
