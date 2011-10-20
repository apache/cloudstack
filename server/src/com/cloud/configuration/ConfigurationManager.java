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
import java.util.Map;
import java.util.Set;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine;

/**
 * ConfigurationManager handles adding pods/zones, changing IP ranges, enabling external firewalls, and editing configuration values
 *
 */
public interface ConfigurationManager extends ConfigurationService, Manager {
	
	/**
	 * Updates a configuration entry with a new value
	 * @param userId
	 * @param name
	 * @param value
	 */
	void updateConfiguration(long userId, String name, String value);

	/**
	 * Creates a new service offering
	 * @param name
	 * @param cpu
	 * @param ramSize
	 * @param speed
	 * @param displayText
	 * @param localStorageRequired
	 * @param offerHA
	 * @param domainId
	 * @param hostTag
	 * @param networkRate TODO
	 * @param id
	 * @param useVirtualNetwork
	 * @return ID
	 */
	ServiceOfferingVO createServiceOffering(long userId, boolean isSystem, VirtualMachine.Type vm_typeType, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean limitResourceUse, String tags, Long domainId, String hostTag, Integer networkRate);
	
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
	DiskOfferingVO createDiskOffering(Long domainId, String name, String description, Long numGibibytes, String tags, boolean isCustomized);
    
	/**
	 * Creates a new pod
	 * @param userId
	 * @param podName
	 * @param zoneId
	 * @param gateway
	 * @param cidr
	 * @param startIp
	 * @param endIp
	 * @param allocationState
	 * @param skipGatewayOverlapCheck (true if it is ok to not validate that gateway IP address overlap with Start/End IP of the POD)
	 * @return Pod
	 */
	HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, String allocationState, boolean skipGatewayOverlapCheck);

    /**
     * Creates a new zone
     * @param userId
     * @param zoneName
     * @param dns1
     * @param dns2
     * @param internalDns1
     * @param internalDns2
     * @param guestCidr
     * @param zoneType
     * @param allocationState
     * @param networkDomain TODO
     * @return
     * @throws 
     * @throws 
     */
    DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String guestCidr, String domain, Long domainId, NetworkType zoneType, String allocationState, String networkDomain);

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

	void checkServiceOfferingAccess(Account caller, ServiceOffering so)
	throws PermissionDeniedException;

	void checkDiskOfferingAccess(Account caller, DiskOffering dof)
			throws PermissionDeniedException;
	
	
	   /**
     * Creates a new network offering
	 * @param name
	 * @param displayText
	 * @param trafficType
	 * @param tags
	 * @param maxConnections
	 * @param networkRate TODO
	 * @param serviceProviderMap TODO
	 * @param isDefault TODO
	 * @param isSecurityGroupEnabled TODO
	 * @param type TODO
	 * @param systemOnly TODO
	 * @param id
	 * @param specifyVlan;
     * @return network offering object
     */

    NetworkOfferingVO createNetworkOffering(long userId, String name, String displayText, TrafficType trafficType, String tags, Integer maxConnections, boolean specifyVlan, Availability availability, Integer networkRate, Map<Service, Set<Provider>> serviceProviderMap, boolean isDefault, boolean isSecurityGroupEnabled, Network.Type type, boolean systemOnly);
    
    Vlan createVlanAndPublicIpRange(Long userId, Long zoneId, Long podId, String startIP, String endIP, String vlanGateway, String vlanNetmask, boolean forVirtualNetwork, String vlanId, Account account, Long networkId) throws InsufficientCapacityException, ConcurrentOperationException, InvalidParameterValueException;
    
    void createDefaultNetworks(long zoneId) throws ConcurrentOperationException;
    
    HostPodVO getPod(long id);
    
    ClusterVO getCluster(long id);
    
    boolean deleteAccountSpecificVirtualRanges(long accountId);
    
    DataCenterVO getZone(long id);
    
    /**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     * @param id
     * @param name
     * @param startIp
     * @param endIp
     * @param gateway
     * @param netmask
     * @param allocationState 
     * @return Pod
     * @throws  
     * @throws  
     */
    Pod editPod(long id, String name, String startIp, String endIp, String gateway, String netmask, String allocationStateStr);
    
}
