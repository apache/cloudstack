/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.security;

import java.util.HashMap;
import java.util.List;

import com.cloud.api.commands.AuthorizeNetworkGroupIngressCmd;
import com.cloud.api.commands.CreateSecurityGroupCmd;
import com.cloud.api.commands.DeleteSecurityGroupCmd;
import com.cloud.api.commands.ListNetworkGroupsCmd;
import com.cloud.api.commands.RevokeNetworkGroupIngressCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.State;

/**
 * Ensures that network firewall rules stay updated as VMs go up and down
 *
 */
public interface SecurityGroupManager extends Manager {
	
	public static final String DEFAULT_GROUP_NAME = "default"; 
	public static final String DEFAULT_GROUP_DESCRIPTION = "Default Network Group"; 

	public void handleVmStateTransition(UserVm userVm, State vmState);
	
	public List<IngressRuleVO> authorizeNetworkGroupIngress(AuthorizeNetworkGroupIngressCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
	
	public NetworkGroupVO createNetworkGroup(String name, String description, Long domainId, Long accountId, String accountName);

	/**
	 * Create a network group with the given name and description
	 * @param command the command specifying the name and description
	 * @return the created network group if successful, null otherwise
	 */
	public NetworkGroupVO createNetworkGroup(CreateSecurityGroupCmd command) throws PermissionDeniedException, InvalidParameterValueException;
	
	public NetworkGroupVO createDefaultNetworkGroup( Long accountId);
	
	public boolean addInstanceToGroups(Long userVmId, List<NetworkGroupVO> groups);

	public void removeInstanceFromGroups(Long userVmId);

	boolean revokeNetworkGroupIngress(RevokeNetworkGroupIngressCmd cmd);
	
	public void deleteNetworkGroup(DeleteSecurityGroupCmd cmd) throws ResourceInUseException, PermissionDeniedException, InvalidParameterValueException;

    /**
     * Search for network groups and associated ingress rules for the given account, domain, group name, and/or keyword.
     * The search terms are specified in the search criteria.
     * @return the list of network groups and associated ingress rules
     */
    public List<NetworkGroupRulesVO> searchForNetworkGroupRules(ListNetworkGroupsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException;

	public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates);
	
	public String getNetworkGroupsNamesForVm(long vmId);
}
