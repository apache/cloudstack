package com.cloud.network.element;

import java.util.List;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.rules.PortForwardingRule;

public interface PortForwardingServiceProvider extends NetworkElement {
    /**
     * Apply rules
     * @param network
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException;

    /**
     * Apply ip addresses to this network service provider
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException;
}
