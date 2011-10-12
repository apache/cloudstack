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
package com.cloud.network;

import java.util.List;
import java.util.Map;

import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.RestartNetworkCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;

public interface NetworkService {

    List<? extends Network> getVirtualNetworksOwnedByAccountInZone(long zoneId, Account owner);

    List<? extends NetworkOffering> listNetworkOfferings();

    IpAddress allocateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * Associates a public IP address for a router.
     * 
     * @param cmd
     *            - the command specifying ipAddress
     * @return ip address object
     * @throws ResourceAllocationException
     *             , InsufficientCapacityException
     */
    IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException, ResourceUnavailableException;

    boolean disassociateIpAddress(long ipAddressId);

    Network createNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException;

    List<? extends Network> searchForNetworks(ListNetworksCmd cmd);

    boolean deleteNetwork(long networkId);

    boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    int getActiveNicsInNetwork(long networkId);

    Network getNetwork(long networkId);

    IpAddress getIp(long id);

    NetworkProfile convertNetworkToNetworkProfile(long networkId);

    Map<Service, Map<Capability, String>> getZoneCapabilities(long zoneId);

    Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId, long zoneId);

    boolean isNetworkAvailableInDomain(long networkId, long domainId);

    Long getDedicatedNetworkDomain(long networkId);

    Network updateNetwork(long networkId, String name, String displayText, List<String> tags, Account caller, String domainSuffix, long networkOfferingId);

    Integer getNetworkRate(long networkId, Long vmId);

    Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType);
    
    Map<String, String> listNetworkOfferingServices(long networkOfferingId);
}
