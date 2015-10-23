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

import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

/**
 *
 *
 */
public interface UserVmManager extends UserVmService {
    static final String EnableDynamicallyScaleVmCK = "enable.dynamic.scale.vm";
    static final String AllowUserExpungeRecoverVmCK ="allow.user.expunge.recover.vm";
    static final ConfigKey<Boolean> EnableDynamicallyScaleVm = new ConfigKey<Boolean>("Advanced", Boolean.class, EnableDynamicallyScaleVmCK, "false",
        "Enables/Disables dynamically scaling a vm", true, ConfigKey.Scope.Zone);
    static final ConfigKey<Boolean> AllowUserExpungeRecoverVm = new ConfigKey<Boolean>("Advanced", Boolean.class, AllowUserExpungeRecoverVmCK, "false",
        "Determines whether users can expunge or recover their vm", true, ConfigKey.Scope.Account);

    static final int MAX_USER_DATA_LENGTH_BYTES = 2048;

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

    /**
     * Obtains statistics for a list of host or VMs; CPU and network utilization
     * @param host ID
     * @param host name
     * @param list of VM IDs or host id
     * @return GetVmStatsAnswer
     */
    HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds);

    HashMap<Long, List<VmDiskStatsEntry>> getVmDiskStatistics(long hostId, String hostName, List<Long> vmIds);

    boolean deleteVmGroup(long groupId);

    boolean addInstanceToGroup(long userVmId, String group);

    InstanceGroupVO getGroupForVm(long vmId);

    void removeInstanceFromInstanceGroup(long vmId);

    boolean expunge(UserVmVO vm, long callerUserId, Account caller);

    Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    boolean upgradeVirtualMachine(Long id, Long serviceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
        ConcurrentOperationException, ManagementServerException,
        VirtualMachineMigrationException;

    boolean setupVmForPvlan(boolean add, Long hostId, NicProfile nic);

    void collectVmDiskStatistics(UserVmVO userVm);

    UserVm updateVirtualMachine(long id, String displayName, String group, Boolean ha, Boolean isDisplayVmEnabled, Long osTypeId, String userData,
                                Boolean isDynamicallyScalable, HTTPMethod httpMethod, String customId, String hostName, String instanceName) throws ResourceUnavailableException, InsufficientCapacityException;

    //the validateCustomParameters, save and remove CustomOfferingDetils functions can be removed from the interface once we can
    //find a common place for all the scaling and upgrading code of both user and systemvms.
    void validateCustomParameters(ServiceOfferingVO serviceOffering, Map<String, String> customParameters);

    public void saveCustomOfferingDetails(long vmId, ServiceOffering serviceOffering);

    public void removeCustomOfferingDetails(long vmId);

    void generateUsageEvent(VirtualMachine vm, boolean isDisplay, String eventType);
}
