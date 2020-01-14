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

import org.apache.cloudstack.api.command.admin.address.ReleasePodIpCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.cloudstack.api.response.AcquirePodIpCmdResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;

/**
 * The NetworkService interface is the "public" api to entities that make requests to the orchestration engine
 * Such entities are usually the admin and end-user API.
 *
 */
public interface NetworkService {

    List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner);

    IpAddress allocateIP(Account ipOwner, long zoneId, Long networkId, Boolean displayIp, String ipaddress) throws ResourceAllocationException, InsufficientAddressCapacityException,
        ConcurrentOperationException;

    boolean releaseIpAddress(long ipAddressId) throws InsufficientAddressCapacityException;

    IpAddress allocatePortableIP(Account ipOwner, int regionId, Long zoneId, Long networkId, Long vpcId) throws ResourceAllocationException,
        InsufficientAddressCapacityException, ConcurrentOperationException;

    boolean releasePortableIpAddress(long ipAddressId);

    Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException;

    Pair<List<? extends Network>, Integer> searchForNetworks(ListNetworksCmd cmd);

    boolean deleteNetwork(long networkId, boolean forced);

    boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup, boolean makeRedundant) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    int getActiveNicsInNetwork(long networkId);

    Network getNetwork(long networkId);

    Network getNetwork(String networkUuid);

    IpAddress getIp(long id);

    Network updateGuestNetwork(final UpdateNetworkCmd cmd);

    /**
     * Migrate a network from one physical network to another physical network
     * @param networkId of the network that needs to be migrated
     * @param networkOfferingId new network offering id for the network
     * @param resume if previous migration failed try to resume of just fail directly because anomaly is detected
     * @return the migrated network
     */
    Network migrateGuestNetwork(long networkId, long networkOfferingId, Account callerAccount, User callerUser, boolean resume);

    /**
     * Migrate a vpc from on physical network to another physical network
     * @param vpcId the id of the vpc that needs to be migrated
     * @param vpcNetworkofferingId the new vpc offering id
     * @param resume if previous migration failed try to resume of just fail directly because anomaly is detected
     * @return the migrated vpc
     */
    Vpc migrateVpcNetwork(long vpcId, long vpcNetworkofferingId, Map<String, String> networkToOffering, Account account, User callerUser, boolean resume);

    PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> isolationMethods, String broadcastDomainRange, Long domainId,
        List<String> tags, String name);

    Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name);

    PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state);

    boolean deletePhysicalNetwork(Long id);

    List<? extends Service> listNetworkServices(String providerName);

    PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId,
        List<String> enabledServices);

    Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId, String name, String state, Long startIndex,
        Long pageSize);

    PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String state, List<String> enabledServices);

    boolean deleteNetworkServiceProvider(Long id) throws ConcurrentOperationException, ResourceUnavailableException;

    PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId);

    PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId);

    PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId);

    long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType);

    PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType, String isolationMethod, String xenLabel, String kvmLabel, String vmwareLabel,
        String simulatorLabel, String vlan, String hypervLabel, String ovm3label);

    PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id);

    PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel, String hypervLabel, String ovm3label);

    boolean deletePhysicalNetworkTrafficType(Long id);

    GuestVlan dedicateGuestVlanRange(DedicateGuestVlanRangeCmd cmd);

    Pair<List<? extends GuestVlan>, Integer> listDedicatedGuestVlanRanges(ListDedicatedGuestVlanRangesCmd cmd);

    boolean releaseDedicatedGuestVlanRange(Long dedicatedGuestVlanRangeId);

    Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId);

    Network getExclusiveGuestNetwork(long zoneId);

    List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd);

    List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner);

    IpAddress associateIPToNetwork(long ipId, long networkId) throws InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException,
        ConcurrentOperationException;

    /**
     *
     * @param networkName
     * @param displayText
     * @param physicalNetworkId
     * @param broadcastUri TODO set the guru name based on the broadcastUri?
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
    Network createPrivateNetwork(String networkName, String displayText, long physicalNetworkId, String broadcastUri, String startIp, String endIP, String gateway,
        String netmask, long networkOwnerId, Long vpcId, Boolean sourceNat, Long networkOfferingId) throws ResourceAllocationException, ConcurrentOperationException,
        InsufficientCapacityException;

    /**
     * Requests an IP address for the guest NIC
     */
    NicSecondaryIp allocateSecondaryGuestIP(long nicId, IpAddresses requestedIpPair) throws InsufficientAddressCapacityException;

    boolean releaseSecondaryIpFromNic(long ipAddressId);

    /**
     * lists the NIC information
     */
    List<? extends Nic> listNics(ListNicsCmd listNicsCmd);

    Map<Network.Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service);

    IpAddress updateIP(Long id, String customId, Boolean displayIp);

    boolean configureNicSecondaryIp(NicSecondaryIp secIp, boolean isZoneSgEnabled);

    List<? extends NicSecondaryIp> listVmNicSecondaryIps(ListNicsCmd listNicsCmd);

    AcquirePodIpCmdResponse allocatePodIp(Account account, String zoneId, String podId) throws ResourceAllocationException, ConcurrentOperationException;

    boolean releasePodIp(ReleasePodIpCmdByAdmin ip) throws CloudRuntimeException;
}
