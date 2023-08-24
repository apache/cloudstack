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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

/**
 *
 *
 */
public interface UserVmManager extends UserVmService {
    String EnableDynamicallyScaleVmCK = "enable.dynamic.scale.vm";
    String AllowDiskOfferingChangeDuringScaleVmCK = "allow.diskoffering.change.during.scale.vm";
    String AllowUserExpungeRecoverVmCK ="allow.user.expunge.recover.vm";
    ConfigKey<Boolean> EnableDynamicallyScaleVm = new ConfigKey<Boolean>("Advanced", Boolean.class, EnableDynamicallyScaleVmCK, "false",
        "Enables/Disables dynamically scaling a vm", true, ConfigKey.Scope.Zone);
    ConfigKey<Boolean> AllowDiskOfferingChangeDuringScaleVm = new ConfigKey<Boolean>("Advanced", Boolean.class, AllowDiskOfferingChangeDuringScaleVmCK, "false",
            "Determines whether to allow or disallow disk offering change for root volume during scaling of a stopped or running vm", true, ConfigKey.Scope.Zone);
    ConfigKey<Boolean> AllowUserExpungeRecoverVm = new ConfigKey<Boolean>("Advanced", Boolean.class, AllowUserExpungeRecoverVmCK, "false",
        "Determines whether users can expunge or recover their vm", true, ConfigKey.Scope.Account);
    ConfigKey<Boolean> DisplayVMOVFProperties = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.display.ovf.properties", "false",
            "Set display of VMs OVF properties as part of VM details", true);

    ConfigKey<Boolean> DestroyRootVolumeOnVmDestruction = new ConfigKey<Boolean>("Advanced", Boolean.class, "destroy.root.volume.on.vm.destruction", "false",
            "Destroys the VM's root volume when the VM is destroyed.",
            true, ConfigKey.Scope.Domain);

    static final int MAX_USER_DATA_LENGTH_BYTES = 2048;

    public  static  final String CKS_NODE = "cksnode";

    /**
     * @param hostId get all of the virtual machines that belong to one host.
     * @return collection of VirtualMachine.
     */
    List<? extends UserVm> getVirtualMachines(long hostId);

    /**
     * @param vmId id of the virtual machine.
     * @return VirtualMachine
     */
    UserVmVO getVirtualMachine(long vmId);

    /**
     * Stops the virtual machine
     * @param userId the id of the user performing the action
     * @param vmId
     * @return true if stopped; false if problems.
     */
    boolean stopVirtualMachine(long userId, long vmId);

    HashMap<String, VolumeStatsEntry> getVolumeStatistics(long clusterId, String poolUuid, StoragePoolType poolType, int timeout);

    boolean deleteVmGroup(long groupId);

    boolean addInstanceToGroup(long userVmId, String group);

    InstanceGroupVO getGroupForVm(long vmId);

    void removeInstanceFromInstanceGroup(long vmId);

    String finalizeUserData(String userData, Long userDataId, VirtualMachineTemplate template);

    String validateUserData(String userData, HTTPMethod httpmethod);

    boolean isVMUsingLocalStorage(VMInstanceVO vm);

    boolean expunge(UserVmVO vm);

    Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse, boolean isExplicitHost)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    boolean upgradeVirtualMachine(Long id, Long serviceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
        ConcurrentOperationException, ManagementServerException,
        VirtualMachineMigrationException;

    boolean setupVmForPvlan(boolean add, Long hostId, NicProfile nic);

    UserVm updateVirtualMachine(long id, String displayName, String group, Boolean ha, Boolean isDisplayVmEnabled, Long osTypeId, String userData,
                                Long userDataId, String userDataDetails, Boolean isDynamicallyScalable, HTTPMethod httpMethod, String customId, String hostName, String instanceName, List<Long> securityGroupIdList, Map<String, Map<Integer, String>> extraDhcpOptionsMap) throws ResourceUnavailableException, InsufficientCapacityException;

    //the validateCustomParameters, save and remove CustomOfferingDetils functions can be removed from the interface once we can
    //find a common place for all the scaling and upgrading code of both user and systemvms.
    void validateCustomParameters(ServiceOfferingVO serviceOffering, Map<String, String> customParameters);

    void generateUsageEvent(VirtualMachine vm, boolean isDisplay, String eventType);

    void persistDeviceBusInfo(UserVmVO paramUserVmVO, String paramString);

    boolean checkIfDynamicScalingCanBeEnabled(VirtualMachine vm, ServiceOffering offering, VirtualMachineTemplate template, Long zoneId);

    Boolean getDestroyRootVolumeOnVmDestruction(Long domainId);

}
