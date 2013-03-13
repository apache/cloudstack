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

import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
@Local(value = { VMSnapshotManager.class, VMSnapshotService.class })
public class VMSnapshotManagerImpl extends ManagerBase implements VMSnapshotManager, VMSnapshotService {
    private static final Logger s_logger = Logger.getLogger(VMSnapshotManagerImpl.class);
    String _name;
    @Inject VMSnapshotDao _vmSnapshotDao;
    @Inject VolumeDao _volumeDao;
    @Inject AccountDao _accountDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject UserVmDao _userVMDao;
    @Inject HostDao _hostDao;
    @Inject UserDao _userDao;
    @Inject AgentManager _agentMgr;
    @Inject HypervisorGuruManager _hvGuruMgr;
    @Inject AccountManager _accountMgr;
    @Inject GuestOSDao _guestOSDao;
    @Inject PrimaryDataStoreDao _storagePoolDao;
    @Inject SnapshotDao _snapshotDao;
    @Inject VirtualMachineManager _itMgr;
    @Inject DataStoreManager dataStoreMgr;
    @Inject ConfigurationDao _configDao;
    int _vmSnapshotMax;
    StateMachine2<VMSnapshot.State, VMSnapshot.Event, VMSnapshot> _vmSnapshottateMachine ;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        if (_configDao == null) {
            throw new ConfigurationException(
                    "Unable to get the configuration dao.");
        }

        _vmSnapshotMax = NumbersUtil.parseInt(_configDao.getValue("vmsnapshot.max"), VMSNAPSHOTMAX);

        _vmSnapshottateMachine   = VMSnapshot.State.getStateMachine();
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
            VMSnapshot.State[] status = { VMSnapshot.State.Ready, VMSnapshot.State.Creating, VMSnapshot.State.Allocated, 
                    VMSnapshot.State.Error, VMSnapshot.State.Expunging, VMSnapshot.State.Reverting };
            sc.setParameters("status", (Object[]) status);
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

    protected Account getCaller(){
        return UserContext.current().getCaller();
    }
    
    @Override
    public VMSnapshot allocVMSnapshot(Long vmId, String vsDisplayName, String vsDescription, Boolean snapshotMemory) 
            throws ResourceAllocationException {

        Account caller = getCaller();

        // check if VM exists
        UserVmVO userVmVo = _userVMDao.findById(vmId);
        if (userVmVo == null) {
            throw new InvalidParameterValueException("Creating VM snapshot failed due to VM:" + vmId + " is a system VM or does not exist");
        }
        
        // parameter length check
        if(vsDisplayName != null && vsDisplayName.length()>255)
            throw new InvalidParameterValueException("Creating VM snapshot failed due to length of VM snapshot vsDisplayName should not exceed 255");
        if(vsDescription != null && vsDescription.length()>255)
            throw new InvalidParameterValueException("Creating VM snapshot failed due to length of VM snapshot vsDescription should not exceed 255");
        
        // VM snapshot display name must be unique for a VM
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);
        String vmSnapshotName = userVmVo.getInstanceName() + "_VS_" + timeString;
        if (vsDisplayName == null) {
            vsDisplayName = vmSnapshotName;
        }
        if(_vmSnapshotDao.findByName(vmId,vsDisplayName) != null){
            throw new InvalidParameterValueException("Creating VM snapshot failed due to VM snapshot with name" + vsDisplayName + "  already exists"); 
        }
        
        // check VM state
        if (userVmVo.getState() != VirtualMachine.State.Running && userVmVo.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException("Creating vm snapshot failed due to VM:" + vmId + " is not in the running or Stopped state");
        }
        
        if(snapshotMemory && userVmVo.getState() == VirtualMachine.State.Stopped){
            throw new InvalidParameterValueException("Can not snapshot memory when VM is in stopped state");
        }
        
        // for KVM, only allow snapshot with memory when VM is in running state
        if(userVmVo.getHypervisorType() == HypervisorType.KVM && userVmVo.getState() == State.Running && !snapshotMemory){
            throw new InvalidParameterValueException("KVM VM does not allow to take a disk-only snapshot when VM is in running state");
        }
        
        // check access
        _accountMgr.checkAccess(caller, null, true, userVmVo);

        // check max snapshot limit for per VM
        if (_vmSnapshotDao.findByVm(vmId).size() >= _vmSnapshotMax) {
            throw new CloudRuntimeException("Creating vm snapshot failed due to a VM can just have : " + _vmSnapshotMax
                    + " VM snapshots. Please delete old ones");
        }

        // check if there are active volume snapshots tasks
        List<VolumeVO> listVolumes = _volumeDao.findByInstance(vmId);
        for (VolumeVO volume : listVolumes) {
            List<SnapshotVO> activeSnapshots = _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.State.Creating,
                    Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
            if (activeSnapshots.size() > 0) {
                throw new CloudRuntimeException(
                        "There is other active volume snapshot tasks on the instance to which the volume is attached, please try again later.");
            }
        }
        
        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(vmId)) {
            throw new CloudRuntimeException("There is other active vm snapshot tasks on the instance, please try again later");
        }
        
        VMSnapshot.Type vmSnapshotType = VMSnapshot.Type.Disk;
        if(snapshotMemory && userVmVo.getState() == VirtualMachine.State.Running)
            vmSnapshotType = VMSnapshot.Type.DiskAndMemory;
        
        try {
            VMSnapshotVO vmSnapshotVo = new VMSnapshotVO(userVmVo.getAccountId(), userVmVo.getDomainId(), vmId, vsDescription, vmSnapshotName,
                    vsDisplayName, userVmVo.getServiceOfferingId(), vmSnapshotType, null);
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_CREATE, eventDescription = "creating VM snapshot", async = true)
    public VMSnapshot creatVMSnapshot(Long vmId, Long vmSnapshotId) {
        UserVmVO userVm = _userVMDao.findById(vmId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Create vm to snapshot failed due to vm: " + vmId + " is not found");
        }
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if(vmSnapshot == null){
            throw new CloudRuntimeException("VM snapshot id: " + vmSnapshotId + " can not be found");
        }
        Long hostId = pickRunningHost(vmId);
        try {
            vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return createVmSnapshotInternal(userVm, vmSnapshot, hostId);
    }

    protected VMSnapshot createVmSnapshotInternal(UserVmVO userVm, VMSnapshotVO vmSnapshot, Long hostId) {
        try { 
            CreateVMSnapshotAnswer answer = null;
            GuestOSVO guestOS = _guestOSDao.findById(userVm.getGuestOSId());
            
            // prepare snapshotVolumeTos
            List<VolumeTO> volumeTOs = getVolumeTOList(userVm.getId());
            
            // prepare target snapshotTO and its parent snapshot (current snapshot)
            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = _vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null)
                current = getSnapshotWithParents(currentSnapshot);
            VMSnapshotTO target = new VMSnapshotTO(vmSnapshot.getId(),  vmSnapshot.getName(), vmSnapshot.getType(), null, vmSnapshot.getDescription(), false,
                    current);
            if (current == null)
                vmSnapshot.setParent(null);
            else
                vmSnapshot.setParent(current.getId());

            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(userVm.getInstanceName(),target ,volumeTOs, guestOS.getDisplayName(),userVm.getState());
            
            answer = (CreateVMSnapshotAnswer) sendToPool(hostId, ccmd);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshot, userVm, answer, hostId);
                s_logger.debug("Create vm snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());
            }else{
                String errMsg = answer.getDetails();
                s_logger.error("Agent reports creating vm snapshot " + vmSnapshot.getName() + " failed for vm: " + userVm.getInstanceName() + " due to " + errMsg);
                vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
            }
            return vmSnapshot;
        } catch (Exception e) {
            if(e instanceof AgentUnavailableException){
                try {
                    vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    s_logger.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
            String msg = e.getMessage();
            s_logger.error("Create vm snapshot " + vmSnapshot.getName() + " failed for vm: " + userVm.getInstanceName() + " due to " + msg);
            throw new CloudRuntimeException(msg);
        } finally{
            if(vmSnapshot.getState() == VMSnapshot.State.Allocated){
                s_logger.warn("Create vm snapshot " + vmSnapshot.getName() + " failed for vm: " + userVm.getInstanceName());
                _vmSnapshotDao.remove(vmSnapshot.getId());
            }
        }
    }

    protected List<VolumeTO> getVolumeTOList(Long vmId) {
        List<VolumeTO> volumeTOs = new ArrayList<VolumeTO>();
        List<VolumeVO> volumeVos = _volumeDao.findByInstance(vmId);
        
        for (VolumeVO volume : volumeVos) {
            StoragePool pool = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(volume.getPoolId());
            VolumeTO volumeTO = new VolumeTO(volume, pool);
            volumeTOs.add(volumeTO);
        }
        return volumeTOs;
    }

    // get snapshot and its parents recursively
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
            currentTO.setParent(convert2VMSnapshotTO(parent));
            current = snapshotMap.get(current.getParent());
            currentTO = currentTO.getParent();
        }
        return result;
    }

    private VMSnapshotTO convert2VMSnapshotTO(VMSnapshotVO vo) {
        return new VMSnapshotTO(vo.getId(), vo.getName(),  vo.getType(), vo.getCreated().getTime(), vo.getDescription(),
                vo.getCurrent(), null);
    }

    protected boolean vmSnapshotStateTransitTo(VMSnapshotVO vsnp, VMSnapshot.Event event) throws NoTransitionException {
        return _vmSnapshottateMachine.transitTo(vsnp, event, null, _vmSnapshotDao);
    }
    
    @DB
    protected void processAnswer(VMSnapshotVO vmSnapshot, UserVmVO  userVm, Answer as, Long hostId) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            if (as instanceof CreateVMSnapshotAnswer) {
                CreateVMSnapshotAnswer answer = (CreateVMSnapshotAnswer) as;
                finalizeCreate(vmSnapshot, answer.getVolumeTOs());
                vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
            } else if (as instanceof RevertToVMSnapshotAnswer) {
                RevertToVMSnapshotAnswer answer = (RevertToVMSnapshotAnswer) as;
                finalizeRevert(vmSnapshot, answer.getVolumeTOs());
                vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
            } else if (as instanceof DeleteVMSnapshotAnswer) {
                DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer) as;
                finalizeDelete(vmSnapshot, answer.getVolumeTOs());
                _vmSnapshotDao.remove(vmSnapshot.getId());
            }
            txn.commit();
        } catch (Exception e) {
            String errMsg = "Error while process answer: " + as.getClass() + " due to " + e.getMessage();
            s_logger.error(errMsg, e);
            txn.rollback();
            throw new CloudRuntimeException(errMsg);
        } finally {
            txn.close();
        }
    }

    protected void finalizeDelete(VMSnapshotVO vmSnapshot, List<VolumeTO> VolumeTOs) {
        // update volumes path
        updateVolumePath(VolumeTOs);
        
        // update children's parent snapshots
        List<VMSnapshotVO> children= _vmSnapshotDao.listByParent(vmSnapshot.getId());
        for (VMSnapshotVO child : children) {
            child.setParent(vmSnapshot.getParent());
            _vmSnapshotDao.persist(child);
        }
        
        // update current snapshot 
        VMSnapshotVO current = _vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if(current != null && current.getId() == vmSnapshot.getId() && vmSnapshot.getParent() != null){
            VMSnapshotVO parent = _vmSnapshotDao.findById(vmSnapshot.getParent());
            parent.setCurrent(true);
            _vmSnapshotDao.persist(parent);
        }
        vmSnapshot.setCurrent(false);
        _vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeCreate(VMSnapshotVO vmSnapshot, List<VolumeTO> VolumeTOs) {
        // update volumes path
        updateVolumePath(VolumeTOs);
        
        vmSnapshot.setCurrent(true);
        
        // change current snapshot
        if (vmSnapshot.getParent() != null) {
            VMSnapshotVO previousCurrent = _vmSnapshotDao.findById(vmSnapshot.getParent());
            previousCurrent.setCurrent(false);
            _vmSnapshotDao.persist(previousCurrent);
        }
        _vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeRevert(VMSnapshotVO vmSnapshot, List<VolumeTO> volumeToList) {
        // update volumes path
        updateVolumePath(volumeToList);

        // update current snapshot, current snapshot is the one reverted to
        VMSnapshotVO previousCurrent = _vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if(previousCurrent != null){
            previousCurrent.setCurrent(false);
            _vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshot.setCurrent(true);
        _vmSnapshotDao.persist(vmSnapshot);
    }

    private void updateVolumePath(List<VolumeTO> volumeTOs) {
        for (VolumeTO volume : volumeTOs) {
            if (volume.getPath() != null) {
                VolumeVO volumeVO = _volumeDao.findById(volume.getId());
                volumeVO.setPath(volume.getPath());
                _volumeDao.persist(volumeVO);
            }
        }
    }
    
    public VMSnapshotManagerImpl() {
        
    }
    
    protected Answer sendToPool(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException {
        long targetHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(hostId, cmd);
        Answer answer = _agentMgr.send(targetHostId, cmd);
        return answer;
    }
    
    @Override
    public boolean hasActiveVMSnapshotTasks(Long vmId){
        List<VMSnapshotVO> activeVMSnapshots = _vmSnapshotDao.listByInstanceId(vmId, 
                VMSnapshot.State.Creating, VMSnapshot.State.Expunging,VMSnapshot.State.Reverting,VMSnapshot.State.Allocated);
        return activeVMSnapshots.size() > 0;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_DELETE, eventDescription = "delete vm snapshots", async=true)
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
            if(expungingSnapshots.size() > 0 && expungingSnapshots.get(0).getId() == vmSnapshot.getId())
                s_logger.debug("Target VM snapshot already in expunging state, go on deleting it: " + vmSnapshot.getDisplayName());
            else
                throw new InvalidParameterValueException("There is other active vm snapshot tasks on the instance, please try again later");
        }

        if(vmSnapshot.getState() == VMSnapshot.State.Allocated){
            return _vmSnapshotDao.remove(vmSnapshot.getId());
        }else{
            return deleteSnapshotInternal(vmSnapshot);
        }
    }

    @DB
    protected boolean deleteSnapshotInternal(VMSnapshotVO vmSnapshot) {
        UserVmVO userVm = _userVMDao.findById(vmSnapshot.getVmId());
        
        try {
            vmSnapshotStateTransitTo(vmSnapshot,VMSnapshot.Event.ExpungeRequested);
            Long hostId = pickRunningHost(vmSnapshot.getVmId());

            // prepare snapshotVolumeTos
            List<VolumeTO> volumeTOs = getVolumeTOList(vmSnapshot.getVmId());
            
            // prepare DeleteVMSnapshotCommand
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = getSnapshotWithParents(vmSnapshot).getParent();
            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(), 
                    vmSnapshot.getCreated().getTime(), vmSnapshot.getDescription(), vmSnapshot.getCurrent(), parent);
            GuestOSVO guestOS = _guestOSDao.findById(userVm.getGuestOSId());
            DeleteVMSnapshotCommand deleteSnapshotCommand = new DeleteVMSnapshotCommand(vmInstanceName, vmSnapshotTO, volumeTOs,guestOS.getDisplayName());
            
            DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer) sendToPool(hostId, deleteSnapshotCommand);
           
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshot, userVm, answer, hostId);
                s_logger.debug("Delete VM snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());
                return true;
            } else {
                s_logger.error("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + answer.getDetails());
                return false;
            }
        } catch (Exception e) {
            String msg = "Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + e.getMessage();
            s_logger.error(msg , e);
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_REVERT, eventDescription = "revert to VM snapshot", async = true)
    public UserVm revertToSnapshot(Long vmSnapshotId) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {

        // check if VM snapshot exists in DB
        VMSnapshotVO vmSnapshotVo = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshotVo == null) {
            throw new InvalidParameterValueException(
                    "unable to find the vm snapshot with id " + vmSnapshotId);
        }
        Long vmId = vmSnapshotVo.getVmId();
        UserVmVO userVm = _userVMDao.findById(vmId);
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
        if (userVm.getState() != VirtualMachine.State.Running
                && userVm.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm is not in the state of Running or Stopped.");
        }
        
        // if snapshot is not created, error out
        if (vmSnapshotVo.getState() != VMSnapshot.State.Ready) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm snapshot is not in the state of Created.");
        }

        UserVO callerUser = _userDao.findById(UserContext.current().getCallerUserId());
        
        UserVmVO vm = null;
        Long hostId = null;
        Account owner = _accountDao.findById(vmSnapshotVo.getAccountId());
        
        // start or stop VM first, if revert from stopped state to running state, or from running to stopped
        if(userVm.getState() == VirtualMachine.State.Stopped && vmSnapshotVo.getType() == VMSnapshot.Type.DiskAndMemory){
            try {
        	    vm = _itMgr.advanceStart(userVm, new HashMap<VirtualMachineProfile.Param, Object>(), callerUser, owner);
        	    hostId = vm.getHostId();
        	} catch (Exception e) {
        	    s_logger.error("Start VM " + userVm.getInstanceName() + " before reverting failed due to " + e.getMessage());
        	    throw new CloudRuntimeException(e.getMessage());
        	}
        }else {
            if(userVm.getState() == VirtualMachine.State.Running && vmSnapshotVo.getType() == VMSnapshot.Type.Disk){
                try {
    			    _itMgr.advanceStop(userVm, true, callerUser, owner);
                } catch (Exception e) {
                    s_logger.error("Stop VM " + userVm.getInstanceName() + " before reverting failed due to " + e.getMessage());
    			    throw new CloudRuntimeException(e.getMessage());
                }
            }
            hostId = pickRunningHost(userVm.getId());
        }
        
        if(hostId == null)
            throw new CloudRuntimeException("Can not find any host to revert snapshot " + vmSnapshotVo.getName());
        
        // check if there are other active VM snapshot tasks
        if (hasActiveVMSnapshotTasks(userVm.getId())) {
            throw new InvalidParameterValueException("There is other active vm snapshot tasks on the instance, please try again later");
        }
        
        userVm = _userVMDao.findById(userVm.getId());
        try {
            vmSnapshotStateTransitTo(vmSnapshotVo, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return revertInternal(userVm, vmSnapshotVo, hostId);
    }

    private UserVm revertInternal(UserVmVO userVm, VMSnapshotVO vmSnapshotVo, Long hostId) {
        try {
            VMSnapshotVO snapshot = _vmSnapshotDao.findById(vmSnapshotVo.getId());
            // prepare RevertToSnapshotCommand
            List<VolumeTO> volumeTOs = getVolumeTOList(userVm.getId());
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = getSnapshotWithParents(snapshot).getParent();
            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(snapshot.getId(), snapshot.getName(), snapshot.getType(), 
            		snapshot.getCreated().getTime(), snapshot.getDescription(), snapshot.getCurrent(), parent);
            
            GuestOSVO guestOS = _guestOSDao.findById(userVm.getGuestOSId());
            RevertToVMSnapshotCommand revertToSnapshotCommand = new RevertToVMSnapshotCommand(vmInstanceName, vmSnapshotTO, volumeTOs, guestOS.getDisplayName());
           
            RevertToVMSnapshotAnswer answer = (RevertToVMSnapshotAnswer) sendToPool(hostId, revertToSnapshotCommand);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVo, userVm, answer, hostId);
                s_logger.debug("RevertTo " + vmSnapshotVo.getName() + " succeeded for vm: " + userVm.getInstanceName());
            } else {
                String errMsg = "Revert VM: " + userVm.getInstanceName() + " to snapshot: "+ vmSnapshotVo.getName() + " failed";
                if(answer != null && answer.getDetails() != null)
                    errMsg = errMsg + " due to " + answer.getDetails();
                s_logger.error(errMsg);
                // agent report revert operation fails
                vmSnapshotStateTransitTo(vmSnapshotVo, VMSnapshot.Event.OperationFailed);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (Exception e) {
            if(e instanceof AgentUnavailableException){
                try {
                    vmSnapshotStateTransitTo(vmSnapshotVo, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    s_logger.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
            // for other exceptions, do not change VM snapshot state, leave it for snapshotSync
            String errMsg = "revert vm: " + userVm.getInstanceName() + " to snapshot " + vmSnapshotVo.getName() + " failed due to " + e.getMessage();
            s_logger.error(errMsg);
            throw new CloudRuntimeException(e.getMessage());
        } 
        return userVm;
    }


    @Override
    public VMSnapshot getVMSnapshotById(Long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        return vmSnapshot;
    }

    protected Long pickRunningHost(Long vmId) {
        UserVmVO vm = _userVMDao.findById(vmId);
        // use VM's host if VM is running
        if(vm.getState() == State.Running)
            return vm.getHostId();
        
        // check if lastHostId is available
        if(vm.getLastHostId() != null){
           HostVO lastHost =  _hostDao.findById(vm.getLastHostId());
           if(lastHost.getStatus() == com.cloud.host.Status.Up && !lastHost.isInMaintenanceStates())
               return lastHost.getId();
        }
        
        List<VolumeVO> listVolumes = _volumeDao.findByInstance(vmId);
        if (listVolumes == null || listVolumes.size() == 0) {
            throw new InvalidParameterValueException("vmInstance has no volumes");
        }
        VolumeVO volume = listVolumes.get(0);
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            throw new InvalidParameterValueException("pool id is not found");
        }
        StoragePoolVO storagePool = _storagePoolDao.findById(poolId);
        if (storagePool == null) {
            throw new InvalidParameterValueException("storage pool is not found");
        }
        List<HostVO> listHost = _hostDao.listAllUpAndEnabledNonHAHosts(Host.Type.Routing, storagePool.getClusterId(), storagePool.getPodId(),
                storagePool.getDataCenterId(), null);
        if (listHost == null || listHost.size() == 0) {
            throw new InvalidParameterValueException("no host in up state is found");
        }
        return listHost.get(0).getId();
    }

    @Override
    public VirtualMachine getVMBySnapshotId(Long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        if(vmSnapshot == null){
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
            if(type != null && target.getType() != type)
                continue;
            if (!deleteSnapshotInternal(target)) {
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
            if(userVm == null)
                return false;
            
            List<VMSnapshotVO> vmSnapshotsInExpungingStates = _vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging, VMSnapshot.State.Reverting, VMSnapshot.State.Creating);
            for (VMSnapshotVO vmSnapshotVO : vmSnapshotsInExpungingStates) {
                if(vmSnapshotVO.getState() == VMSnapshot.State.Expunging){
                    return deleteSnapshotInternal(vmSnapshotVO);
                }else if(vmSnapshotVO.getState() == VMSnapshot.State.Creating){
                    return createVmSnapshotInternal(userVm, vmSnapshotVO, hostId) != null;
                }else if(vmSnapshotVO.getState() == VMSnapshot.State.Reverting){
                    return revertInternal(userVm, vmSnapshotVO, hostId) != null;
                }
            }
        }catch (Exception e) {
            s_logger.error(e.getMessage(),e);
            if(_vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging).size() == 0)
                return true;
            else
                return false;
        }
        return false;
    }
 
}
