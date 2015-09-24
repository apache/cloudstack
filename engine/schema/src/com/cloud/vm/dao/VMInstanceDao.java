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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

/*
 * Data Access Object for vm_instance table
 */
public interface VMInstanceDao extends GenericDao<VMInstanceVO, Long>, StateDao<State, VirtualMachine.Event, VirtualMachine> {
    /**
     * What are the vms running on this host?
     * @param hostId host.
     * @return list of VMInstanceVO running on that host.
     */
    List<VMInstanceVO> listByHostId(long hostId);

    /**
     * List VMs by zone ID
     * @param zoneId
     * @return list of VMInstanceVO in the specified zone
     */
    List<VMInstanceVO> listByZoneId(long zoneId);

    /**
     * List VMs by pod ID
     * @param podId
     * @return list of VMInstanceVO in the specified pod
     */
    List<VMInstanceVO> listByPodId(long podId);

    /**
     * Lists non-expunged VMs by zone ID and templateId
     * @param zoneId
     * @return list of VMInstanceVO in the specified zone, deployed from the specified template, that are not expunged
     */
    public List<VMInstanceVO> listNonExpungedByZoneAndTemplate(long zoneId, long templateId);

    /**
     * Find vm instance with names like.
     *
     * @param name name that fits SQL like.
     * @return list of VMInstanceVO
     */
    List<VMInstanceVO> findVMInstancesLike(String name);

    List<VMInstanceVO> findVMInTransition(Date time, State... states);

    List<VMInstanceVO> listByHostAndState(long hostId, State... states);

    List<VMInstanceVO> listByTypes(VirtualMachine.Type... types);

    VMInstanceVO findByIdTypes(long id, VirtualMachine.Type... types);

    VMInstanceVO findVMByInstanceName(String name);

    VMInstanceVO findVMByHostName(String hostName);

    void updateProxyId(long id, Long proxyId, Date time);

    List<VMInstanceVO> listByHostIdTypes(long hostid, VirtualMachine.Type... types);

    List<VMInstanceVO> listUpByHostIdTypes(long hostid, VirtualMachine.Type... types);

    List<VMInstanceVO> listByZoneIdAndType(long zoneId, VirtualMachine.Type type);

    List<VMInstanceVO> listUpByHostId(Long hostId);

    List<VMInstanceVO> listByLastHostId(Long hostId);

    List<VMInstanceVO> listByTypeAndState(VirtualMachine.Type type, State state);

    List<VMInstanceVO> listByAccountId(long accountId);

    public List<Long> findIdsOfAllocatedVirtualRoutersForAccount(long accountId);

    List<VMInstanceVO> listByClusterId(long clusterId);  // this does not pull up VMs which are starting

    List<VMInstanceVO> listLHByClusterId(long clusterId);  // get all the VMs even starting one on this cluster

    List<VMInstanceVO> listVmsMigratingFromHost(Long hostId);

    public Long countActiveByHostId(long hostId);

    Pair<List<Long>, Map<Long, Double>> listClusterIdsInZoneByVmCount(long zoneId, long accountId);

    Pair<List<Long>, Map<Long, Double>> listClusterIdsInPodByVmCount(long podId, long accountId);

    Pair<List<Long>, Map<Long, Double>> listPodIdsInZoneByVmCount(long dataCenterId, long accountId);

    List<Long> listHostIdsByVmCount(long dcId, Long podId, Long clusterId, long accountId);

    Long countRunningByAccount(long accountId);

    List<VMInstanceVO> listNonRemovedVmsByTypeAndNetwork(long networkId, VirtualMachine.Type... types);

    /**
     * @param networkId
     * @param types
     * @return
     */
    List<String> listDistinctHostNames(long networkId, VirtualMachine.Type... types);

    List<VMInstanceVO> findByHostInStates(Long hostId, State... states);

    List<VMInstanceVO> listStartingWithNoHostId();

    boolean updatePowerState(long instanceId, long powerHostId, VirtualMachine.PowerState powerState);

    void resetVmPowerStateTracking(long instanceId);

    void resetHostPowerStateTracking(long hostId);

    HashMap<String, Long> countVgpuVMs(Long dcId, Long podId, Long clusterId);

    VMInstanceVO findVMByHostNameInZone(String hostName, long zoneId);

    boolean isPowerStateUpToDate(long instanceId);
}
