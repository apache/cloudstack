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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * Manages allocating resources to vms.
 */
public interface VirtualMachineManager extends Manager {

    VirtualMachine allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkVO, NicProfile>> networks,
            Map<VirtualMachineProfile.Param, Object> params,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner);

    VirtualMachine allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<Pair<NetworkVO, NicProfile>> networkProfiles,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner);

    VirtualMachine start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException,
            ResourceUnavailableException;

    VirtualMachine start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account, DeploymentPlan planToDeploy)
            throws InsufficientCapacityException, ResourceUnavailableException;

    boolean stop(String vmUuid, User caller, Account account);

    boolean expunge(String vmUuid, User caller, Account account) throws ResourceUnavailableException;

    void registerGuru(VirtualMachine.Type type, VirtualMachineGuru guru);
    
    // FIXME: This method is added by VirtualMachinePowerStateSyncImpl
    Collection<VirtualMachineGuru> getRegisteredGurus();

    // FIXME: Apparently this method is added by Kelven for VmWorkJobDispatcher.  Should look into removing this.
    VirtualMachineGuru getVmGuru(VirtualMachine vm);
    
    boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) throws NoTransitionException;

    VirtualMachine advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException,
            ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    VirtualMachine advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account, DeploymentPlan planToDeploy)
            throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    boolean advanceStop(String vmUuid, boolean forced, User caller, Account account) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    boolean advanceExpunge(String vmUuid, User caller, Account account) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    boolean destroy(String vmUuid, User caller, Account account) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    boolean migrateAway(VirtualMachine.Type type, long vmid, long hostId) throws InsufficientServerCapacityException, VirtualMachineMigrationException;

    VirtualMachine migrate(String vmUuid, long srcHostId, DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
            VirtualMachineMigrationException;

    VirtualMachine migrateWithStorage(String vmUuid, long srcId, long destId, Map<VolumeVO, StoragePoolVO> volumeToPool) throws ResourceUnavailableException,
            ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException;

    boolean reboot(String vmUuid, User caller, Account account);

    boolean advanceReboot(String vmUuid, User caller, Account account) throws InsufficientCapacityException,
            ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    VirtualMachine storageMigration(String vmUuid, StoragePool storagePoolId);

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
     * @param profile
     * @param hvGuru
     * @return
     */
    VirtualMachineTO toVmTO(VirtualMachineProfile profile);


    VirtualMachine reConfigureVm(VirtualMachine vm, ServiceOffering newServiceOffering, boolean sameHost)
            throws ResourceUnavailableException, ConcurrentOperationException;

    VirtualMachine findHostAndMigrate(String vmUuid, Long newSvcOfferingId) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException,
            VirtualMachineMigrationException, ManagementServerException;

    VirtualMachine migrateForScale(String vmUuid, long srcHostId, DeployDestination dest, Long newSvcOfferingId)
            throws ResourceUnavailableException, ConcurrentOperationException,
            ManagementServerException, VirtualMachineMigrationException;

    //
    // VM work handlers
    //
    VirtualMachine processVmStartWork(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account, DeploymentPlan planToDeploy)
           throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;

    boolean processVmStopWork(String vmUuid, boolean forced, User user, Account account)
    	throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    NicTO toNicTO(NicProfile nic, HypervisorType hypervisorType);
}
