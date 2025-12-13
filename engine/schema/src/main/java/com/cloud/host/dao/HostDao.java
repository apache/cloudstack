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
package com.cloud.host.dao;

import java.util.Date;
import java.util.List;

import com.cloud.cpu.CPU;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.resource.ResourceState;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

/**
 * Data Access Object for server
 *
 */
public interface HostDao extends GenericDao<HostVO, Long>, StateDao<Status, Status.Event, Host> {
    long countBy(long clusterId, ResourceState... states);

    Integer countAllByType(final Host.Type type);

    Integer countAllInClusterByTypeAndStates(Long clusterId, final Host.Type type, List<Status> status);

    Integer countAllByTypeInZone(long zoneId, final Host.Type type);

    Integer countUpAndEnabledHostsInZone(long zoneId);

    Pair<Integer, Integer> countAllHostsAndCPUSocketsByType(Type type);

    /**
     * Mark all hosts associated with a certain management server
     * as disconnected.
     *
     * @param msId management server id.
     */
    void markHostsAsDisconnected(long msId, long lastPing);

    List<HostVO> findLostHosts(long timeout);

    List<HostVO> findAndUpdateDirectAgentToLoad(long lastPingSecondsAfter, Long limit, long managementServerId);

    List<RunningHostCountInfo> getRunningHostCounts(Date cutTime);

    long getNextSequence(long hostId);

    void loadDetails(HostVO host);

    void saveDetails(HostVO host);

    void loadHostTags(HostVO host);

    List<HostVO> listByHostTag(Host.Type type, Long clusterId, Long podId, Long dcId, String hostTag);

    List<HostVO> findAndUpdateApplianceToLoad(long lastPingSecondsAfter, long managementServerId);

    boolean updateResourceState(ResourceState oldState, ResourceState.Event event, ResourceState newState, Host vo);

    HostVO findByGuid(String guid);

    HostVO findByTypeNameAndZoneId(long zoneId, String name, Host.Type type);

    List<HostVO> findHypervisorHostInCluster(long clusterId);

    List<HostVO> findHypervisorHostInPod(long podId);

    List<HostVO> findHypervisorHostInZone(long zoneId);

    HostVO findAnyStateHypervisorHostInCluster(long clusterId);

    HostVO findOldestExistentHypervisorHostInCluster(long clusterId);

    List<HostVO> listAllUpAndEnabledNonHAHosts(Type type, Long clusterId, Long podId, long dcId, String haTag);

    List<HostVO> findByDataCenterId(Long zoneId);

    List<Long> listIdsByDataCenterId(Long zoneId);

    List<HostVO> findByPodId(Long podId);

    List<HostVO> findByPodId(Long podId, Type type);

    List<Long> listIdsByPodId(Long podId);

    List<HostVO> findByClusterId(Long clusterId);

    List<HostVO> findByClusterId(Long clusterId, Type type);

    List<Long> listIdsByClusterId(Long clusterId);

    List<Long> listIdsForUpRouting(Long zoneId, Long podId, Long clusterId);

    List<Long> listIdsByType(Type type);

    List<Long> listIdsForUpEnabledByZoneAndHypervisor(Long zoneId, HypervisorType hypervisorType);

    List<HostVO> findByClusterIdAndEncryptionSupport(Long clusterId);

    /**
     * Returns host Ids that are 'Up' and 'Enabled' from the given Data Center/Zone
     */
    List<Long> listEnabledIdsByDataCenterId(long id);

    /**
     * Returns host Ids that are 'Up' and 'Disabled' from the given Data Center/Zone
     */
    List<Long> listDisabledIdsByDataCenterId(long id);

    List<HostVO> listByDataCenterIdAndHypervisorType(long zoneId, Hypervisor.HypervisorType hypervisorType);

    List<Long> listAllHosts(long zoneId);

    List<HostVO> listAllHostsByZoneAndHypervisorType(long zoneId, HypervisorType hypervisorType);

    List<HostVO> listAllHostsThatHaveNoRuleTag(Host.Type type, Long clusterId, Long podId, Long dcId);

    HostVO findByPublicIp(String publicIp);

    List<Long> listClustersByHostTag(String hostTagOnOffering);

    List<HostVO> listByType(Type type);

    /**
     * Finds a host by ip address, excludes removed hosts.
     *
     * @param ip The ip address to match on
     * @return One matched host
     */
    HostVO findByIp(String ip);

    /**
     * This method will look for a host that is of the same hypervisor and zone as indicated in its parameters.
     * <ul>
     * <li>We give priority to 'Enabled' hosts, but if no 'Enabled' hosts are found, we use 'Disabled' hosts
     * <li>If no host is found, we throw a runtime exception
     * </ul>
     *
     * Side note: this method is currently only used in XenServerGuru; therefore, it was designed to meet XenServer deployment scenarios requirements.
     */
    HostVO findHostInZoneToExecuteCommand(long zoneId, HypervisorType hypervisorType);

    List<HostVO> listAllHostsUpByZoneAndHypervisor(long zoneId, HypervisorType hypervisorType);

    List<HostVO> listByHostCapability(Host.Type type, Long clusterId, Long podId, long dcId, String hostCapabilty);

    List<HostVO> listByClusterHypervisorTypeAndHostCapability(Long clusterId, HypervisorType hypervisorType, String hostCapabilty);

    List<HostVO> listByClusterAndHypervisorType(long clusterId, HypervisorType hypervisorType);

    HostVO findByName(String name);

    HostVO findHostByHypervisorTypeAndVersion(HypervisorType hypervisorType, String hypervisorVersion);

    List<HostVO> listHostsWithActiveVMs(long offeringId);

    List<HostVO> listHostsByMsAndDc(long msId, long dcId);

    List<HostVO> listHostsByMsDcResourceState(long msId, long dcId, List<ResourceState> excludedResourceStates);

    List<HostVO> listHostsByMs(long msId);

    List<HostVO> listHostsByMsResourceState(long msId, List<ResourceState> excludedResourceStates);

    /**
     * Count Hosts by given Management Server, Host and Hypervisor Types,
     * and exclude Hosts with given Resource States.
     *
     * @param msId                   Management Server Id
     * @param excludedResourceStates Resource States to be excluded
     * @param hostTypes              Host Types
     * @param hypervisorTypes        Hypervisor Types
     * @return Hosts count
     */
    int countHostsByMsResourceStateTypeAndHypervisorType(long msId, List<ResourceState> excludedResourceStates,
                                                         List<Type> hostTypes, List<HypervisorType> hypervisorTypes);

    /**
     * Retrieves the host ids/agents this {@see ManagementServer} has responsibility over.
     * @param msId the id of the {@see ManagementServer}
     * @return the host ids/agents this {@see ManagementServer} has responsibility over
     */
    List<String> listByMs(long msId);

    /**
     * Retrieves the last host ids/agents this {@see ManagementServer} has responsibility over.
     * @param msId the id of the {@see ManagementServer}
     * @return the last host ids/agents this {@see ManagementServer} has responsibility over
     */
    List<String> listByLastMs(long msId);

    /**
     * Retrieves the hypervisor versions of the hosts in the datacenter which are in Up state in ascending order
     * @param datacenterId data center id
     * @param hypervisorType hypervisor type of the hosts
     * @return ordered list of hypervisor versions
     */
    List<String> listOrderedHostsHypervisorVersionsInDatacenter(long datacenterId, HypervisorType hypervisorType);

    List<HostVO> findHostsWithTagRuleThatMatchComputeOferringTags(String computeOfferingTags);

    List<Long> findClustersThatMatchHostTagRule(String computeOfferingTags);

    List<Long> listSsvmHostsWithPendingMigrateJobsOrderedByJobCount();

    boolean isHostUp(long hostId);

    List<Long> findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(final Long zoneId, final Long clusterId,
            final Long msId, final List<ResourceState> resourceStates, final List<Type> types,
            final List<Hypervisor.HypervisorType> hypervisorTypes);

    List<HypervisorType> listDistinctHypervisorTypes(final Long zoneId);

    List<Pair<HypervisorType, CPU.CPUArch>> listDistinctHypervisorArchTypes(final Long zoneId);

    List<CPU.CPUArch> listDistinctArchTypes(final Long clusterId);

    List<HostVO> listByIds(final List<Long> ids);

    Long findClusterIdByVolumeInfo(VolumeInfo volumeInfo);

    List<String> listDistinctStorageAccessGroups(String name, String keyword);
}
