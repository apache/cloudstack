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

package com.cloud.vm.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
@Local(value = {VMSnapshotManager.class, VMSnapshotService.class})
public class VMSnapshotManagerImpl extends ManagerBase implements VMSnapshotManager, VMSnapshotService {
    private static final Logger s_logger = Logger.getLogger(VMSnapshotManagerImpl.class);
    String _name;
    @Inject
    VMSnapshotDao _vmSnapshotDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    UserVmDao _userVMDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    GuestOSDao _guestOSDao;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    StorageStrategyFactory storageStrategyFactory;

    int _vmSnapshotMax;
    int _wait;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        _vmSnapshotMax = NumbersUtil.parseInt(_configDao.getValue("vmsnapshot.max"), VMSNAPSHOTMAX);

        String value = _configDao.getValue("vmsnapshot.create.wait");
        _wait = NumbersUtil.parseInt(value, 1800);

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<VMSnapshotVO> listVMSnapshots(ListVMSnapshotCmd cmd) {
        Account caller = getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Long vmId = cmd.getVmId();

        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        String name = cmd.getVmSnapshotName();
        String accountName = cmd.getAccountName();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject =
            new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(VMSnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMSnapshotVO> sb = _vmSnapshotDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("vm_id", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("domain_id", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getState(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("display_name", sb.entity().getDisplayName(), SearchCriteria.Op.EQ);
        sb.and("account_id", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<VMSnapshotVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (accountName != null && cmd.getDomainId() != null) {
            Account account = _accountMgr.getActiveAccountByName(accountName, cmd.getDomainId());
            sc.setParameters("account_id", account.getId());
        }

        if (vmId != null) {
            sc.setParameters("vm_id", vmId);
        }

        if (domainId != null) {
            sc.setParameters("domain_id", domainId);
        }

        if (state == null) {
            VMSnapshot.State[] status =
                {VMSnapshot.State.Ready, VMSnapshot.State.Creating, VMSnapshot.State.Allocated, VMSnapshot.State.Error, VMSnapshot.State.Expunging,
                    VMSnapshot.State.Reverting};
            sc.setParameters("status", (Object[])status);
        } else {
            sc.setParameters("state", state);
        }

        if (name != null) {
            sc.setParameters("display_name", name);
        }

        if (keyword != null) {
            SearchCriteria<VMSnapshotVO> ssc = _vmSnapshotDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("display_name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        return _vmSnapshotDao.search(sc, searchFilter);

    }

    protected Account getCaller() {
        return CallContext.current().getCallingAccount();
    }

    @Override
    public VMSnapshot allocVMSnapshot(Long vmId, String vsDisplayName, String vsDescription, Boolean snapshotMemory) throws ResourceAllocationException {

        Account caller = getCaller();

        // check if VM exists
        UserVmVO userVmVo = _userVMDao.findById(vmId);
        if (userVmVo == null) {
            throw new InvalidParameterValueException("Creating VM snapshot failed due to VM:" + vmId + " is a system VM or does not exist");
        }

        // check hypervisor capabilities
        if (!_hypervisorCapabilitiesDao.isVmSnapshotEnabled(userVmVo.getHypervisorType(), "default"))
            throw new InvalidParameterValueException("VM snapshot is not enabled for hypervisor type: " + userVmVo.getHypervisorType());

        // parameter length check
        if (vsDisplayName != null && vsDisplayName.length() > 255)
            throw new InvalidParameterValueException("Creating VM snapshot failed due to length of VM snapshot vsDisplayName should not exceed 255");
        if (vsDescription != null && vsDescription.length() > 255)
            throw new InvalidParameterValueException("Creating VM snapshot failed due to length of VM snapshot vsDescription should not exceed 255");

        // VM snapshot display name must be unique for a VM
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);
        String vmSnapshotName = userVmVo.getInstanceName() + "_VS_" + timeString;
        if (vsDisplayName == null) {
            vsDisplayName = vmSnapshotName;
        }
        if (_vmSnapshotDao.findByName(vmId, vsDisplayName) != null) {
            throw new InvalidParameterValueException("Creating VM snapshot failed due to VM snapshot with name" + vsDisplayName + "  already exists");
        }

        // check VM state
        if (userVmVo.getState() != VirtualMachine.State.Running && userVmVo.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException("Creating vm snapshot failed due to VM:" + vmId + " is not in the running or Stopped state");
        }

        if (snapshotMemory && userVmVo.getState() == VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException("Can not snapshot memory when VM is in stopped state");
        }

        // for KVM, only allow snapshot with memory when VM is in running state
        if (userVmVo.getHypervisorType() == HypervisorType.KVM && userVmVo.getState() == State.Running && !snapshotMemory) {
            throw new InvalidParameterValueException("KVM VM does not allow to take a disk-only snapshot when VM is in running state");
        }

        // check access
        _accountMgr.checkAccess(caller, null, true, userVmVo);

        // check max snapshot limit for per VM
        if (_vmSnapshotDao.findByVm(vmId).size() >= _vmSnapshotMax) {
            throw new CloudRuntimeException("Creating vm snapshot failed due to a VM can just have : " + _vmSnapshotMax + " VM snapshots. Please delete old ones");
        }

        // check if there are active volume snapshots tasks
        List<VolumeVO> listVolumes = _volumeDao.findByInstance(vmId);
        for (VolumeVO volume : listVolumes) {
            List<SnapshotVO> activeSnapshots =
                _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
            if (activeSnapshots.size() > 0) {
                throw new CloudRuntimeException("There is other active volume snapshot tasks on the instance to which the volume is attached, please try again later.");
            }
        }

        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(vmId)) {
            throw new CloudRuntimeException("There is other active vm snapshot tasks on the instance, please try again later");
        }

        VMSnapshot.Type vmSnapshotType = VMSnapshot.Type.Disk;
        if (snapshotMemory && userVmVo.getState() == VirtualMachine.State.Running)
            vmSnapshotType = VMSnapshot.Type.DiskAndMemory;

        try {
            VMSnapshotVO vmSnapshotVo =
                new VMSnapshotVO(userVmVo.getAccountId(), userVmVo.getDomainId(), vmId, vsDescription, vmSnapshotName, vsDisplayName, userVmVo.getServiceOfferingId(),
                    vmSnapshotType, null);
            VMSnapshot vmSnapshot = _vmSnapshotDao.persist(vmSnapshotVo);
            if (vmSnapshot == null) {
                throw new CloudRuntimeException("Failed to create snapshot for vm: " + vmId);
            }
            return vmSnapshot;
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("Create vm snapshot record failed for vm: " + vmId + " due to: " + msg);
        }
        return null;
    }

    @Override
    public String getName() {
        return _name;
    }

    private VMSnapshotStrategy findVMSnapshotStrategy(VMSnapshot vmSnapshot) {
        VMSnapshotStrategy snapshotStrategy = storageStrategyFactory.getVmSnapshotStrategy(vmSnapshot);

        if (snapshotStrategy == null) {
            throw new CloudRuntimeException("can't find vm snapshot strategy for vmsnapshot: " + vmSnapshot.getId());
        }

        return snapshotStrategy;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_CREATE, eventDescription = "creating VM snapshot", async = true)
    public VMSnapshot creatVMSnapshot(Long vmId, Long vmSnapshotId, Boolean quiescevm) {
        UserVmVO userVm = _userVMDao.findById(vmId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Create vm to snapshot failed due to vm: " + vmId + " is not found");
        }
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshot == null) {
            throw new CloudRuntimeException("VM snapshot id: " + vmSnapshotId + " can not be found");
        }
        VMSnapshotOptions options = new VMSnapshotOptions(quiescevm);
        vmSnapshot.setOptions(options);
        try {
            VMSnapshotStrategy strategy = findVMSnapshotStrategy(vmSnapshot);
            VMSnapshot snapshot = strategy.takeVMSnapshot(vmSnapshot);
            return snapshot;
        } catch (Exception e) {
            s_logger.debug("Failed to create vm snapshot: " + vmSnapshotId, e);
            return null;
        }
    }

    public VMSnapshotManagerImpl() {

    }

    @Override
    public boolean hasActiveVMSnapshotTasks(Long vmId) {
        List<VMSnapshotVO> activeVMSnapshots =
            _vmSnapshotDao.listByInstanceId(vmId, VMSnapshot.State.Creating, VMSnapshot.State.Expunging, VMSnapshot.State.Reverting, VMSnapshot.State.Allocated);
        return activeVMSnapshots.size() > 0;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_DELETE, eventDescription = "delete vm snapshots", async = true)
    public boolean deleteVMSnapshot(Long vmSnapshotId) {
        Account caller = getCaller();

        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshot == null) {
            throw new InvalidParameterValueException("unable to find the vm snapshot with id " + vmSnapshotId);
        }

        _accountMgr.checkAccess(caller, null, true, vmSnapshot);

        // check VM snapshot states, only allow to delete vm snapshots in created and error state
        if (VMSnapshot.State.Ready != vmSnapshot.getState() && VMSnapshot.State.Expunging != vmSnapshot.getState() && VMSnapshot.State.Error != vmSnapshot.getState()) {
            throw new InvalidParameterValueException("Can't delete the vm snapshotshot " + vmSnapshotId + " due to it is not in Created or Error, or Expunging State");
        }

        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(vmSnapshot.getVmId())) {
            List<VMSnapshotVO> expungingSnapshots = _vmSnapshotDao.listByInstanceId(vmSnapshot.getVmId(), VMSnapshot.State.Expunging);
            if (expungingSnapshots.size() > 0 && expungingSnapshots.get(0).getId() == vmSnapshot.getId())
                s_logger.debug("Target VM snapshot already in expunging state, go on deleting it: " + vmSnapshot.getDisplayName());
            else
                throw new InvalidParameterValueException("There is other active vm snapshot tasks on the instance, please try again later");
        }

        if (vmSnapshot.getState() == VMSnapshot.State.Allocated) {
            return _vmSnapshotDao.remove(vmSnapshot.getId());
        } else {
            try {
                VMSnapshotStrategy strategy = findVMSnapshotStrategy(vmSnapshot);
                return strategy.deleteVMSnapshot(vmSnapshot);
            } catch (Exception e) {
                s_logger.debug("Failed to delete vm snapshot: " + vmSnapshotId, e);
                return false;
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_REVERT, eventDescription = "revert to VM snapshot", async = true)
    public UserVm revertToSnapshot(Long vmSnapshotId) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {

        // check if VM snapshot exists in DB
        VMSnapshotVO vmSnapshotVo = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshotVo == null) {
            throw new InvalidParameterValueException("unable to find the vm snapshot with id " + vmSnapshotId);
        }
        Long vmId = vmSnapshotVo.getVmId();
        UserVmVO userVm = _userVMDao.findById(vmId);
        // check if VM exists
        if (userVm == null) {
            throw new InvalidParameterValueException("Revert vm to snapshot: " + vmSnapshotId + " failed due to vm: " + vmId + " is not found");
        }

        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(vmId)) {
            throw new InvalidParameterValueException("There is other active vm snapshot tasks on the instance, please try again later");
        }

        Account caller = getCaller();
        _accountMgr.checkAccess(caller, null, true, vmSnapshotVo);

        // VM should be in running or stopped states
        if (userVm.getState() != VirtualMachine.State.Running && userVm.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException("VM Snapshot reverting failed due to vm is not in the state of Running or Stopped.");
        }

        // if snapshot is not created, error out
        if (vmSnapshotVo.getState() != VMSnapshot.State.Ready) {
            throw new InvalidParameterValueException("VM Snapshot reverting failed due to vm snapshot is not in the state of Created.");
        }

        UserVmVO vm = null;
        Long hostId = null;

        // start or stop VM first, if revert from stopped state to running state, or from running to stopped
        if (userVm.getState() == VirtualMachine.State.Stopped && vmSnapshotVo.getType() == VMSnapshot.Type.DiskAndMemory) {
            try {
                _itMgr.advanceStart(userVm.getUuid(), new HashMap<VirtualMachineProfile.Param, Object>());
                vm = _userVMDao.findById(userVm.getId());
                hostId = vm.getHostId();
            } catch (Exception e) {
                s_logger.error("Start VM " + userVm.getInstanceName() + " before reverting failed due to " + e.getMessage());
                throw new CloudRuntimeException(e.getMessage());
            }
        } else {
            if (userVm.getState() == VirtualMachine.State.Running && vmSnapshotVo.getType() == VMSnapshot.Type.Disk) {
                try {
                    _itMgr.advanceStop(userVm.getUuid(), true);
                } catch (Exception e) {
                    s_logger.error("Stop VM " + userVm.getInstanceName() + " before reverting failed due to " + e.getMessage());
                    throw new CloudRuntimeException(e.getMessage());
                }
            }
        }

        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(userVm.getId())) {
            throw new InvalidParameterValueException("There is other active vm snapshot tasks on the instance, please try again later");
        }

        try {
            VMSnapshotStrategy strategy = findVMSnapshotStrategy(vmSnapshotVo);
            strategy.revertVMSnapshot(vmSnapshotVo);
            return userVm;
        } catch (Exception e) {
            s_logger.debug("Failed to revert vmsnapshot: " + vmSnapshotId, e);
            return null;
        }
    }

    @Override
    public VMSnapshot getVMSnapshotById(Long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        return vmSnapshot;
    }

    @Override
    public VirtualMachine getVMBySnapshotId(Long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        if (vmSnapshot == null) {
            throw new InvalidParameterValueException("unable to find the vm snapshot with id " + id);
        }
        Long vmId = vmSnapshot.getVmId();
        UserVmVO vm = _userVMDao.findById(vmId);
        return vm;
    }

    @Override
    public boolean deleteAllVMSnapshots(long vmId, VMSnapshot.Type type) {
        boolean result = true;
        List<VMSnapshotVO> listVmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (listVmSnapshots == null || listVmSnapshots.isEmpty()) {
            return true;
        }
        for (VMSnapshotVO snapshot : listVmSnapshots) {
            VMSnapshotVO target = _vmSnapshotDao.findById(snapshot.getId());
            if (type != null && target.getType() != type)
                continue;
            VMSnapshotStrategy strategy = findVMSnapshotStrategy(target);
            if (!strategy.deleteVMSnapshot(target)) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public boolean syncVMSnapshot(VMInstanceVO vm, Long hostId) {
        try {

            UserVmVO userVm = _userVMDao.findById(vm.getId());
            if (userVm == null)
                return false;

            List<VMSnapshotVO> vmSnapshotsInExpungingStates =
                _vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging, VMSnapshot.State.Reverting, VMSnapshot.State.Creating);
            for (VMSnapshotVO vmSnapshotVO : vmSnapshotsInExpungingStates) {
                VMSnapshotStrategy strategy = findVMSnapshotStrategy(vmSnapshotVO);
                if (vmSnapshotVO.getState() == VMSnapshot.State.Expunging) {
                    return strategy.deleteVMSnapshot(vmSnapshotVO);
                } else if (vmSnapshotVO.getState() == VMSnapshot.State.Creating) {
                    return strategy.takeVMSnapshot(vmSnapshotVO) != null;
                } else if (vmSnapshotVO.getState() == VMSnapshot.State.Reverting) {
                    return strategy.revertVMSnapshot(vmSnapshotVO);
                }
            }
        } catch (Exception e) {
            s_logger.error(e.getMessage(), e);
            if (_vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging).size() == 0)
                return true;
            else
                return false;
        }
        return false;
    }

}
