/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.util.List;

import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
public interface ExternalLoadBalancerManager extends ExternalNetworkManager {
	
	public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd);
	
	public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd);
	
	public List<HostVO> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd);
	
	public ExternalLoadBalancerResponse getApiResponse(Host externalLoadBalancer);
	
	public boolean manageGuestNetwork(boolean add, Network guestConfig) throws ResourceUnavailableException;
	
	public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;
	
}
