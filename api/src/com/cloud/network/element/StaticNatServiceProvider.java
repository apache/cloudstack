package com.cloud.network.element;

import java.util.List;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.rules.StaticNat;

public interface StaticNatServiceProvider extends NetworkElement {
    /**
     * Creates static nat rule (public IP to private IP mapping) on the network element
     * @param config
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException;

    /**
     * Apply ip addresses to this network service provider
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException;
}
