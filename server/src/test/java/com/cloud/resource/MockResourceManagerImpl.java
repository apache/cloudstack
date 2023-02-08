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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.DeleteClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.UpdateClusterCmd;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.api.command.admin.host.AddSecondaryStorageCmd;
import org.apache.cloudstack.api.command.admin.host.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.ReconnectHostCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostPasswordCmd;
import org.apache.cloudstack.api.command.admin.host.CancelHostAsDegradedCmd;
import org.apache.cloudstack.api.command.admin.host.DeclareHostAsDegradedCmd;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.gpu.HostGpuGroupsVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceState.Event;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.fsm.NoTransitionException;

public class MockResourceManagerImpl extends ManagerBase implements ResourceManager {

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateHost(com.cloud.api.commands.UpdateHostCmd)
     */
    @Override
    public Host updateHost(final UpdateHostCmd cmd) throws NoTransitionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Host autoUpdateHostAllocationState(Long hostId, ResourceState.Event resourceEvent) throws NoTransitionException {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#cancelMaintenance(com.cloud.api.commands.CancelMaintenanceCmd)
     */
    @Override
    public Host cancelMaintenance(final CancelMaintenanceCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#reconnectHost(com.cloud.api.commands.ReconnectHostCmd)
     */
    @Override
    public Host reconnectHost(final ReconnectHostCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverCluster(com.cloud.api.commands.AddClusterCmd)
     */
    @Override
    public List<? extends Cluster> discoverCluster(final AddClusterCmd cmd) throws IllegalArgumentException, DiscoveryException, ResourceInUseException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#deleteCluster(com.cloud.api.commands.DeleteClusterCmd)
     */
    @Override
    public boolean deleteCluster(final DeleteClusterCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateCluster(com.cloud.org.Cluster, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Cluster updateCluster(UpdateClusterCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverHosts(com.cloud.api.commands.AddHostCmd)
     */
    @Override
    public List<? extends Host> discoverHosts(final AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#discoverHosts(com.cloud.api.commands.AddSecondaryStorageCmd)
     */
    @Override
    public List<? extends Host> discoverHosts(final AddSecondaryStorageCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#maintain(com.cloud.api.commands.PrepareForMaintenanceCmd)
     */
    @Override
    public Host maintain(final PrepareForMaintenanceCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Host declareHostAsDegraded(DeclareHostAsDegradedCmd cmd) {
        return null;
    }

    @Override
    public Host cancelHostAsDegraded(final CancelHostAsDegradedCmd cmd) {
        return null;
    }

    @Override
    public boolean updateClusterPassword(final UpdateHostPasswordCmd upasscmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#updateHostPassword(com.cloud.api.commands.UpdateHostPasswordCmd)
     */
    @Override
    public boolean updateHostPassword(final UpdateHostPasswordCmd upasscmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getHost(long)
     */
    @Override
    public Host getHost(final long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getCluster(java.lang.Long)
     */
    @Override
    public Cluster getCluster(final Long clusterId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataCenter getZone(Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceService#getSupportedHypervisorTypes(long, boolean, java.lang.Long)
     */
    @Override
    public List<HypervisorType> getSupportedHypervisorTypes(final long zoneId, final boolean forVirtualRouter, final Long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#registerResourceEvent(java.lang.Integer, com.cloud.resource.ResourceListener)
     */
    @Override
    public void registerResourceEvent(final Integer event, final ResourceListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#unregisterResourceEvent(com.cloud.resource.ResourceListener)
     */
    @Override
    public void unregisterResourceEvent(final ResourceListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#registerResourceStateAdapter(java.lang.String, com.cloud.resource.ResourceStateAdapter)
     */
    @Override
    public void registerResourceStateAdapter(final String name, final ResourceStateAdapter adapter) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#unregisterResourceStateAdapter(java.lang.String)
     */
    @Override
    public void unregisterResourceStateAdapter(final String name) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#createHostAndAgent(java.lang.Long, com.cloud.resource.ServerResource, java.util.Map, boolean, java.util.List, boolean)
     */
    @Override
    public Host createHostAndAgent(final Long hostId, final ServerResource resource, final Map<String, String> details, final boolean old, final List<String> hostTags, final boolean forRebalance) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#addHost(long, com.cloud.resource.ServerResource, com.cloud.host.Host.Type, java.util.Map)
     */
    @Override
    public Host addHost(final long zoneId, final ServerResource resource, final Type hostType, final Map<String, String> hostDetails) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#createHostVOForConnectedAgent(com.cloud.agent.api.StartupCommand[])
     */
    @Override
    public HostVO createHostVOForConnectedAgent(final StartupCommand[] cmds) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#checkCIDR(com.cloud.dc.HostPodVO, com.cloud.dc.DataCenterVO, java.lang.String, java.lang.String)
     */
    @Override
    public void checkCIDR(final HostPodVO pod, final DataCenterVO dc, final String serverPrivateIP, final String serverPrivateNetmask) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#fillRoutingHostVO(com.cloud.host.HostVO, com.cloud.agent.api.StartupRoutingCommand, com.cloud.hypervisor.Hypervisor.HypervisorType, java.util.Map, java.util.List)
     */
    @Override
    public HostVO fillRoutingHostVO(final HostVO host, final StartupRoutingCommand ssCmd, final HypervisorType hyType, final Map<String, String> details, final List<String> hostTags) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#deleteRoutingHost(com.cloud.host.HostVO, boolean, boolean)
     */
    @Override
    public void deleteRoutingHost(final HostVO host, final boolean isForced, final boolean forceDestroyStorage) throws UnableDeleteHostException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#executeUserRequest(long, com.cloud.resource.ResourceState.Event)
     */
    @Override
    public boolean executeUserRequest(final long hostId, final Event event) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#resourceStateTransitTo(com.cloud.host.Host, com.cloud.resource.ResourceState.Event, long)
     */
    @Override
    public boolean resourceStateTransitTo(final Host host, final Event event, final long msId) throws NoTransitionException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#umanageHost(long)
     */
    @Override
    public boolean umanageHost(final long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#migrateAwayFailed(long)
     */
    @Override
    public boolean migrateAwayFailed(final long hostId, final long vmId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#maintain(long)
     */
    @Override
    public boolean maintain(final long hostId) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#maintain(long)
     */
    @Override
    public boolean checkAndMaintain(final long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#deleteHost(long, boolean, boolean)
     */
    @Override
    public boolean deleteHost(final long hostId, final boolean isForced, final boolean isForceDeleteStorage) {
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
    public List<HostVO> listAllUpAndEnabledHosts(final Type type, final Long clusterId, final Long podId, final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HostVO> listAllHosts(final Type type, final Long clusterId, final Long podId, final long dcId) {
        return null;
    }

    @Override
    public List<HostVO> listAllUpHosts(Type type, Long clusterId, Long podId, long dcId) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInCluster(long)
     */
    @Override
    public List<HostVO> listAllHostsInCluster(final long clusterId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listHostsInClusterByStatus(long, com.cloud.host.Status)
     */
    @Override
    public List<HostVO> listHostsInClusterByStatus(final long clusterId, final Status status) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllUpAndEnabledHostsInOneZoneByType(com.cloud.host.Host.Type, long)
     */
    @Override
    public List<HostVO> listAllUpAndEnabledHostsInOneZoneByType(final Type type, final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInOneZoneByType(com.cloud.host.Host.Type, long)
     */
    @Override
    public List<HostVO> listAllHostsInOneZoneByType(final Type type, final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllHostsInAllZonesByType(com.cloud.host.Host.Type)
     */
    @Override
    public List<HostVO> listAllHostsInAllZonesByType(final Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAvailHypervisorInZone(java.lang.Long, java.lang.Long)
     */
    @Override
    public List<HypervisorType> listAvailHypervisorInZone(final Long hostId, final Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByGuid(java.lang.String)
     */
    @Override
    public HostVO findHostByGuid(final String guid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByName(java.lang.String)
     */
    @Override
    public HostVO findHostByName(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getHostStatistics(long)
     */
    @Override
    public HostStats getHostStatistics(final long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getGuestOSCategoryId(long)
     */
    @Override
    public Long getGuestOSCategoryId(final long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getHostTags(long)
     */
    @Override
    public String getHostTags(final long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listByDataCenter(long)
     */
    @Override
    public List<PodCluster> listByDataCenter(final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllNotInMaintenanceHostsInOneZone(com.cloud.host.Host.Type, java.lang.Long)
     */
    @Override
    public List<HostVO> listAllNotInMaintenanceHostsInOneZone(final Type type, final Long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getDefaultHypervisor(long)
     */
    @Override
    public HypervisorType getDefaultHypervisor(final long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getAvailableHypervisor(long)
     */
    @Override
    public HypervisorType getAvailableHypervisor(final long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#getMatchingDiscover(com.cloud.hypervisor.Hypervisor.HypervisorType)
     */
    @Override
    public Discoverer getMatchingDiscover(final HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#findHostByGuid(long, java.lang.String)
     */
    @Override
    public List<HostVO> findHostByGuid(final long dcId, final String guid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.resource.ResourceManager#listAllUpAndEnabledNonHAHosts(com.cloud.host.Host.Type, java.lang.Long, java.lang.Long, long)
     */
    @Override
    public List<HostVO> listAllUpAndEnabledNonHAHosts(final Type type, final Long clusterId, final Long podId, final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
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
    public List<HostVO> listAllUpAndEnabledHostsInOneZoneByHypervisor(final HypervisorType type, final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HostVO> listAllUpAndEnabledHostsInOneZone(final long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseHostReservation(final Long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isGPUDeviceAvailable(final long hostId, final String groupName, final String vgpuType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public GPUDeviceTO getGPUDevice(final long hostId, final String groupName, final String vgpuType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HostGpuGroupsVO> listAvailableGPUDevice(final long hostId, final String groupName, final String vgpuType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateGPUDetails(final long hostId, final HashMap<String, HashMap<String, VgpuTypesInfo>> deviceDetails) {
        // TODO Auto-generated method stub
    }

    @Override
    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGPUStatistics(final HostVO host) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO findOneRandomRunningHostByHypervisor(HypervisorType type, Long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean cancelMaintenance(long hostId) {
        return false;
    }

    @Override
    public boolean isHostGpuEnabled(final long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getConfigComponentName() {
        return null;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }
}
