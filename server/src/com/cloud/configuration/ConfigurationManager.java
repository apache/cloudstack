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
package com.cloud.configuration;

import java.util.List;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.DataCenterNetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.component.Manager;

/**
 * ConfigurationManager handles adding pods/zones, changing IP ranges, enabling external firewalls, and editing configuration values
 *
 */
public interface ConfigurationManager extends Manager {
	
	/**
	 * Updates a configuration entry with a new value
	 * @param userId
	 * @param name
	 * @param value
	 */
	void updateConfiguration(long userId, String name, String value);

	/**
	 * Creates a new service offering
	 * @param id
	 * @param name
	 * @param cpu
	 * @param ramSize
	 * @param speed
	 * @param displayText
	 * @param localStorageRequired
	 * @param offerHA
	 * @param useVirtualNetwork
	 * @return ID
	 */
	ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags);
	


	/**
	 * Creates a new disk offering
	 * @param domainId
	 * @param name
	 * @param description
	 * @param numGibibytes
	 * @param tags
	 * @param isCustomized
	 * @return newly created disk offering
	 */
	DiskOfferingVO createDiskOffering(long domainId, String name, String description, Long numGibibytes, String tags, Boolean isCustomized);
    
	/**
	 * Creates a new pod
	 * @param userId
	 * @param podName
	 * @param zoneId
	 * @param gateway
	 * @param cidr
	 * @param startIp
	 * @param endIp
	 * @return Pod
	 */
	HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp);


    /**
     * Creates a new zone
     * @param userId
     * @param zoneName
     * @param dns1
     * @param dns2
     * @param internalDns1
     * @param internalDns2
     * @param vnetRange
     * @param guestCidr
     * @param zoneType
     * @return
     * @throws 
     * @throws 
     */
    DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr, String domain, Long domainId, DataCenterNetworkType zoneType);
    
	/**
	 * Associates an ip address list to an account.  The list of ip addresses are all addresses associated with the given vlan id.
	 * @param userId
	 * @param accountId
	 * @param zoneId
	 * @param vlanId
	 * @throws InsufficientAddressCapacityException
	 * @throws 
	 */
    public void associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId) throws InsufficientAddressCapacityException, ConcurrentOperationException;

	/**
	 * Deletes a VLAN from the database, along with all of its IP addresses. Will not delete VLANs that have allocated IP addresses.
	 * @param userId
	 * @param vlanDbId
	 * @return success/failure
	 */
	boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId);

	
	/**
	 * Adds/deletes private IPs
	 * @param add - either true or false
	 * @param podId
	 * @param startIP
	 * @param endIP
	 * @return Message to display to user
	 * @throws  if unable to add private ip range
	 */
	String changePrivateIPRange(boolean add, long podId, String startIP, String endIP);
	
	/**
	 * Converts a comma separated list of tags to a List
	 * @param tags
	 * @return List of tags
	 */
	List<String> csvTagsToList(String tags);
	
	/**
	 * Converts a List of tags to a comma separated list
	 * @param tags
	 * @return String containing a comma separated list of tags
	 */
	String listToCsvTags(List<String> tags);

	void checkAccess(Account caller, DataCenter zone)
			throws PermissionDeniedException;
	
}
