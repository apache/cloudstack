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
package com.cloud.configuration;

import java.util.Map;
import java.util.Set;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.user.Account;

/**
 * ConfigurationManager handles adding pods/zones, changing IP ranges, enabling external firewalls, and editing
 * configuration values
 *
 */
public interface ConfigurationManager {
    /**
     * @param offering
     * @return
     */
    boolean isOfferingForVpc(NetworkOffering offering);

    Integer getNetworkOfferingNetworkRate(long networkOfferingId, Long dataCenterId);

    Integer getServiceOfferingNetworkRate(long serviceOfferingId, Long dataCenterId);

    /**
     * Updates a configuration entry with a new value
     *
     * @param userId
     * @param name
     * @param value
     */
    String updateConfiguration(long userId, String name, String category, String value, String scope, Long id);

//    /**
//     * Creates a new service offering
//     *
//     * @param name
//     * @param cpu
//     * @param ramSize
//     * @param speed
//     * @param displayText
//     * @param localStorageRequired
//     * @param offerHA
//     * @param domainId
//     * @param volatileVm
//     * @param hostTag
//     * @param networkRate
//     *            TODO
//     * @param id
//     * @param useVirtualNetwork
//     * @param deploymentPlanner
//     * @param details
//     * @param bytesReadRate
//     * @param bytesWriteRate
//     * @param iopsReadRate
//     * @param iopsWriteRate
//     * @return ID
//     */
//    ServiceOfferingVO createServiceOffering(long userId, boolean isSystem, VirtualMachine.Type vm_typeType, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired,
//            boolean offerHA, boolean limitResourceUse, boolean volatileVm, String tags, Long domainId, String hostTag, Integer networkRate, String deploymentPlanner, Map<String, String> details,
//            Long bytesReadRate, Long bytesWriteRate, Long iopsReadRate, Long iopsWriteRate);

//    /**
//     * Creates a new disk offering
//     *
//     * @param domainId
//     * @param name
//     * @param description
//     * @param numGibibytes
//     * @param tags
//     * @param isCustomized
//     * @param localStorageRequired
//     * @param isDisplayOfferingEnabled
//     * @param isCustomizedIops (is admin allowing users to set custom iops?)
//     * @param minIops
//     * @param maxIops
//     * @param bytesReadRate
//     * @param bytesWriteRate
//     * @param iopsReadRate
//     * @param iopsWriteRate
//     * @return newly created disk offering
//     */
//    DiskOfferingVO createDiskOffering(Long domainId, String name, String description, Long numGibibytes, String tags, boolean isCustomized,
//            boolean localStorageRequired, boolean isDisplayOfferingEnabled, Boolean isCustomizedIops, Long minIops, Long maxIops,
//            Long bytesReadRate, Long bytesWriteRate, Long iopsReadRate, Long iopsWriteRate);

    /**
     * Creates a new pod
     *
     * @param userId
     * @param podName
     * @param zoneId
     * @param gateway
     * @param cidr
     * @param startIp
     * @param endIp
     * @param allocationState
     * @param skipGatewayOverlapCheck
     *            (true if it is ok to not validate that gateway IP address overlap with Start/End IP of the POD)
     * @return Pod
     */
    HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, String allocationState,
        boolean skipGatewayOverlapCheck);

    /**
     * Creates a new zone
     *
     * @param userId
     * @param zoneName
     * @param dns1
     * @param dns2
     * @param internalDns1
     * @param internalDns2
     * @param guestCidr
     * @param zoneType
     * @param allocationState
     * @param networkDomain
     *            TODO
     * @param isSecurityGroupEnabled
     *            TODO
     * @param ip6Dns1 TODO
     * @param ip6Dns2 TODO
     * @return
     * @throws
     * @throws
     */
    DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String guestCidr, String domain,
        Long domainId, NetworkType zoneType, String allocationState, String networkDomain, boolean isSecurityGroupEnabled, boolean isLocalStorageEnabled, String ip6Dns1,
        String ip6Dns2);

    /**
     * Deletes a VLAN from the database, along with all of its IP addresses. Will not delete VLANs that have allocated
     * IP addresses.
     *
     * @param userId
     * @param vlanDbId
     * @param caller TODO
     * @return success/failure
     */
    boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId, Account caller);

    void checkZoneAccess(Account caller, DataCenter zone);

    void checkDiskOfferingAccess(Account caller, DiskOffering dof);

    /**
     * Creates a new network offering
     * @param name
     * @param displayText
     * @param trafficType
     * @param tags
     * @param specifyVlan
     * @param networkRate
     *            TODO
     * @param serviceProviderMap
     *            TODO
     * @param isDefault
     *            TODO
     * @param type
     *            TODO
     * @param systemOnly
     *            TODO
     * @param serviceOfferingId
     * @param conserveMode
     *            ;
     * @param specifyIpRanges
     *            TODO
     * @param isPersistent
     *            ;
     * @param details TODO
     * @param id
     * @return network offering object
     */

    NetworkOfferingVO createNetworkOffering(String name, String displayText, TrafficType trafficType, String tags, boolean specifyVlan, Availability availability,
        Integer networkRate, Map<Service, Set<Provider>> serviceProviderMap, boolean isDefault, Network.GuestType type, boolean systemOnly, Long serviceOfferingId,
        boolean conserveMode, Map<Service, Map<Capability, String>> serviceCapabilityMap, boolean specifyIpRanges, boolean isPersistent,
        Map<NetworkOffering.Detail, String> details, boolean egressDefaultPolicy, Integer maxconn, boolean enableKeepAlive);

    Vlan createVlanAndPublicIpRange(long zoneId, long networkId, long physicalNetworkId, boolean forVirtualNetwork, Long podId, String startIP, String endIP,
        String vlanGateway, String vlanNetmask, String vlanId, Domain domain, Account vlanOwner, String startIPv6, String endIPv6, String vlanIp6Gateway, String vlanIp6Cidr)
        throws InsufficientCapacityException, ConcurrentOperationException, InvalidParameterValueException;

    void createDefaultSystemNetworks(long zoneId) throws ConcurrentOperationException;

    boolean releaseAccountSpecificVirtualRanges(long accountId);

    /**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     *
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

    void checkPodCidrSubnets(long zoneId, Long podIdToBeSkipped, String cidr);

    AllocationState findPodAllocationState(HostPodVO pod);

    AllocationState findClusterAllocationState(ClusterVO cluster);
}
