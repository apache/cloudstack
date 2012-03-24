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
package com.cloud.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceState.Event;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * ResourceManager manages how physical resources are organized within the
 * CloudStack. It also manages the life cycle of the physical resources.
 */
public interface ResourceManager extends ResourceService{
    /**
     * Register a listener for different types of resource life cycle events.
     * There can only be one type of listener per type of host.
     * 
     * @param Event type see ResourceListener.java, allow combination of multiple events.
     * @param listener the listener to notify.
     */
    public void registerResourceEvent(Integer event, ResourceListener listener);
    
    public void unregisterResourceEvent(ResourceListener listener);
    
    /**
     * 
     * @param name of adapter
     * @param adapter
     * @param hates, a list of names which will be eliminated by this adapter. Especially for the case where 
     * can be only one adapter responds to an event, e.g. startupCommand
     */
    public void registerResourceStateAdapter(String name, ResourceStateAdapter adapter);
    
    public void unregisterResourceStateAdapter(String name);
    
	public Host createHostAndAgent(Long hostId, ServerResource resource, Map<String, String> details, boolean old, List<String> hostTags,
	        boolean forRebalance);
	
	public Host addHost(long zoneId, ServerResource resource, Type hostType, Map<String, String> hostDetails);
	
	public HostVO createHostVOForConnectedAgent(StartupCommand[] cmds);
	
	public void checkCIDR(HostPodVO pod, DataCenterVO dc, String serverPrivateIP, String serverPrivateNetmask);
	
	public HostVO fillRoutingHostVO(HostVO host, StartupRoutingCommand ssCmd, HypervisorType hyType, Map<String, String> details, List<String> hostTags);
	
	public void deleteRoutingHost(HostVO host, boolean isForced, boolean forceDestroyStorage) throws UnableDeleteHostException;
	
    public boolean executeUserRequest(long hostId, ResourceState.Event event) throws AgentUnavailableException;

	boolean resourceStateTransitTo(Host host, Event event, long msId) throws NoTransitionException;

	boolean umanageHost(long hostId);

	boolean maintenanceFailed(long hostId);
	
	public boolean maintain(final long hostId) throws AgentUnavailableException;
	
    @Override
    public boolean deleteHost(long hostId, boolean isForced, boolean isForceDeleteStorage);
    
    public List<HostVO> findDirectlyConnectedHosts();
    
    public List<HostVO> listAllUpAndEnabledHosts(Host.Type type, Long clusterId, Long podId, long dcId);
    
    public List<HostVO> listAllHostsInCluster(long clusterId);
    
    public List<HostVO> listHostsInClusterByStatus(long clusterId, Status status);
    
    public List<HostVO> listAllUpAndEnabledHostsInOneZoneByType(Host.Type type, long dcId);
    
    public List<HostVO> listAllHostsInOneZoneByType(Host.Type type, long dcId);
    
    public List<HostVO> listAllHostsInAllZonesByType(Type type);
    
    public List<HypervisorType> listAvailHypervisorInZone(Long hostId, Long zoneId);
    
    public HostVO findHostByGuid(String guid);
    
    public HostVO findHostByName(String name);
    
    public List<HostVO> listHostsByNameLike(String name);
    
    /**
     * Find a pod based on the user id, template, and data center.
     * 
     * @param template
     * @param dc
     * @param userId
     * @return
     */
    Pair<HostPodVO, Long> findPod(VirtualMachineTemplate template, ServiceOfferingVO offering, DataCenterVO dc, long accountId, Set<Long> avoids);
    
    HostStats getHostStatistics(long hostId);
    
    Long getGuestOSCategoryId(long hostId);
    
    String getHostTags(long hostId);
    
    List<PodCluster> listByDataCenter(long dcId);

	List<HostVO> listAllNotInMaintenanceHostsInOneZone(Type type, Long dcId);

	HypervisorType getDefaultHypervisor(long zoneId);

	HypervisorType getAvailableHypervisor(long zoneId);

    Discoverer getMatchingDiscover(HypervisorType hypervisorType);

	List<HostVO> findHostByGuid(long dcId, String guid);
}
