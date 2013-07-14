// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network;

import java.util.List;
import java.util.Map;

import com.cloud.offering.NetworkOffering;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.*;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.GuestVlan;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;

/**
 * The NetworkService interface is the "public" api to entities that make requests to the orchestration engine
 * Such entities are usually the admin and end-user API.
 *
 */
public interface NetworkService {

    List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner);

    IpAddress allocateIP(Account ipOwner, long zoneId, Long networkId) throws ResourceAllocationException,
        InsufficientAddressCapacityException, ConcurrentOperationException;

    boolean releaseIpAddress(long ipAddressId) throws InsufficientAddressCapacityException;

    IpAddress allocatePortableIP(Account ipOwner, int regionId, Long zoneId, Long networkId, Long vpcId) throws ResourceAllocationException,
            InsufficientAddressCapacityException, ConcurrentOperationException;

    boolean releasePortableIpAddress(long ipAddressId);

    Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceAllocationException;

    List<? extends Network> searchForNetworks(ListNetworksCmd cmd);

    boolean deleteNetwork(long networkId);

    boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException;

    int getActiveNicsInNetwork(long networkId);

    Network getNetwork(long networkId);

    Network getNetwork(String networkUuid);

    IpAddress getIp(long id);

    Network updateGuestNetwork(long networkId, String name, String displayText, Account callerAccount, User callerUser,
                               String domainSuffix, Long networkOfferingId, Boolean changeCidr, String guestVmCidr, Boolean displayNetwork);

    PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, 
            List<String> isolationMethods, String broadcastDomainRange, Long domainId, List<String> tags, String name);

    Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword,
            Long startIndex, Long pageSize, String name);

    PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags,
                                          String newVnetRangeString, String state, String removeVlan);

    boolean deletePhysicalNetwork(Long id);

    List<? extends Service> listNetworkServices(String providerName);

    PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName,
            Long destinationPhysicalNetworkId, List<String> enabledServices);

    Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId, String name,
            String state, Long startIndex, Long pageSize);

    PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String state, List<String> enabledServices);

    boolean deleteNetworkServiceProvider(Long id) throws ConcurrentOperationException, ResourceUnavailableException;

    PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId);

    PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId);

    long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType);

    PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType,
            String xenLabel, String kvmLabel, String vmwareLabel, String simulatorLabel, String vlan);

    PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id);

    PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel);

    boolean deletePhysicalNetworkTrafficType(Long id);

    GuestVlan dedicateGuestVlanRange(DedicateGuestVlanRangeCmd cmd);

    Pair<List<? extends GuestVlan>, Integer> listDedicatedGuestVlanRanges(ListDedicatedGuestVlanRangesCmd cmd);

    boolean releaseDedicatedGuestVlanRange(Long dedicatedGuestVlanRangeId);

    Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId);


    Network getExclusiveGuestNetwork(long zoneId);

    List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd);

    List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner);
    
    

    /**
     * @param networkId
     * @param entityId
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     * @throws ResourceAllocationException
     * @throws InsufficientAddressCapacityException
     */
    IpAddress associateIPToNetwork(long ipId, long networkId) throws InsufficientAddressCapacityException,
        ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException;

    /**
     *
     * @param networkName
     * @param displayText
     * @param physicalNetworkId
     * @param vlan
     * @param startIp
     * @param endIP TODO
     * @param gateway
     * @param netmask
     * @param networkOwnerId
     * @param vpcId TODO
     * @param sourceNat
     * @return
     * @throws InsufficientCapacityException
     * @throws ConcurrentOperationException
     * @throws ResourceAllocationException
     */
    Network createPrivateNetwork(String networkName, String displayText, long physicalNetworkId, String vlan,
                                     String startIp, String endIP, String gateway, String netmask, long networkOwnerId, Long vpcId, Boolean sourceNat)
                    throws ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException;

    /* Requests an IP address for the guest nic */
    NicSecondaryIp allocateSecondaryGuestIP(Account account, long zoneId, Long nicId,
            Long networkId, String ipaddress) throws InsufficientAddressCapacityException;

    boolean releaseSecondaryIpFromNic(long ipAddressId);

    /* lists the nic informaton */
    List<? extends Nic> listNics(ListNicsCmd listNicsCmd);

    Map<Network.Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service);
}
