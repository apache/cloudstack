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
package com.cloud.host.dao;

import java.util.Date;
import java.util.List;

import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.utils.db.GenericDao;

/**
 * Data Access Object for server
 *
 */
public interface HostDao extends GenericDao<HostVO, Long> {
    List<HostVO> listBy(Host.Type type, Long clusterId, Long podId, long dcId);

    long countBy(long clusterId,  Status... statuses);

    List<HostVO> listByDataCenter(long dcId);
	List<HostVO> listByHostPod(long podId);
	List<HostVO> listByStatus(Status... status);
	List<HostVO> listBy(Host.Type type, long dcId);
	List<HostVO> listAllBy(Host.Type type, long dcId);
	HostVO findSecondaryStorageHost(long dcId);
	List<HostVO> listByCluster(long clusterId);
	/**
	 * Lists all secondary storage hosts, across all zones
	 * @return list of Hosts
	 */
	List<HostVO> listSecondaryStorageHosts();

    /**
     * Mark all hosts associated with a certain management server
     * as disconnected.
     * 
     * @param msId management server id.
     */
    void markHostsAsDisconnected(long msId, long lastPing);

	List<HostVO> findLostHosts(long timeout);

	List<HostVO> findHostsLike(String hostName);

	/**
	 * Find hosts that are directly connected.
	 */
	List<HostVO> findDirectlyConnectedHosts();

    List<HostVO> findAndUpdateDirectAgentToLoad(long lastPingSecondsAfter, Long limit, long managementServerId);
	/**
	 * Mark the host as disconnected if it is in one of these states.
	 * The management server id is set to null.
	 * The lastPinged timestamp is set to current.
	 * The state is set to the state passed in.
	 * The disconnectedOn timestamp is set to current.
	 *
	 * @param host host to be marked
	 * @param state state to be set to.
	 * @param ifStates only if it is one of these states.
	 * @return true if it's updated; false if not.
	 */
	boolean disconnect(HostVO host, Event event, long msId);

	boolean connect(HostVO host, long msId);

	HostVO findByStorageIpAddressInDataCenter(long dcId, String privateIpAddress);
    HostVO findByPrivateIpAddressInDataCenter(long dcId, String privateIpAddress);

	public HostVO findByGuid(String guid);

	public HostVO findByName(String name);


	/**
	 * find all hosts of a certain type in a data center
	 * @param type
	 * @param routingCapable
	 * @param dcId
	 * @return
	 */
	List<HostVO> listByTypeDataCenter(Host.Type type, long dcId);

	/**
	 * find all hosts of a particular type
	 * @param type
	 * @return
	 */
	List<HostVO> listByType(Type type);

	/**
	 * update the host and changes the status depending on the Event and
	 * the current status.  If the status changed between
	 * @param host host object to change
	 * @param event event that happened.
	 * @param management server who's making this update
	 * @return true if updated; false if not.
	 */
	boolean updateStatus(HostVO host, Event event, long msId);

    List<RunningHostCountInfo> getRunningHostCounts(Date cutTime);

    long getNextSequence(long hostId);

    void loadDetails(HostVO host);

    void saveDetails(HostVO host);

	HostVO findConsoleProxyHost(String name, Type type);

    List<HypervisorType> getAvailHypervisorInZone(Long hostId, Long zoneId);

    /**
     * Returns a list of host ids given the conditions.
     * @param dataCenterId if specified, then must be in this data center.
     * @param podId if specified, then must be in this pod.
     * @param clusterId if specified, then must be in this cluster.
     * @param hostType TODO
     * @param statuses the host needs to be in.
     * @return ids of the host meeting the search parameters.
     */
    List<Long> listBy(Long dataCenterId, Long podId, Long clusterId, Type hostType, Status... statuses);

    List<HostVO> listBy(Long clusterId, Long podId, long dcId);

    void loadHostTags(HostVO host);

    List<HostVO> listByHostTag(Host.Type type, Long clusterId, Long podId, long dcId, String hostTag);

    long countRoutingHostsByDataCenter(long dcId);

    List<HostVO> listSecondaryStorageHosts(long dataCenterId);

    boolean directConnect(HostVO host, long msId);
    
    List<HostVO> listDirectHostsBy(long msId, Status status);
    
    List<HostVO> listManagedDirectAgents();
    
    List<HostVO> listManagedRoutingAgents();

    HostVO findTrafficMonitorHost();

    List<HostVO> listLocalSecondaryStorageHosts();

    List<HostVO> listLocalSecondaryStorageHosts(long dataCenterId);

    List<HostVO> listAllSecondaryStorageHosts(long dataCenterId);

    List<HostVO> listRoutingHostsByManagementServer(long msId);

    List<HostVO> listSecondaryStorageVM(long dcId);
    
    List<HostVO> listAllRoutingAgents();

	List<HostVO> findAndUpdateApplianceToLoad(long lastPingSecondsAfter, long managementServerId);

    List<HostVO> listByInAllStatus(Type type, Long clusterId, Long podId, long dcId);

    List<HostVO> listByClusterStatus(long clusterId, Status status); 
    
    List<HostVO> listSecondaryStorageVMInUpAndConnecting();
}
