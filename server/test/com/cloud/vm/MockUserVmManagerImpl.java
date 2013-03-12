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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMSSHKeyCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateDefaultNicForVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.api.command.user.vmgroup.CreateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.DeleteVMGroupCmd;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.Criteria;
import com.cloud.storage.StoragePool;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = { UserVmManager.class, UserVmService.class })
public class MockUserVmManagerImpl extends ManagerBase implements UserVmManager, UserVmService {

    @Override
    public UserVmVO findByName(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVmVO findById(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVmVO persist(UserVmVO vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<UserVmVO> profile, long hostId, Commands cmds, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<UserVmVO> profile) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<UserVmVO> profile, StopAnswer answer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void finalizeExpunge(UserVmVO vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public Long convertToId(String vmName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends UserVm> getVirtualMachines(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVmVO getVirtualMachine(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean stopVirtualMachine(long userId, long vmId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteVmGroup(long groupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addInstanceToGroup(long userVmId, String group) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public InstanceGroupVO getGroupForVm(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeInstanceFromInstanceGroup(long vmId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean expunge(UserVmVO vm, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pair<List<UserVmJoinVO>, Integer> searchForUserVMs(Criteria c, Account caller, Long domainId, boolean isRecursive, List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm destroyVm(DestroyVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm destroyVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm startVirtualMachine(StartVMCmd cmd) throws StorageUnavailableException, ExecutionException, ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm rebootVirtualMachine(RebootVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm updateVirtualMachine(UpdateVMCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm addNicToVirtualMachine(AddNicToVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public UserVm removeNicFromVirtualMachine(RemoveNicFromVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public UserVm updateDefaultNicForVirtualMachine(UpdateDefaultNicForVMCmd cmd) throws InvalidParameterValueException, CloudRuntimeException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm startVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InstanceGroup createVmGroup(CreateVMGroupCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteVmGroup(DeleteVMGroupCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm stopVirtualMachine(long vmId, boolean forced) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deletePrivateTemplateRecord(Long templateId) {
        // TODO Auto-generated method stub

    }

    @Override
    public HypervisorType getHypervisorTypeOfUserVM(long vmid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException, StorageUnavailableException,
    ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm getUserVm(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createBasicSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> securityGroupIdList, Account owner,
            String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps,
            IpAddresses defaultIp, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException,
            ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData,
            String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
            StorageUnavailableException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner, String hostName,
            String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps,
            String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualMachine migrateVirtualMachine(Long vmId, Host destinationHost) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
    VirtualMachineMigrationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm moveVMToUser(AssignVMCmd moveUserVMCmd)
            throws ResourceAllocationException, ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualMachine vmStorageMigration(Long vmId, StoragePool destPool) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm restoreVM(RestoreVMCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void prepareStop(VirtualMachineProfile<UserVmVO> profile) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VirtualMachineGuru#plugNic(com.cloud.network.Network, com.cloud.agent.api.to.NicTO, com.cloud.agent.api.to.VirtualMachineTO, com.cloud.vm.ReservationContext, com.cloud.deploy.DeployDestination)
     */
    @Override
    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO vm, ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VirtualMachineGuru#unplugNic(com.cloud.network.Network, com.cloud.agent.api.to.NicTO, com.cloud.agent.api.to.VirtualMachineTO, com.cloud.vm.ReservationContext, com.cloud.deploy.DeployDestination)
     */
    @Override
    public boolean unplugNic(Network network, NicTO nic, VirtualMachineTO vm, ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserVm resetVMSSHKey(ResetVMSSHKeyCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }
}
