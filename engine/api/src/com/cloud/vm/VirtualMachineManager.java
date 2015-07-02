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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;

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
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.StoragePool;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.NoTransitionException;

/**
 * Manages allocating resources to vms.
 */
public interface VirtualMachineManager extends Manager {

    static final ConfigKey<Boolean> ExecuteInSequence = new ConfigKey<Boolean>("Advanced", Boolean.class, "execute.in.sequence.hypervisor.commands", "false",
            "If set to true, StartCommand, StopCommand, CopyCommand, MigrateCommand will be synchronized on the agent side."
                    + " If set to false, these commands become asynchronous. Default value is false.", false);

    static final ConfigKey<String> VmConfigDriveLabel = new ConfigKey<String>("Hidden", String.class, "vm.configdrive.label", "config",
            "The default lable name for the config drive", false);

    public interface Topics {
        public static final String VM_POWER_STATE = "vm.powerstate";
    }

    /**
     * Allocates a new virtual machine instance in the CloudStack DB.  This
     * orchestrates the creation of all virtual resources needed in CloudStack
     * DB to bring up a VM.
     *
     * @param vmInstanceName Instance name of the VM.  This name uniquely
     *        a VM in CloudStack's deploy environment.  The caller gets to
     *        define this VM but it must be unqiue for all of CloudStack.
     * @param template The template this VM is based on.
     * @param serviceOffering The service offering that specifies the offering this VM should provide.
     * @param defaultNetwork The default network for the VM.
     * @param rootDiskOffering For created VMs not based on templates, root disk offering specifies the root disk.
     * @param dataDiskOfferings Data disks to attach to the VM.
     * @param auxiliaryNetworks additional networks to attach the VMs to.
     * @param plan How to deploy the VM.
     * @param hyperType Hypervisor type
     * @throws InsufficientCapacityException If there are insufficient capacity to deploy this vm.
     */
    void allocate(String vmInstanceName, VirtualMachineTemplate template, ServiceOffering serviceOffering, DiskOfferingInfo rootDiskOfferingInfo,
        List<DiskOfferingInfo> dataDiskOfferings, LinkedHashMap<? extends Network, List<? extends NicProfile>> auxiliaryNetworks, DeploymentPlan plan,
        HypervisorType hyperType) throws InsufficientCapacityException;

    void allocate(String vmInstanceName, VirtualMachineTemplate template, ServiceOffering serviceOffering,
        LinkedHashMap<? extends Network, List<? extends NicProfile>> networkProfiles, DeploymentPlan plan, HypervisorType hyperType) throws InsufficientCapacityException;

    void start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params);

    void start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy, DeploymentPlanner planner);

    void stop(String vmUuid) throws ResourceUnavailableException;

    void expunge(String vmUuid) throws ResourceUnavailableException;

    void registerGuru(VirtualMachine.Type type, VirtualMachineGuru guru);

    boolean stateTransitTo(VirtualMachine vm, VirtualMachine.Event e, Long hostId) throws NoTransitionException;

    void advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlanner planner) throws InsufficientCapacityException, ResourceUnavailableException,
            ConcurrentOperationException, OperationTimedoutException;

    void advanceStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy, DeploymentPlanner planner) throws InsufficientCapacityException,
            ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    void orchestrateStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy, DeploymentPlanner planner) throws InsufficientCapacityException,
        ResourceUnavailableException, ConcurrentOperationException, OperationTimedoutException;

    void advanceStop(String vmUuid, boolean cleanupEvenIfUnableToStop) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void advanceExpunge(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void destroy(String vmUuid) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    void migrateAway(String vmUuid, long hostId) throws InsufficientServerCapacityException;

    void migrate(String vmUuid, long srcHostId, DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException;

    void migrateWithStorage(String vmUuid, long srcId, long destId, Map<Long, Long> volumeToPool) throws ResourceUnavailableException, ConcurrentOperationException;

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
     * @param newServiceOffering
     */
    void checkIfCanUpgrade(VirtualMachine vmInstance, ServiceOffering newServiceOffering);

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
    boolean removeNicFromVm(VirtualMachine vm, Nic nic) throws ConcurrentOperationException, ResourceUnavailableException;

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

    VirtualMachine reConfigureVm(String vmUuid, ServiceOffering newServiceOffering, boolean sameHost) throws ResourceUnavailableException, ConcurrentOperationException,
            InsufficientServerCapacityException;

    void findHostAndMigrate(String vmUuid, Long newSvcOfferingId, DeploymentPlanner.ExcludeList excludeHostList) throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException;

    void migrateForScale(String vmUuid, long srcHostId, DeployDestination dest, Long newSvcOfferingId) throws ResourceUnavailableException, ConcurrentOperationException;
}
