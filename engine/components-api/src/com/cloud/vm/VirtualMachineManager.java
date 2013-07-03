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

import org.apache.cloudstack.framework.jobs.Outcome;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
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
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * VirtualMachineManager orchestrates the life cycle of a virtual machine.
 * It does not care what is the type of the virtual machine.  As long as
 * it can find the virtual machine in the database via the vm instance name,
 * it starts to orchestrates the life cycle.
 * 
 * There's a set of easy to use methods to start/stop a virtual machine.  These
 * methods rethrows any exceptions as CloudRuntimeException so the caller's
 * code can be less complicated.
 * 
 * There is also a set of advance methods.  The advance methods throw
 * exceptions that describes the problem that actually happen.  For callers,
 * that can adjust to the exceptions, it should call the advance methods.
 * 
 * All of the methods expect the UserContext to be set.  It retrieves information
 * wrt the user and account of the caller from the UserContext.  The current
 * caller and account input parameters will be deprecated.
 * 
 */
public interface VirtualMachineManager extends Manager {
    public interface Topics {
        public static final String VM_POWER_STATE = "vm.powerstate";
    }

    boolean allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkVO, NicProfile>> networks,
            Map<VirtualMachineProfile.Param, Object> params,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner);

    boolean allocate(String vmInstanceName,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<Pair<NetworkVO, NicProfile>> networkProfiles,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner);

    void easyStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params);

    void easyStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy);

    void easyStop(String vmUuid);

    void expunge(String vmUuid);

    void registerGuru(VirtualMachine.Type type, VirtualMachineGuru guru);
    
    boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) throws NoTransitionException;

    /**
     * Files a start job to start the virtual machine.  The caller can use
     * the Outcome object to wait for the result.  The Outcome throws
     * ExecutionException if there's a problem with the job execution.
     * The cause of the ExecutionException carries the reason to why
     * there is a problem.
     *   - ConcurrentOperationException: There are multiple operations happening on the same objects.
     *   - InsufficientCapacityException: Insufficient capacity to start a VM.  The exception carries the cause.
     *   - ResourceUnavailableException: The resource needed to start a VM is not available.
     *   - OperationTimedoutException: The operation has been sent to the physical resource but we timed out waiting for results.
     * 
     * Most callers should use this method to start VMs.  Of the various
     * possible exceptions, the worst is OperationTimedoutException.  This
     * indicates that the operation was sent to the physical resource but
     * there was no response.  Under these situations, we do not know if the
     * operation succeeded or failed and require manual intervention.
     * 
     * @param vmUuid uuid to the VM to start
     * @param params parameters passed to be passed down
     * @param planToDeploy plan on where to deploy the vm.
     * @return Outcome to wait for the result.
     */
    Outcome<VirtualMachine> start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy);

    Outcome<VirtualMachine> stop(String vmUuid, boolean cleanup) throws ResourceUnavailableException, OperationTimedoutException,
            ConcurrentOperationException;

    void advanceExpunge(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException,
            ConcurrentOperationException;

    boolean destroy(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException,
            ConcurrentOperationException;

    boolean migrateAway(VirtualMachine.Type type, long vmid, long hostId) throws InsufficientServerCapacityException, VirtualMachineMigrationException;

    Outcome<VirtualMachine> migrate(String vmUuid, long srcHostId, DeployDestination dest);

    Outcome<VirtualMachine> migrateWithStorage(String vmUuid, long srcHostId, long destId, Map<Volume, StoragePool> volumeToPool);

    void reboot(String vmUuid);

    void advanceReboot(String vmUuid) throws InsufficientCapacityException,
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


    boolean reConfigureVm(VirtualMachine vm, ServiceOffering newServiceOffering, boolean sameHost)
            throws ResourceUnavailableException, ConcurrentOperationException;

    boolean findHostAndMigrate(String vmUuid, Long newSvcOfferingId) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException,
            VirtualMachineMigrationException, ManagementServerException;

    boolean migrateForScale(String vmUuid, long srcHostId, DeployDestination dest, Long newSvcOfferingId)
            throws ResourceUnavailableException, ConcurrentOperationException,
            ManagementServerException, VirtualMachineMigrationException;

    NicTO toNicTO(NicProfile nic, HypervisorType hypervisorType);

}
