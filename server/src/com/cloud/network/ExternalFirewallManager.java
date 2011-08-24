/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.util.List;

import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.server.api.response.ExternalFirewallResponse;

public interface ExternalFirewallManager extends ExternalNetworkManager {
	
	public Host addExternalFirewall(AddExternalFirewallCmd cmd);
	
	public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd);
	
	public List<HostVO> listExternalFirewalls(ListExternalFirewallsCmd cmd);
	
	public ExternalFirewallResponse getApiResponse(Host externalFirewall);
		
	public boolean manageGuestNetwork(boolean add, Network network, NetworkOffering offering) throws ResourceUnavailableException;
	
	public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;

	public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddresses) throws ResourceUnavailableException;
	
}
