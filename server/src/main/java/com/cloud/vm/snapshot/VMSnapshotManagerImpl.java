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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.agent.api.RestoreVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.api.query.MutualExclusiveIdsManagerBase;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.gpu.GPU;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.ReflectionUse;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkConstants;
import com.cloud.vm.VmWorkJobHandler;
import com.cloud.vm.VmWorkJobHandlerProxy;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

@Component
public class VMSnapshotManagerImpl extends MutualExclusiveIdsManagerBase implements VMSnapshotManager, VMSnapshotService, VmWorkJobHandler, Configurable {
    private static final Logger s_logger = Logger.getLogger(VMSnapshotManagerImpl.class);

    public static final String VM_WORK_JOB_HANDLER = VMSnapshotManagerImpl.class.getSimpleName();

    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject VMSnapshotDao _vmSnapshotDao;
    @Inject VolumeDao _volumeDao;
    @Inject AccountDao _accountDao;
    @Inject UserVmDao _userVMDao;
    @Inject AccountManager _accountMgr;
    @Inject GuestOSDao _guestOSDao;
    @Inject SnapshotDao _snapshotDao;
    @Inject VirtualMachineManager _itMgr;
    @Inject ConfigurationDao _configDao;
    @Inject HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    StorageStrategyFactory storageStrategyFactory;
    @Inject
    VolumeDataFactory volumeDataFactory;
    @Inject
    EntityManager _entityMgr;
    @Inject
    AsyncJobManager _jobMgr;

    @Inject
    VmWorkJobDao _workJobDao;
    @Inject
    protected UserVmManager _userVmManager;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected UserVmDetailsDao _userVmDetailsDao;
    @Inject
    protected VMSnapshotDetailsDao _vmSnapshotDetailsDao;

    VmWorkJobHandlerProxy _jobHandlerProxy = new VmWorkJobHandlerProxy(this);

    int _vmSnapshotMax;
    int _wait;

    static final ConfigKey<Long> VmJobCheckInterval = new ConfigKey<Long>("Advanced",
            Long.class, "vm.job.check.interval", "3000",
            "Interval in milliseconds to check if the job is complete", false);

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
    public Pair<List<? extends VMSnapshot>, Integer> listVMSnapshots(ListVMSnapshotCmd cmd) {
        Account caller = getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Long vmId = cmd.getVmId();

        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        String name = cmd.getVmSnapshotName();
        String accountName = cmd.getAccountName();

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll,
                false);
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
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
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

        setIdsListToSearchCriteria(sc, ids);

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
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        Pair<List<VMSnapshotVO>,Integer> searchAndCount = _vmSnapshotDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VMSnapshot>, Integer>(searchAndCount.first(), searchAndCount.second());

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

        // VM snapshot with memory is not supported for VGPU Vms
        if (snapshotMemory && _serviceOfferingDetailsDao.findDetail(userVmVo.getServiceOfferingId(), GPU.Keys.vgpuType.toString()) != null) {
            throw new InvalidParameterValueException("VM snapshot with MEMORY is not supported for vGPU enabled VMs.");
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

        if(snapshotMemory && userVmVo.getState() != VirtualMachine.State.Running){
            throw new InvalidParameterValueException("Can not snapshot memory when VM is not in Running state");
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
            if (userVmVo.getHypervisorType() == HypervisorType.KVM && volume.getFormat() != ImageFormat.QCOW2) {
                throw new CloudRuntimeException("We only support create vm snapshots from vm with QCOW2 image");
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
            return createAndPersistVMSnapshot(userVmVo, vsDescription, vmSnapshotName, vsDisplayName, vmSnapshotType);
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("Create vm snapshot record failed for vm: " + vmId + " due to: " + msg);
        }
        return null;
    }

    /**
     * Create, persist and return vm snapshot for userVmVo with given parameters.
     * Persistence and support for custom service offerings are done on the same transaction
     * @param userVmVo user vm
     * @param vmId vm id
     * @param vsDescription vm description
     * @param vmSnapshotName vm snapshot name
     * @param vsDisplayName vm snapshot display name
     * @param vmSnapshotType vm snapshot type
     * @return vm snapshot
     * @throws CloudRuntimeException if vm snapshot couldn't be persisted
     */
    protected VMSnapshot createAndPersistVMSnapshot(UserVmVO userVmVo, String vsDescription, String vmSnapshotName, String vsDisplayName, VMSnapshot.Type vmSnapshotType) {
        final Long vmId = userVmVo.getId();
        final Long serviceOfferingId = userVmVo.getServiceOfferingId();
        final VMSnapshotVO vmSnapshotVo =
                new VMSnapshotVO(userVmVo.getAccountId(), userVmVo.getDomainId(), vmId, vsDescription, vmSnapshotName, vsDisplayName, serviceOfferingId,
                        vmSnapshotType, null);
        return Transaction.execute(new TransactionCallbackWithException<VMSnapshot, CloudRuntimeException>() {
            @Override
            public VMSnapshot doInTransaction(TransactionStatus status) {
                VMSnapshot vmSnapshot = _vmSnapshotDao.persist(vmSnapshotVo);
                if (vmSnapshot == null) {
                    throw new CloudRuntimeException("Failed to create snapshot for vm: " + vmId);
                }
                addSupportForCustomServiceOffering(vmId, serviceOfferingId, vmSnapshot.getId());
                return vmSnapshot;
            }
        });
    }

    /**
     * Add entries on vm_snapshot_details if service offering is dynamic. This will allow setting details when revert to vm snapshot
     * @param vmId vm id
     * @param serviceOfferingId service offering id
     * @param vmSnapshotId vm snapshot id
     */
    protected void addSupportForCustomServiceOffering(long vmId, long serviceOfferingId, long vmSnapshotId) {
        ServiceOfferingVO serviceOfferingVO = _serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOfferingVO.isDynamic()) {
            List<UserVmDetailVO> vmDetails = _userVmDetailsDao.listDetails(vmId);
            List<VMSnapshotDetailsVO> vmSnapshotDetails = new ArrayList<VMSnapshotDetailsVO>();
            for (UserVmDetailVO detail : vmDetails) {
                if(detail.getName().equalsIgnoreCase(VmDetailConstants.CPU_NUMBER) || detail.getName().equalsIgnoreCase(VmDetailConstants.CPU_SPEED) || detail.getName().equalsIgnoreCase(VmDetailConstants.MEMORY)) {
                    vmSnapshotDetails.add(new VMSnapshotDetailsVO(vmSnapshotId, detail.getName(), detail.getValue(), detail.isDisplay()));
                }
            }
            _vmSnapshotDetailsDao.saveDetails(vmSnapshotDetails);
        }
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
    public VMSnapshot createVMSnapshot(Long vmId, Long vmSnapshotId, Boolean quiescevm) {
        UserVmVO userVm = _userVMDao.findById(vmId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Create vm to snapshot failed due to vm: " + vmId + " is not found");
        }
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshot == null) {
            throw new CloudRuntimeException("VM snapshot id: " + vmSnapshotId + " can not be found");
        }

        // serialize VM operation
        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmId);
            try {
                return orchestrateCreateVMSnapshot(vmId, vmSnapshotId, quiescevm);
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }

        } else {
            Outcome<VMSnapshot> outcome = createVMSnapshotThroughJobQueue(vmId, vmSnapshotId, quiescevm);

            VMSnapshot result = null;
            try {
                result = outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException)
                    throw (ConcurrentOperationException)jobResult;
                else if (jobResult instanceof Throwable)
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
            }

            return result;
        }
    }

    private VMSnapshot orchestrateCreateVMSnapshot(Long vmId, Long vmSnapshotId, Boolean quiescevm) {
        UserVmVO userVm = _userVMDao.findById(vmId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Create vm to snapshot failed due to vm: " + vmId + " is not found");
        }

        List<VolumeVO> volumeVos = _volumeDao.findByInstanceAndType(vmId, Type.ROOT);
        if(volumeVos == null ||volumeVos.isEmpty()) {
            throw new CloudRuntimeException("Create vm to snapshot failed due to no root disk found");
        }

        VolumeVO rootVolume = volumeVos.get(0);
        if(!rootVolume.getState().equals(Volume.State.Ready)) {
            throw new CloudRuntimeException("Create vm to snapshot failed due to vm: " + vmId + " has root disk in " + rootVolume.getState() + " state");
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
            throw new CloudRuntimeException("Failed to create vm snapshot: " + vmSnapshotId, e);
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

        // serialize VM operation
        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmSnapshot.getVmId());
            try {
                return orchestrateDeleteVMSnapshot(vmSnapshotId);
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }
        } else {
            Outcome<VMSnapshot> outcome = deleteVMSnapshotThroughJobQueue(vmSnapshot.getVmId(), vmSnapshotId);

            VMSnapshot result = null;
            try {
                result = outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException)
                    throw (ConcurrentOperationException)jobResult;
                else if (jobResult instanceof Throwable)
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
            }

            if (jobResult instanceof Boolean)
                return ((Boolean)jobResult).booleanValue();

            return false;
        }
    }

    private boolean orchestrateDeleteVMSnapshot(Long vmSnapshotId) {
        Account caller = getCaller();

        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshot == null) {
            throw new InvalidParameterValueException("unable to find the vm snapshot with id " + vmSnapshotId);
        }

        _accountMgr.checkAccess(caller, null, true, vmSnapshot);

        List<VMSnapshot.State> validStates = Arrays.asList(VMSnapshot.State.Ready, VMSnapshot.State.Expunging, VMSnapshot.State.Error, VMSnapshot.State.Allocated);
        // check VM snapshot states, only allow to delete vm snapshots in ready, expunging, allocated and error state
        if (!validStates.contains(vmSnapshot.getState())) {
            throw new InvalidParameterValueException("Can't delete the vm snapshot " + vmSnapshotId + " due to it is not in " + validStates.toString()  + "States");
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
        if (userVm.getState() != VirtualMachine.State.Running
                && userVm.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm is not in the state of Running or Stopped.");
        }

        if (userVm.getState() == VirtualMachine.State.Running && vmSnapshotVo.getType() == VMSnapshot.Type.Disk || userVm.getState() == VirtualMachine.State.Stopped
                && vmSnapshotVo.getType() == VMSnapshot.Type.DiskAndMemory) {
            throw new InvalidParameterValueException(
                    "VM Snapshot revert not allowed. This will result in VM state change. You can revert running VM to disk and memory type snapshot and stopped VM to disk type"
                            + " snapshot");
        }

        // if snapshot is not created, error out
        if (vmSnapshotVo.getState() != VMSnapshot.State.Ready) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm snapshot is not in the state of Created.");
        }

        // serialize VM operation
        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance

            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmSnapshotVo.getVmId());
            try {
                return orchestrateRevertToVMSnapshot(vmSnapshotId);
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }

        } else {
            Outcome<VMSnapshot> outcome = revertToVMSnapshotThroughJobQueue(vmSnapshotVo.getVmId(), vmSnapshotId);

            VMSnapshot result = null;
            try {
                result = outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException)
                    throw (ConcurrentOperationException)jobResult;
                else if (jobResult instanceof InsufficientCapacityException)
                    throw (InsufficientCapacityException)jobResult;
                else if (jobResult instanceof ResourceUnavailableException)
                    throw (ResourceUnavailableException)jobResult;
                else if (jobResult instanceof Throwable)
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
            }

            return userVm;
        }
    }

    /**
     * If snapshot was taken with a different service offering than actual used in vm, should change it back to it
     * @param userVm vm to change service offering (if necessary)
     * @param vmSnapshotVo vm snapshot
     */
    protected void updateUserVmServiceOffering(UserVm userVm, VMSnapshotVO vmSnapshotVo) {
        if (vmSnapshotVo.getServiceOfferingId() != userVm.getServiceOfferingId()) {
            changeUserVmServiceOffering(userVm, vmSnapshotVo);
        }
    }

    /**
     * Get user vm details as a map
     * @param userVm user vm
     * @return map
     */
    protected Map<String, String> getVmMapDetails(UserVm userVm) {
        List<UserVmDetailVO> userVmDetails = _userVmDetailsDao.listDetails(userVm.getId());
        Map<String, String> details = new HashMap<String, String>();
        for (UserVmDetailVO detail : userVmDetails) {
            details.put(detail.getName(), detail.getValue());
        }
        return details;
    }

    /**
     * Update service offering on {@link userVm} to the one specified in {@link vmSnapshotVo}
     * @param userVm user vm to be updated
     * @param vmSnapshotVo vm snapshot
     */
    protected void changeUserVmServiceOffering(UserVm userVm, VMSnapshotVO vmSnapshotVo) {
        Map<String, String> vmDetails = getVmMapDetails(userVm);
        boolean result = upgradeUserVmServiceOffering(userVm.getId(), vmSnapshotVo.getServiceOfferingId(), vmDetails);
        if (! result){
            throw new CloudRuntimeException("VM Snapshot reverting failed due to vm service offering couldn't be changed to the one used when snapshot was taken");
        }
        s_logger.debug("Successfully changed service offering to " + vmSnapshotVo.getServiceOfferingId() + " for vm " + userVm.getId());
    }

    /**
     * Upgrade virtual machine {@linkplain vmId} to new service offering {@linkplain serviceOfferingId}
     * @param vmId vm id
     * @param serviceOfferingId service offering id
     * @param details vm details
     * @return if operation was successful
     */
    protected boolean upgradeUserVmServiceOffering(Long vmId, Long serviceOfferingId, Map<String, String> details) {
        boolean result;
        try {
            result = _userVmManager.upgradeVirtualMachine(vmId, serviceOfferingId, details);
            if (! result){
                s_logger.error("Couldn't change service offering for vm " + vmId + " to " + serviceOfferingId);
            }
            return result;
        } catch (ConcurrentOperationException | ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException e) {
            s_logger.error("Couldn't change service offering for vm " + vmId + " to " + serviceOfferingId + " due to: " + e.getMessage());
            return false;
        }
    }

    private UserVm orchestrateRevertToVMSnapshot(Long vmSnapshotId) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {

        // check if VM snapshot exists in DB
        final VMSnapshotVO vmSnapshotVo = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshotVo == null) {
            throw new InvalidParameterValueException(
                    "unable to find the vm snapshot with id " + vmSnapshotId);
        }
        Long vmId = vmSnapshotVo.getVmId();
        final UserVmVO userVm = _userVMDao.findById(vmId);
        // check if VM exists
        if (userVm == null) {
            throw new InvalidParameterValueException("Revert vm to snapshot: "
                    + vmSnapshotId + " failed due to vm: " + vmId
                    + " is not found");
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
                _itMgr.advanceStart(userVm.getUuid(), new HashMap<VirtualMachineProfile.Param, Object>(), null);
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
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<CloudRuntimeException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws CloudRuntimeException {
                    revertUserVmDetailsFromVmSnapshot(userVm, vmSnapshotVo);
                    updateUserVmServiceOffering(userVm, vmSnapshotVo);
                }
            });
            return userVm;
        } catch (Exception e) {
            s_logger.debug("Failed to revert vmsnapshot: " + vmSnapshotId, e);
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    /**
     * Update or add user vm details from vm snapshot for vms with custom service offerings
     * @param userVm user vm
     * @param vmSnapshotVo vm snapshot
     */
    protected void revertUserVmDetailsFromVmSnapshot(UserVmVO userVm, VMSnapshotVO vmSnapshotVo) {
        ServiceOfferingVO serviceOfferingVO = _serviceOfferingDao.findById(vmSnapshotVo.getServiceOfferingId());
        if (serviceOfferingVO.isDynamic()) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = _vmSnapshotDetailsDao.listDetails(vmSnapshotVo.getId());
            List<UserVmDetailVO> userVmDetails = new ArrayList<UserVmDetailVO>();
            for (VMSnapshotDetailsVO detail : vmSnapshotDetails) {
                userVmDetails.add(new UserVmDetailVO(userVm.getId(), detail.getName(), detail.getValue(), detail.isDisplay()));
            }
            _userVmDetailsDao.saveDetails(userVmDetails);
        }
    }

    @Override
    public RestoreVMSnapshotCommand createRestoreCommand(UserVmVO userVm, List<VMSnapshotVO> vmSnapshotVOs) {
        if (!HypervisorType.KVM.equals(userVm.getHypervisorType()))
            return null;

        List<VMSnapshotTO> snapshots = new ArrayList<VMSnapshotTO>();
        Map<Long, VMSnapshotTO> snapshotAndParents = new HashMap<Long, VMSnapshotTO>();
        for (VMSnapshotVO vmSnapshotVO: vmSnapshotVOs) {
            if (vmSnapshotVO.getType() == VMSnapshot.Type.DiskAndMemory) {
                VMSnapshotVO snapshot = _vmSnapshotDao.findById(vmSnapshotVO.getId());
                VMSnapshotTO parent = getSnapshotWithParents(snapshot).getParent();
                VMSnapshotOptions options = snapshot.getOptions();
                boolean quiescevm = true;
                if (options != null)
                    quiescevm = options.needQuiesceVM();
                VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(snapshot.getId(), snapshot.getName(), snapshot.getType(),
                        snapshot.getCreated().getTime(), snapshot.getDescription(), snapshot.getCurrent(), parent, quiescevm);
                snapshots.add(vmSnapshotTO);
                snapshotAndParents.put(vmSnapshotVO.getId(), parent);
            }
        }
        if (snapshotAndParents.isEmpty())
            return null;

        // prepare RestoreVMSnapshotCommand
        String vmInstanceName = userVm.getInstanceName();
        List<VolumeObjectTO> volumeTOs = getVolumeTOList(userVm.getId());
        GuestOSVO guestOS = _guestOSDao.findById(userVm.getGuestOSId());
        RestoreVMSnapshotCommand restoreSnapshotCommand = new RestoreVMSnapshotCommand(vmInstanceName, null, volumeTOs, guestOS.getDisplayName());
        restoreSnapshotCommand.setSnapshots(snapshots);
        restoreSnapshotCommand.setSnapshotAndParents(snapshotAndParents);
        return restoreSnapshotCommand;
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
        // serialize VM operation
        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmId);
            try {
                return orchestrateDeleteAllVMSnapshots(vmId, type);
            } finally {
                if (placeHolder != null)
                    _workJobDao.expunge(placeHolder.getId());
            }

        } else {
            Outcome<VirtualMachine> outcome = deleteAllVMSnapshotsThroughJobQueue(vmId, type);

            try {
                outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException)
                    throw (ConcurrentOperationException)jobResult;
                else if (jobResult instanceof InvalidParameterValueException)
                    throw (InvalidParameterValueException)jobResult;
                else if (jobResult instanceof Throwable)
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
            }

            if (jobResult instanceof Boolean)
                return (Boolean)jobResult;

            return false;
        }
    }

    private boolean orchestrateDeleteAllVMSnapshots(long vmId, VMSnapshot.Type type) {
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
        try{

            UserVmVO userVm = _userVMDao.findById(vm.getId());
            if (userVm == null)
                return false;

            List<VMSnapshotVO> vmSnapshotsInExpungingStates = _vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging, VMSnapshot.State.Reverting, VMSnapshot.State.Creating);
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

    public class VmJobVMSnapshotOutcome extends OutcomeImpl<VMSnapshot> {
        private long _vmSnapshotId;

        public VmJobVMSnapshotOutcome(final AsyncJob job, final long vmSnapshotId) {
            super(VMSnapshot.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert (jobVo != null);
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS)
                        return true;

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
            _vmSnapshotId = vmSnapshotId;
        }

        @Override
        protected VMSnapshot retrieve() {
            return _vmSnapshotDao.findById(_vmSnapshotId);
        }
    }

    public class VmJobVirtualMachineOutcome extends OutcomeImpl<VirtualMachine> {
        long vmId;

        public VmJobVirtualMachineOutcome(final AsyncJob job, final long vmId) {
            super(VirtualMachine.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert (jobVo != null);
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS)
                        return true;

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
        }

        @Override
        protected VirtualMachine retrieve() {
            return _vmInstanceDao.findById(vmId);
        }
    }

    public Outcome<VMSnapshot> createVMSnapshotThroughJobQueue(final Long vmId, final Long vmSnapshotId, final boolean quiesceVm) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkCreateVMSnapshot.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkCreateVMSnapshot workInfo = new VmWorkCreateVMSnapshot(callingUser.getId(), callingAccount.getId(), vm.getId(),
                VMSnapshotManagerImpl.VM_WORK_JOB_HANDLER, vmSnapshotId, quiesceVm);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVMSnapshotOutcome(workJob,vmSnapshotId);
    }

    public Outcome<VMSnapshot> deleteVMSnapshotThroughJobQueue(final Long vmId, final Long vmSnapshotId) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkDeleteVMSnapshot.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkDeleteVMSnapshot workInfo = new VmWorkDeleteVMSnapshot(callingUser.getId(), callingAccount.getId(), vm.getId(),
                VMSnapshotManagerImpl.VM_WORK_JOB_HANDLER, vmSnapshotId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVMSnapshotOutcome(workJob,vmSnapshotId);
    }

    public Outcome<VMSnapshot> revertToVMSnapshotThroughJobQueue(final Long vmId, final Long vmSnapshotId) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkRevertToVMSnapshot.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkRevertToVMSnapshot workInfo = new VmWorkRevertToVMSnapshot(callingUser.getId(), callingAccount.getId(), vm.getId(),
                VMSnapshotManagerImpl.VM_WORK_JOB_HANDLER, vmSnapshotId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVMSnapshotOutcome(workJob,vmSnapshotId);
    }

    public Outcome<VirtualMachine> deleteAllVMSnapshotsThroughJobQueue(final Long vmId, final VMSnapshot.Type type) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkDeleteAllVMSnapshots.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkDeleteAllVMSnapshots workInfo = new VmWorkDeleteAllVMSnapshots(callingUser.getId(), callingAccount.getId(), vm.getId(),
                VMSnapshotManagerImpl.VM_WORK_JOB_HANDLER, type);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob,vmId);
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateCreateVMSnapshot(VmWorkCreateVMSnapshot work) throws Exception {
        VMSnapshot snapshot = orchestrateCreateVMSnapshot(work.getVmId(), work.getVmSnapshotId(), work.isQuiesceVm());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED,
                _jobMgr.marshallResultObject(new Long(snapshot.getId())));
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateDeleteVMSnapshot(VmWorkDeleteVMSnapshot work) {
        boolean result = orchestrateDeleteVMSnapshot(work.getVmSnapshotId());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED,
                _jobMgr.marshallResultObject(result));
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateRevertToVMSnapshot(VmWorkRevertToVMSnapshot work) throws Exception {
        orchestrateRevertToVMSnapshot(work.getVmSnapshotId());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateDeleteAllVMSnapshots(VmWorkDeleteAllVMSnapshots work) {
        boolean result = orchestrateDeleteAllVMSnapshots(work.getVmId(), work.getSnapshotType());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED,
                _jobMgr.marshallResultObject(result));
    }

    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(VmWork work) throws Exception {
        return _jobHandlerProxy.handleVmWorkJob(work);
    }

    private VmWorkJobVO createPlaceHolderWork(long instanceId) {
        VmWorkJobVO workJob = new VmWorkJobVO("");

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_PLACEHOLDER);
        workJob.setCmd("");
        workJob.setCmdInfo("");

        workJob.setAccountId(0);
        workJob.setUserId(0);
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(instanceId);
        workJob.setInitMsid(ManagementServerNode.getManagementServerId());

        _workJobDao.persist(workJob);

        return workJob;
    }

    private List<VolumeObjectTO> getVolumeTOList(Long vmId) {
        List<VolumeObjectTO> volumeTOs = new ArrayList<VolumeObjectTO>();
        List<VolumeVO> volumeVos = _volumeDao.findByInstance(vmId);
        VolumeInfo volumeInfo = null;
        for (VolumeVO volume : volumeVos) {
            volumeInfo = volumeDataFactory.getVolume(volume.getId());

            volumeTOs.add((VolumeObjectTO)volumeInfo.getTO());
        }
        return volumeTOs;
    }

    private VMSnapshotTO convert2VMSnapshotTO(VMSnapshotVO vo) {
        return new VMSnapshotTO(vo.getId(), vo.getName(), vo.getType(), vo.getCreated().getTime(), vo.getDescription(), vo.getCurrent(), null, true);
    }

    private VMSnapshotTO getSnapshotWithParents(VMSnapshotVO snapshot) {
        Map<Long, VMSnapshotVO> snapshotMap = new HashMap<Long, VMSnapshotVO>();
        List<VMSnapshotVO> allSnapshots = _vmSnapshotDao.findByVm(snapshot.getVmId());
        for (VMSnapshotVO vmSnapshotVO : allSnapshots) {
            snapshotMap.put(vmSnapshotVO.getId(), vmSnapshotVO);
        }

        VMSnapshotTO currentTO = convert2VMSnapshotTO(snapshot);
        VMSnapshotTO result = currentTO;
        VMSnapshotVO current = snapshot;
        while (current.getParent() != null) {
            VMSnapshotVO parent = snapshotMap.get(current.getParent());
            if (parent == null) {
                break;
            }
            currentTO.setParent(convert2VMSnapshotTO(parent));
            current = snapshotMap.get(current.getParent());
            currentTO = currentTO.getParent();
        }
        return result;
    }

    @Override
    public boolean deleteVMSnapshotsFromDB(Long vmId) {
        List<VMSnapshotVO> listVmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (listVmSnapshots == null || listVmSnapshots.isEmpty()) {
            return true;
        }
        for (VMSnapshotVO snapshot : listVmSnapshots) {
            try {
                VMSnapshotStrategy strategy = findVMSnapshotStrategy(snapshot);
                if (! strategy.deleteVMSnapshotFromDB(snapshot)) {
                    s_logger.error("Couldn't delete vm snapshot with id " + snapshot.getId());
                    return false;
                }
            }
            catch (CloudRuntimeException e) {
                s_logger.error("Couldn't delete vm snapshot due to: " + e.getMessage());
            }
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return VMSnapshotManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VMSnapshotExpireInterval};
    }
}
