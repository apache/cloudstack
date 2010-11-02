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

import com.cloud.api.commands.AddConfigCmd;
import com.cloud.api.commands.CreateDiskOfferingCmd;
import com.cloud.api.commands.CreatePodCmd;
import com.cloud.api.commands.CreateServiceOfferingCmd;
import com.cloud.api.commands.CreateVlanIpRangeCmd;
import com.cloud.api.commands.CreateZoneCmd;
import com.cloud.api.commands.DeleteDiskOfferingCmd;
import com.cloud.api.commands.DeletePodCmd;
import com.cloud.api.commands.DeleteServiceOfferingCmd;
import com.cloud.api.commands.DeleteVlanIpRangeCmd;
import com.cloud.api.commands.DeleteZoneCmd;
import com.cloud.api.commands.UpdateCfgCmd;
import com.cloud.api.commands.UpdateDiskOfferingCmd;
import com.cloud.api.commands.UpdatePodCmd;
import com.cloud.api.commands.UpdateServiceOfferingCmd;
import com.cloud.api.commands.UpdateZoneCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
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
	 * Updates a configuration entry with a new value
	 * @param cmd - the command wrapping name and value parameters
	 * @return updated configuration object if successful
	 * @throws InvalidParameterValueException
	 */
	ConfigurationVO updateConfiguration(UpdateCfgCmd cmd) throws InvalidParameterValueException;

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
	 * Create a service offering through the API
	 * @param cmd the command object that specifies the name, number of cpu cores, amount of RAM, etc. for the service offering
	 * @return the newly created service offering if successful, null otherwise
	 */
    ServiceOfferingVO createServiceOffering(CreateServiceOfferingCmd cmd);

	/**
	 * Updates a service offering
	 * @param serviceOfferingId
	 * @param userId
	 * @param name
	 * @param displayText
	 * @param offerHA
	 * @param useVirtualNetwork
	 * @param tags
	 * @return updated service offering
	 */
//	ServiceOfferingVO updateServiceOffering(long serviceOfferingId, long userId, String name, String displayText, Boolean offerHA, Boolean useVirtualNetwork, String tags);
	ServiceOfferingVO updateServiceOffering(UpdateServiceOfferingCmd cmd);
	
	/**
	 * Updates a disk offering
	 * @param cmd - the command specifying diskOfferingId, name, description, tags
	 * @return updated disk offering
	 * @throws 
	 */
	DiskOfferingVO updateDiskOffering(UpdateDiskOfferingCmd cmd);
	
	/**
	 * Deletes a disk offering
	 * @param cmd - the command specifying disk offering id
	 * @return true or false
	 * @throws 
	 */
	boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd);
	
	/**
	 * Deletes a service offering
	 * @param userId
	 * @param serviceOfferingId
	 */
	boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd);
	
	/**
	 * Creates a new disk offering
	 * @param domainId
	 * @param name
	 * @param description
	 * @param numGibibytes
	 * @param mirrored
	 * @param size
	 * @return ID
	 */
	DiskOfferingVO createDiskOffering(CreateDiskOfferingCmd cmd);

	/**
	 * Creates a new disk offering
	 * @param domainId
	 * @param name
	 * @param description
	 * @param numGibibytes
	 * @param tags
	 * @return newly created disk offering
	 */
	DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, String tags);
    
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
	 * Creates a new pod based on the parameters specified in the command object
	 * @param cmd the command object that specifies the name, zone, gateway, cidr, and ip range for the pod
	 * @return the new pod if successful, null otherwise
	 * @throws 
	 * @throws 
	 */
	HostPodVO createPod(CreatePodCmd cmd);

	/**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     * @param userId
     * @param podId
     * @param newPodName
     * @param cidr
	 * @param startIp
	 * @param endIp
     * @return Pod
	 * @throws  
	 * @throws  
     */
	HostPodVO editPod(UpdatePodCmd cmd);
	
	 /**
     * Deletes a pod from the database. Will not allow you to delete pods that are being used anywhere in the system.
     * @param cmd - the command containing podId
     * @return true or false
     * @throws , 
     */
	boolean deletePod(DeletePodCmd cmd);
	
	/**
	 * Creates a new zone
	 * @param cmd
	 * @return the zone if successful, null otherwise
	 * @throws 
	 * @throws 
	 */
    DataCenterVO createZone(CreateZoneCmd cmd);

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
     * @return
     * @throws 
     * @throws 
     */
    DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr, String domain, Long domainId);
    
    /**
     * Edits a zone in the database. Will not allow you to edit DNS values if there are VMs in the specified zone.
     * @param UpdateZoneCmd
     * @return Updated zone
     */
    DataCenterVO editZone(UpdateZoneCmd cmd);

    /**
     * Deletes a zone from the database. Will not allow you to delete zones that are being used anywhere in the system.
     * @param userId
     * @param zoneId
     */
    void deleteZone(DeleteZoneCmd cmd);
	
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
	 * Adds a VLAN to the database, along with an IP address range. Can add three types of VLANs: (1) zone-wide VLANs on the virtual public network (2) pod-wide direct attached VLANs (3) account-specific direct attached VLANs
	 * @param userId
	 * @param vlanType - either "DomR" (VLAN for a virtual public network) or "DirectAttached" (VLAN for IPs that will be directly attached to UserVMs)
	 * @param zoneId
	 * @param accountId
	 * @param podId
	 * @param add
	 * @param vlanId
	 * @param gateway
	 * @param startIP
	 * @param endIP
	 * @throws 
	 * @return The new VlanVO object
	 */
	VlanVO createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException;
//    VlanVO createVlanAndPublicIpRange(long userId, VlanType vlanType, Long zoneId, Long accountId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws , ;

	/**
	 * Deletes a VLAN from the database, along with all of its IP addresses. Will not delete VLANs that have allocated IP addresses.
	 * @param userId
	 * @param vlanDbId
	 * @return success/failure
	 */
	boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId);
	boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd);
	
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
	
	/**
	 * Persists a config value via the API call
	 * @return newly created Config object
	 */
	ConfigurationVO addConfig(AddConfigCmd cmd);
}
