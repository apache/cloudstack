package com.cloud.network.element;

import java.util.List;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;

public interface LoadBalancingServiceProvider extends NetworkElement {
    /**
     * Apply rules
     * @param network
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException;

    IpDeployer getIpDeployer(Network network);
    /**
     * Validate rules
     * @param network
     * @param rule
     * @return true/false. true should be return if there are no validations. false should be return if any oneof the validation fails.
     * @throws 
     */
    boolean validateLBRule(Network network, LoadBalancingRule rule);
}
