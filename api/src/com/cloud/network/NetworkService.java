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
import java.util.Set;

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
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.user.Account;

public interface NetworkService {

    List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner);

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

    Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId);

    boolean isNetworkAvailableInDomain(long networkId, long domainId);

    Long getDedicatedNetworkDomain(long networkId);

    Network updateNetwork(long networkId, String name, String displayText, List<String> tags, Account caller, String domainSuffix, Long networkOfferingId);

    Integer getNetworkRate(long networkId, Long vmId);

    Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType);
    
    Map<String, Set<String>> listNetworkOfferingServices(long networkOfferingId);

    PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> isolationMethods, String broadcastDomainRange, Long domainId, List<String> tags);

    List<? extends PhysicalNetwork> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize);

    PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state);

    boolean deletePhysicalNetwork(Long id);

    List<? extends Service> listNetworkServices();

    List<? extends Provider> listSupportedNetworkServiceProviders(String serviceName);

    PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId);

    List<? extends PhysicalNetworkServiceProvider> listNetworkServiceProviders(Long physicalNetworkId);

    PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, Boolean enabled);

    boolean deleteNetworkServiceProvider(Long id);

    PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId);

    PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId);
    
    long translateZoneIdToPhysicalNetworkId(long zoneId);

    PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType, String xenLabel, String kvmLabel, String vmwareLabel);

    PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id);

    PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel);

    boolean deletePhysicalNetworkTrafficType(Long id);

    List<? extends PhysicalNetworkTrafficType> listTrafficTypes(Long physicalNetworkId);

    PhysicalNetwork getDefaultPhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType);
    
}
