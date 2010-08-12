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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InternalErrorException;
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
	void updateConfiguration(long userId, String name, String value) throws InvalidParameterValueException, InternalErrorException;
	
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
	ServiceOfferingVO updateServiceOffering(long serviceOfferingId, long userId, String name, String displayText, Boolean offerHA, Boolean useVirtualNetwork, String tags);
	
	/**
	 * Updates a disk offering
	 * @param userId
	 * @param diskOfferingId
	 * @param name
	 * @param description
	 * @param tags
	 * @return updated disk offering
	 */
	DiskOfferingVO updateDiskOffering(long userId, long diskOfferingId, String name, String description, String tags);
	
	/**
	 * Deletes a service offering
	 * @param userId
	 * @param serviceOfferingId
	 */
	boolean deleteServiceOffering(long userId, long serviceOfferingId);
	
	/**
	 * Creates a new disk offering
	 * @param domainId
	 * @param name
	 * @param description
	 * @param numGibibytes
	 * @param mirrored
	 * @return ID
	 */
	DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, boolean mirrored, String tags);
	
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
	HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException;
	
	/**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     * @param userId
     * @param podId
     * @param newPodName
     * @param cidr
	 * @param startIp
	 * @param endIp
     * @return Pod
     */
	HostPodVO editPod(long userId, long podId, String newPodName, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException;
	
	 /**
     * Deletes a pod from the database. Will not allow you to delete pods that are being used anywhere in the system.
     * @param userId
     * @param podId
     */
	void deletePod(long userId,long podId) throws InvalidParameterValueException, InternalErrorException;
	
	/**
	 * Creates a new zone
	 * @param userId
	 * @param zoneName
	 * @param dns1
	 * @param dns2
	 * @param dns3
	 * @param dns4
	 * @param vnetRange
	 * @param guestNetworkCidr
	 * @return Zone
	 */
	DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String dns3, String dns4, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException;
	
	/**
     * Edits a zone in the database. Will not allow you to edit DNS values if there are VMs in the specified zone.
     * @param userId
     * @param zoneId
     * @param newZoneName
     * @param dns1
     * @param dns2
     * @param dns3
     * @param dns4
     * @param vnetRange
     * @return Zone
     * @return guestCidr
     */
	DataCenterVO editZone(long userId, long zoneId, String newZoneName, String dns1, String dns2, String dns3, String dns4, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException;
	
	/**
     * Deletes a zone from the database. Will not allow you to delete zones that are being used anywhere in the system.
     * @param userId
     * @param zoneId
     */
	void deleteZone(long userId, long zoneId) throws InvalidParameterValueException, InternalErrorException;
	
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
	 * @throws InvalidParameterValueException
	 * @return The new VlanVO object
	 */
	VlanVO createVlanAndPublicIpRange(long userId, VlanType vlanType, Long zoneId, Long accountId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws InvalidParameterValueException, InternalErrorException;
	
	/**
	 * Deletes a VLAN from the database, along with all of its IP addresses. Will not delete VLANs that have allocated IP addresses.
	 * @param userId
	 * @param vlanDbId
	 * @return success/failure
	 */
	boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) throws InvalidParameterValueException;
	
	/**
	 * Adds/deletes private IPs
	 * @param add - either true or false
	 * @param podId
	 * @param startIP
	 * @param endIP
	 * @return Message to display to user
	 * @throws InvalidParameterValueException if unable to add private ip range
	 */
	String changePrivateIPRange(boolean add, long podId, String startIP, String endIP) throws InvalidParameterValueException;
	
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
	 * Returns a flag that describes whether the manager is being used in a Premium context or not.
	 * @return true for Premium, false for not
	 */
	boolean isPremium();

}
