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
package com.cloud.vm;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * Manages allocating resources to vms.
 */
public interface VirtualMachineManager extends Manager {

    void allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkVO, NicProfile>> networks,
            Map<VirtualMachineProfile.Param, Object> params,
            DeploymentPlan plan,
            HypervisorType hyperType) throws InsufficientCapacityException;

    void allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<Pair<NetworkVO, NicProfile>> networkProfiles,
            DeploymentPlan plan,
            HypervisorType hyperType) throws InsufficientCapacityException;

    void start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params);

    void start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy);

    void stop(String vmUuid) throws ResourceUnavailableException;

    void expunge(String vmUuid) throws ResourceUnavailableException;

    void registerGuru(VirtualMachine.Type type, VirtualMachineGuru guru);

    boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) throws NoTransitionException;

    void advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException,
            ConcurrentOperationException, OperationTimedoutException;

    void advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy) throws InsufficientCapacityException,
            ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    void advanceStop(String vmUuid, boolean cleanupEvenIfUnableToStop) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void advanceExpunge(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void destroy(String vmUuid) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void migrateAway(String vmUuid, long hostId) throws InsufficientServerCapacityException;

    void migrate(String vmUuid, long srcHostId, DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException;

    void migrateWithStorage(String vmUuid, long srcId, long destId, Map<Volume, StoragePool> volumeToPool) throws ResourceUnavailableException, ConcurrentOperationException;

    void reboot(String vmUuid, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException;

    void advanceReboot(String vmUuid, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException,
            ConcurrentOperationException, OperationTimedoutException;

    /**
     * Check to see if a virtual machine can be upgraded to the given service offering
     * 
     * @param vm
     * @param offering
     * @return true if the host can handle the upgrade, false otherwise
     */
    boolean isVirtualMachineUpgradable(final VirtualMachine vm, final ServiceOffering offering);
    
    VirtualMachine findById(long vmId);

    void storageMigration(String vmUuid, StoragePool storagePoolId);

    /**
     * @param vmInstance
     * @param newServiceOfferingId
     */
    void checkIfCanUpgrade(VirtualMachine vmInstance, long newServiceOfferingId);

    /**
     * @param vmId
     * @param serviceOfferingId
     * @return
     */
    boolean upgradeVmDb(long vmId, long serviceOfferingId);

    /**
     * @param vm
     * @param network
     * @param requested TODO
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     * @throws InsufficientCapacityException
     */
    NicProfile addVmToNetwork(VirtualMachine vm, Network network, NicProfile requested) throws ConcurrentOperationException,
                ResourceUnavailableException, InsufficientCapacityException;

    /**
     * @param vm
     * @param nic
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean removeNicFromVm(VirtualMachine vm, NicVO nic) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param vm
     * @param network
     * @param broadcastUri TODO
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean removeVmFromNetwork(VirtualMachine vm, Network network, URI broadcastUri) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param nic
     * @param hypervisorType
     * @return
     */
    NicTO toNicTO(NicProfile nic, HypervisorType hypervisorType);

    /**
     * @param profile
     * @param hvGuru
     * @return
     */
    VirtualMachineTO toVmTO(VirtualMachineProfile profile);


    VMInstanceVO reConfigureVm(VMInstanceVO vm, ServiceOffering newServiceOffering, boolean sameHost) throws ResourceUnavailableException, ConcurrentOperationException;

    void findHostAndMigrate(String vmUuid, Long newSvcOfferingId, DeploymentPlanner.ExcludeList excludeHostList) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException;

    void migrateForScale(String vmUuid, long srcHostId, DeployDestination dest, Long newSvcOfferingId) throws ResourceUnavailableException, ConcurrentOperationException;

}
