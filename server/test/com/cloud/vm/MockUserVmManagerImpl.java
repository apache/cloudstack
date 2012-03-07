package com.cloud.vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.AssignVMCmd;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.CreateVMGroupCmd;
import com.cloud.api.commands.DeleteVMGroupCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.DestroyVMCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.ListVMsCmd;
import com.cloud.api.commands.RebootVMCmd;
import com.cloud.api.commands.RecoverVMCmd;
import com.cloud.api.commands.ResetVMPasswordCmd;
import com.cloud.api.commands.RestoreVMCmd;
import com.cloud.api.commands.StartVMCmd;
import com.cloud.api.commands.UpdateVMCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.Criteria;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.ExecutionException;

@Local(value = { UserVmManager.class, UserVmService.class })
public class MockUserVmManagerImpl implements UserVmManager, UserVmService, Manager {

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
    public boolean attachISOToVM(long vmId, long isoId, boolean attach) {
        // TODO Auto-generated method stub
        return false;
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
    public List<UserVmVO> searchForUserVMs(Criteria c, Account caller, Long domainId, boolean isRecursive, List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getChecksum(Long hostId, String templatePath) {
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
    public Volume attachVolumeToVM(AttachVolumeCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Volume detachVolumeFromVM(DetachVolumeCmd cmmd) {
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
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualMachineTemplate createPrivateTemplateRecord(CreateTemplateCmd cmd, Account templateOwner) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualMachineTemplate createPrivateTemplate(CreateTemplateCmd cmd) {
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
            String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, String> requestedIps,
            String defaultIp, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException,
            ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData,
            String sshKeyPair, Map<Long, String> requestedIps, String defaultIp, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
            StorageUnavailableException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner, String hostName,
            String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, String> requestedIps, String defaultIp,
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
    public List<? extends UserVm> searchForUserVMs(ListVMsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm startVirtualMachine(long vmId, Long hostId) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

}
