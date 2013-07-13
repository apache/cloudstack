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

package com.cloud.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.DeleteClusterCmd;
import org.apache.cloudstack.api.command.admin.host.*;
import org.apache.cloudstack.api.command.admin.storage.*;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodCluster;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceState.Event;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.ImageStore;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.fsm.NoTransitionException;

@Local(value = {ResourceManager.class})
public class MockResourceManagerImpl extends ManagerBase implements ResourceManager {

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateHost(com.cloud.api.commands.UpdateHostCmd)
     */
    @Override
    public Host updateHost(UpdateHostCmd cmd) throws NoTransitionException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#cancelMaintenance(com.cloud.api.commands.CancelMaintenanceCmd)
     */
    @Override
    public Host cancelMaintenance(CancelMaintenanceCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#reconnectHost(com.cloud.api.commands.ReconnectHostCmd)
     */
    @Override
    public Host reconnectHost(ReconnectHostCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverCluster(com.cloud.api.commands.AddClusterCmd)
     */
    @Override
    public List<? extends Cluster> discoverCluster(AddClusterCmd cmd) throws IllegalArgumentException,
            DiscoveryException, ResourceInUseException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#deleteCluster(com.cloud.api.commands.DeleteClusterCmd)
     */
    @Override
    public boolean deleteCluster(DeleteClusterCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateCluster(com.cloud.org.Cluster, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Cluster updateCluster(Cluster cluster, String clusterType, String hypervisor, String allocationState,
                                 String managedstate, Float memoryOvercommitRaito, Float cpuOvercommitRatio) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverHosts(com.cloud.api.commands.AddHostCmd)
     */
    @Override
    public List<? extends Host> discoverHosts(AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException,
            InvalidParameterValueException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverHosts(com.cloud.api.commands.AddSecondaryStorageCmd)
     */
    @Override
    public List<? extends Host> discoverHosts(AddSecondaryStorageCmd cmd) throws IllegalArgumentException,
            DiscoveryException, InvalidParameterValueException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#maintain(com.cloud.api.commands.PrepareForMaintenanceCmd)
     */
    @Override
    public Host maintain(PrepareForMaintenanceCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateHostPassword(com.cloud.api.commands.UpdateHostPasswordCmd)
     */
    @Override
    public boolean updateHostPassword(UpdateHostPasswordCmd upasscmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getHost(long)
     */
    @Override
    public Host getHost(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getCluster(java.lang.Long)
     */
    @Override
    public Cluster getCluster(Long clusterId) {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getSupportedHypervisorTypes(long, boolean, java.lang.Long)
     */
    @Override
    public List<HypervisorType> getSupportedHypervisorTypes(long zoneId, boolean forVirtualRouter, Long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#registerResourceEvent(java.lang.Integer, com.cloud.resource.ResourceListener)
     */
    @Override
    public void registerResourceEvent(Integer event, ResourceListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#unregisterResourceEvent(com.cloud.resource.ResourceListener)
     */
    @Override
    public void unregisterResourceEvent(ResourceListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#registerResourceStateAdapter(java.lang.String, com.cloud.resource.ResourceStateAdapter)
     */
    @Override
    public void registerResourceStateAdapter(String name, ResourceStateAdapter adapter) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#unregisterResourceStateAdapter(java.lang.String)
     */
    @Override
    public void unregisterResourceStateAdapter(String name) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#createHostAndAgent(java.lang.Long, com.cloud.resource.ServerResource, java.util.Map, boolean, java.util.List, boolean)
     */
    @Override
    public Host createHostAndAgent(Long hostId, ServerResource resource, Map<String, String> details, boolean old,
            List<String> hostTags, boolean forRebalance) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#addHost(long, com.cloud.resource.ServerResource, com.cloud.host.Host.Type, java.util.Map)
     */
    @Override
    public Host addHost(long zoneId, ServerResource resource, Type hostType, Map<String, String> hostDetails) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#createHostVOForConnectedAgent(com.cloud.agent.api.StartupCommand[])
     */
    @Override
    public HostVO createHostVOForConnectedAgent(StartupCommand[] cmds) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#checkCIDR(com.cloud.dc.HostPodVO, com.cloud.dc.DataCenterVO, java.lang.String, java.lang.String)
     */
    @Override
    public void checkCIDR(HostPodVO pod, DataCenterVO dc, String serverPrivateIP, String serverPrivateNetmask) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#fillRoutingHostVO(com.cloud.host.HostVO, com.cloud.agent.api.StartupRoutingCommand, com.cloud.hypervisor.Hypervisor.HypervisorType, java.util.Map, java.util.List)
     */
    @Override
    public HostVO fillRoutingHostVO(HostVO host, StartupRoutingCommand ssCmd, HypervisorType hyType,
            Map<String, String> details, List<String> hostTags) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#deleteRoutingHost(com.cloud.host.HostVO, boolean, boolean)
     */
    @Override
    public void deleteRoutingHost(HostVO host, boolean isForced, boolean forceDestroyStorage)
            throws UnableDeleteHostException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#executeUserRequest(long, com.cloud.resource.ResourceState.Event)
     */
    @Override
    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#resourceStateTransitTo(com.cloud.host.Host, com.cloud.resource.ResourceState.Event, long)
     */
    @Override
    public boolean resourceStateTransitTo(Host host, Event event, long msId) throws NoTransitionException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#umanageHost(long)
     */
    @Override
    public boolean umanageHost(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#maintenanceFailed(long)
     */
    @Override
    public boolean maintenanceFailed(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#maintain(long)
     */
    @Override
    public boolean maintain(long hostId) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#deleteHost(long, boolean, boolean)
     */
    @Override
    public boolean deleteHost(long hostId, boolean isForced, boolean isForceDeleteStorage) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findDirectlyConnectedHosts()
     */
    @Override
    public List<HostVO> findDirectlyConnectedHosts() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllUpAndEnabledHosts(com.cloud.host.Host.Type, java.lang.Long, java.lang.Long, long)
     */
    @Override
    public List<HostVO> listAllUpAndEnabledHosts(Type type, Long clusterId, Long podId, long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInCluster(long)
     */
    @Override
    public List<HostVO> listAllHostsInCluster(long clusterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listHostsInClusterByStatus(long, com.cloud.host.Status)
     */
    @Override
    public List<HostVO> listHostsInClusterByStatus(long clusterId, Status status) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllUpAndEnabledHostsInOneZoneByType(com.cloud.host.Host.Type, long)
     */
    @Override
    public List<HostVO> listAllUpAndEnabledHostsInOneZoneByType(Type type, long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInOneZoneByType(com.cloud.host.Host.Type, long)
     */
    @Override
    public List<HostVO> listAllHostsInOneZoneByType(Type type, long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInAllZonesByType(com.cloud.host.Host.Type)
     */
    @Override
    public List<HostVO> listAllHostsInAllZonesByType(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAvailHypervisorInZone(java.lang.Long, java.lang.Long)
     */
    @Override
    public List<HypervisorType> listAvailHypervisorInZone(Long hostId, Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByGuid(java.lang.String)
     */
    @Override
    public HostVO findHostByGuid(String guid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByName(java.lang.String)
     */
    @Override
    public HostVO findHostByName(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listHostsByNameLike(java.lang.String)
     */
    @Override
    public List<HostVO> listHostsByNameLike(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findPod(com.cloud.template.VirtualMachineTemplate, com.cloud.service.ServiceOfferingVO, com.cloud.dc.DataCenterVO, long, java.util.Set)
     */
    @Override
    public Pair<Pod, Long> findPod(VirtualMachineTemplate template, ServiceOfferingVO offering, DataCenterVO dc,
            long accountId, Set<Long> avoids) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getHostStatistics(long)
     */
    @Override
    public HostStats getHostStatistics(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getGuestOSCategoryId(long)
     */
    @Override
    public Long getGuestOSCategoryId(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getHostTags(long)
     */
    @Override
    public String getHostTags(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listByDataCenter(long)
     */
    @Override
    public List<PodCluster> listByDataCenter(long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllNotInMaintenanceHostsInOneZone(com.cloud.host.Host.Type, java.lang.Long)
     */
    @Override
    public List<HostVO> listAllNotInMaintenanceHostsInOneZone(Type type, Long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getDefaultHypervisor(long)
     */
    @Override
    public HypervisorType getDefaultHypervisor(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getAvailableHypervisor(long)
     */
    @Override
    public HypervisorType getAvailableHypervisor(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getMatchingDiscover(com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public Discoverer getMatchingDiscover(HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByGuid(long, java.lang.String)
     */
    @Override
    public List<HostVO> findHostByGuid(long dcId, String guid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllUpAndEnabledNonHAHosts(com.cloud.host.Host.Type, java.lang.Long, java.lang.Long, long)
     */
    @Override
    public List<HostVO> listAllUpAndEnabledNonHAHosts(Type type, Long clusterId, Long podId, long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "MockResourceManagerImpl";
    }

	@Override
	public List<HostVO> listAllUpAndEnabledHostsInOneZoneByHypervisor(
			HypervisorType type, long dcId) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public boolean releaseHostReservation(Long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

}
