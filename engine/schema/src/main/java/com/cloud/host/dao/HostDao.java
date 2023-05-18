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

import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.resource.ResourceState;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

/**
 * Data Access Object for server
 *
 */
public interface HostDao extends GenericDao<HostVO, Long>, StateDao<Status, Status.Event, Host> {
    long countBy(long clusterId, ResourceState... states);

    Integer countAllByType(final Host.Type type);

    Integer countAllByTypeInZone(long zoneId, final Host.Type type);

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

    List<HostVO> listByHostTag(Host.Type type, Long clusterId, Long podId, long dcId, String hostTag);

    List<HostVO> findAndUpdateApplianceToLoad(long lastPingSecondsAfter, long managementServerId);

    boolean updateResourceState(ResourceState oldState, ResourceState.Event event, ResourceState newState, Host vo);

    HostVO findByGuid(String guid);

    HostVO findByTypeNameAndZoneId(long zoneId, String name, Host.Type type);

    List<HostVO> findHypervisorHostInCluster(long clusterId);

    HostVO findOldestExistentHypervisorHostInCluster(long clusterId);

    List<HostVO> listAllUpAndEnabledNonHAHosts(Type type, Long clusterId, Long podId, long dcId, String haTag);

    List<HostVO> findByDataCenterId(Long zoneId);

    List<HostVO> findByPodId(Long podId);

    List<HostVO> findByClusterId(Long clusterId);

    List<HostVO> findByClusterIdAndEncryptionSupport(Long clusterId);

    /**
     * Returns hosts that are 'Up' and 'Enabled' from the given Data Center/Zone
     */
    List<HostVO> listByDataCenterId(long id);

    /**
     * Returns hosts that are from the given Data Center/Zone and at a given state (e.g. Creating, Enabled, Disabled, etc).
     */
    List<HostVO> listByDataCenterIdAndState(long id, ResourceState state);

    /**
     * Returns hosts that are 'Up' and 'Disabled' from the given Data Center/Zone
     */
    List<HostVO> listDisabledByDataCenterId(long id);

    List<HostVO> listByDataCenterIdAndHypervisorType(long zoneId, Hypervisor.HypervisorType hypervisorType);

    List<Long> listAllHosts(long zoneId);

    List<HostVO> listAllHostsByZoneAndHypervisorType(long zoneId, HypervisorType hypervisorType);

    List<HostVO> listAllHostsByType(Host.Type type);

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

    List<HostVO> listByClusterAndHypervisorType(long clusterId, HypervisorType hypervisorType);

    HostVO findByName(String name);

    List<HostVO> listHostsWithActiveVMs(long offeringId);

    /**
     * Retrieves the number of hosts/agents this {@see ManagementServer} has responsibility over.
     * @param msid the id of the {@see ManagementServer}
     * @return the number of hosts/agents this {@see ManagementServer} has responsibility over
     */
    int countByMs(long msid);

    /**
     * Retrieves the hypervisor versions of the hosts in the datacenter which are in Up state in ascending order
     * @param datacenterId data center id
     * @param hypervisorType hypervisor type of the hosts
     * @return ordered list of hypervisor versions
     */
    List<String> listOrderedHostsHypervisorVersionsInDatacenter(long datacenterId, HypervisorType hypervisorType);
}
