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

import com.cloud.utils.Pair;


/**
 * Ensures that network firewall rules stay updated as VMs go up and down
 *
 */
public interface SecurityGroupManager {
	
	public static final String DEFAULT_GROUP_NAME = "default"; 
	public static final String DEFAULT_GROUP_DESCRIPTION = "Default Security Group"; 
	
	public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId, String accountName);
	
	public SecurityGroupVO createDefaultSecurityGroup( Long accountId);
	
	public boolean addInstanceToGroups(Long userVmId, List<String> groups);

	public void removeInstanceFromGroups(Long userVmId);

	public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates);
	
	public String getSecurityGroupsNamesForVm(long vmId);
}
