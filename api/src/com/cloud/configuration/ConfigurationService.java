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

import javax.naming.NamingException;

import com.cloud.api.commands.CreateDiskOfferingCmd;
import com.cloud.api.commands.CreateNetworkOfferingCmd;
import com.cloud.api.commands.CreateServiceOfferingCmd;
import com.cloud.api.commands.CreateVlanIpRangeCmd;
import com.cloud.api.commands.CreateZoneCmd;
import com.cloud.api.commands.DeleteDiskOfferingCmd;
import com.cloud.api.commands.DeleteNetworkOfferingCmd;
import com.cloud.api.commands.DeletePodCmd;
import com.cloud.api.commands.DeleteServiceOfferingCmd;
import com.cloud.api.commands.DeleteVlanIpRangeCmd;
import com.cloud.api.commands.DeleteZoneCmd;
import com.cloud.api.commands.LDAPConfigCmd;
import com.cloud.api.commands.LDAPRemoveCmd;
import com.cloud.api.commands.ListNetworkOfferingsCmd;
import com.cloud.api.commands.UpdateCfgCmd;
import com.cloud.api.commands.UpdateDiskOfferingCmd;
import com.cloud.api.commands.UpdateNetworkOfferingCmd;
import com.cloud.api.commands.UpdatePodCmd;
import com.cloud.api.commands.UpdateServiceOfferingCmd;
import com.cloud.api.commands.UpdateZoneCmd;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;

public interface ConfigurationService {

    /**
     * Updates a configuration entry with a new value
     * 
     * @param cmd
     *            - the command wrapping name and value parameters
     * @return updated configuration object if successful
     */
    Configuration updateConfiguration(UpdateCfgCmd cmd);

    /**
     * Create a service offering through the API
     * 
     * @param cmd
     *            the command object that specifies the name, number of cpu cores, amount of RAM, etc. for the service
     *            offering
     * @return the newly created service offering if successful, null otherwise
     */
    ServiceOffering createServiceOffering(CreateServiceOfferingCmd cmd);

    /**
     * Updates a service offering
     * 
     * @param serviceOfferingId
     * @param userId
     * @param name
     * @param displayText
     * @param offerHA
     * @param useVirtualNetwork
     * @param tags
     * @return updated service offering
     */
    ServiceOffering updateServiceOffering(UpdateServiceOfferingCmd cmd);

    /**
     * Deletes a service offering
     * 
     * @param userId
     * @param serviceOfferingId
     */
    boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd);

    /**
     * Updates a disk offering
     * 
     * @param cmd
     *            - the command specifying diskOfferingId, name, description, tags
     * @return updated disk offering
     * @throws
     */
    DiskOffering updateDiskOffering(UpdateDiskOfferingCmd cmd);

    /**
     * Deletes a disk offering
     * 
     * @param cmd
     *            - the command specifying disk offering id
     * @return true or false
     * @throws
     */
    boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd);

    /**
     * Creates a new disk offering
     * 
     * @param domainId
     * @param name
     * @param description
     * @param numGibibytes
     * @param mirrored
     * @param size
     * @return ID
     */
    DiskOffering createDiskOffering(CreateDiskOfferingCmd cmd);

    /**
     * Creates a new pod based on the parameters specified in the command object
     * 
     * @param zoneId
     *            TODO
     * @param name
     *            TODO
     * @param startIp
     *            TODO
     * @param endIp
     *            TODO
     * @param gateway
     *            TODO
     * @param netmask
     *            TODO
     * @param allocationState
     *            TODO
     * @return the new pod if successful, null otherwise
     * @throws
     * @throws
     */
    Pod createPod(long zoneId, String name, String startIp, String endIp, String gateway, String netmask, String allocationState);

    /**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     * 
     * @param UpdatePodCmd
     *            api command
     */
    Pod editPod(UpdatePodCmd cmd);

    /**
     * Deletes a pod from the database. Will not allow you to delete pods that are being used anywhere in the system.
     * 
     * @param cmd
     *            - the command containing podId
     * @return true or false
     * @throws ,
     */
    boolean deletePod(DeletePodCmd cmd);

    /**
     * Creates a new zone
     * 
     * @param cmd
     * @return the zone if successful, null otherwise
     * @throws
     * @throws
     */
    DataCenter createZone(CreateZoneCmd cmd);

    /**
     * Edits a zone in the database. Will not allow you to edit DNS values if there are VMs in the specified zone.
     * 
     * @param UpdateZoneCmd
     * @return Updated zone
     */
    DataCenter editZone(UpdateZoneCmd cmd);

    /**
     * Deletes a zone from the database. Will not allow you to delete zones that are being used anywhere in the system.
     * 
     * @param userId
     * @param zoneId
     */
    boolean deleteZone(DeleteZoneCmd cmd);

    /**
     * Adds a VLAN to the database, along with an IP address range. Can add three types of VLANs: (1) zone-wide VLANs on
     * the
     * virtual public network (2) pod-wide direct attached VLANs (3) account-specific direct attached VLANs
     * 
     * @param userId
     * @param vlanType
     *            - either "DomR" (VLAN for a virtual public network) or "DirectAttached" (VLAN for IPs that will be
     *            directly
     *            attached to UserVMs)
     * @param zoneId
     * @param accountId
     * @param podId
     * @param add
     * @param vlanId
     * @param gateway
     * @param startIP
     * @param endIP
     * @throws ResourceAllocationException TODO
     * @throws
     * @return The new Vlan object
     */
    Vlan createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, ResourceAllocationException;

    boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd);

    NetworkOffering createNetworkOffering(CreateNetworkOfferingCmd cmd);

    NetworkOffering updateNetworkOffering(UpdateNetworkOfferingCmd cmd);

    List<? extends NetworkOffering> searchForNetworkOfferings(ListNetworkOfferingsCmd cmd);

    boolean deleteNetworkOffering(DeleteNetworkOfferingCmd cmd);

    NetworkOffering getNetworkOffering(long id);

    Integer getNetworkOfferingNetworkRate(long networkOfferingId);

    Account getVlanAccount(long vlanId);

    List<? extends NetworkOffering> listNetworkOfferings(TrafficType trafficType, boolean systemOnly);

    DataCenter getZone(long id);

    ServiceOffering getServiceOffering(long serviceOfferingId);

    Long getDefaultPageSize();

    Integer getServiceOfferingNetworkRate(long serviceOfferingId);

    DiskOffering getDiskOffering(long diskOfferingId);

    boolean updateLDAP(LDAPConfigCmd cmd) throws NamingException;

	boolean removeLDAP(LDAPRemoveCmd cmd);
}
