/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.vmsnapshot;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class DefaultVMSnapshotStrategy extends ManagerBase implements VMSnapshotStrategy {
    private static final Logger s_logger = Logger.getLogger(DefaultVMSnapshotStrategy.class);
    @Inject
    VMSnapshotHelper vmSnapshotHelper;
    @Inject
    GuestOSDao guestOSDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    int _wait;
    @Inject
    ConfigurationDao configurationDao;
    @Inject
    AgentManager agentMgr;
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String value = configurationDao.getValue("vmsnapshot.create.wait");
        _wait = NumbersUtil.parseInt(value, 1800);
        return true;
    }

    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        CreateVMSnapshotAnswer answer = null;
        try {
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());

            // prepare snapshotVolumeTos
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            // prepare target snapshotTO and its parent snapshot (current snapshot)
            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null)
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            VMSnapshotTO target = new VMSnapshotTO(vmSnapshot.getId(),  vmSnapshot.getName(), vmSnapshot.getType(), null, vmSnapshot.getDescription(), false,
                    current);
            if (current == null)
                vmSnapshotVO.setParent(null);
            else
                vmSnapshotVO.setParent(current.getId());

            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(userVm.getInstanceName(),target ,volumeTOs, guestOS.getDisplayName(),userVm.getState());
            ccmd.setWait(_wait);

            answer = (CreateVMSnapshotAnswer)agentMgr.send(hostId, ccmd);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer, hostId);
                s_logger.debug("Create vm snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());
            }else{
                String errMsg = "Creating VM snapshot: " + vmSnapshot.getName() + " failed";
                if(answer != null && answer.getDetails() != null)
                    errMsg = errMsg + " due to " + answer.getDetails();
                s_logger.error(errMsg);
                vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                throw new CloudRuntimeException(errMsg);
            }
            return vmSnapshot;
        } catch (Exception e) {
            if(e instanceof AgentUnavailableException){
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
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
                vmSnapshotDao.remove(vmSnapshot.getId());
            }
            if(vmSnapshot.getState() == VMSnapshot.State.Ready && answer != null){
                for (VolumeTO volumeTo : answer.getVolumeTOs()){
                    publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_CREATE,vmSnapshot,userVm,volumeTo);
                }
            }
        }
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;
        DeleteVMSnapshotAnswer answer = null;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot,VMSnapshot.Event.ExpungeRequested);
            Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());

            // prepare snapshotVolumeTos
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());

            // prepare DeleteVMSnapshotCommand
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();
            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(),
                    vmSnapshot.getCreated().getTime(), vmSnapshot.getDescription(), vmSnapshot.getCurrent(), parent);
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            DeleteVMSnapshotCommand deleteSnapshotCommand = new DeleteVMSnapshotCommand(vmInstanceName, vmSnapshotTO, volumeTOs,guestOS.getDisplayName());

            answer = (DeleteVMSnapshotAnswer) agentMgr.send(hostId, deleteSnapshotCommand);

            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer, hostId);
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
        } finally{
            if(answer != null && answer.getResult()){
                for (VolumeTO volumeTo : answer.getVolumeTOs()){
                    publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE,vmSnapshot,userVm,volumeTo);
                }
            }
        }
    }

    @DB
    protected void processAnswer(VMSnapshotVO vmSnapshot, UserVm userVm, Answer as, Long hostId) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            if (as instanceof CreateVMSnapshotAnswer) {
                CreateVMSnapshotAnswer answer = (CreateVMSnapshotAnswer) as;
                finalizeCreate(vmSnapshot, answer.getVolumeTOs());
                vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
            } else if (as instanceof RevertToVMSnapshotAnswer) {
                RevertToVMSnapshotAnswer answer = (RevertToVMSnapshotAnswer) as;
                finalizeRevert(vmSnapshot, answer.getVolumeTOs());
                vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
            } else if (as instanceof DeleteVMSnapshotAnswer) {
                DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer) as;
                finalizeDelete(vmSnapshot, answer.getVolumeTOs());
                vmSnapshotDao.remove(vmSnapshot.getId());
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
        List<VMSnapshotVO> children= vmSnapshotDao.listByParent(vmSnapshot.getId());
        for (VMSnapshotVO child : children) {
            child.setParent(vmSnapshot.getParent());
            vmSnapshotDao.persist(child);
        }

        // update current snapshot
        VMSnapshotVO current = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if(current != null && current.getId() == vmSnapshot.getId() && vmSnapshot.getParent() != null){
            VMSnapshotVO parent = vmSnapshotDao.findById(vmSnapshot.getParent());
            parent.setCurrent(true);
            vmSnapshotDao.persist(parent);
        }
        vmSnapshot.setCurrent(false);
        vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeCreate(VMSnapshotVO vmSnapshot, List<VolumeTO> VolumeTOs) {
        // update volumes path
        updateVolumePath(VolumeTOs);

        vmSnapshot.setCurrent(true);

        // change current snapshot
        if (vmSnapshot.getParent() != null) {
            VMSnapshotVO previousCurrent = vmSnapshotDao.findById(vmSnapshot.getParent());
            previousCurrent.setCurrent(false);
            vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeRevert(VMSnapshotVO vmSnapshot, List<VolumeTO> volumeToList) {
        // update volumes path
        updateVolumePath(volumeToList);

        // update current snapshot, current snapshot is the one reverted to
        VMSnapshotVO previousCurrent = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if(previousCurrent != null){
            previousCurrent.setCurrent(false);
            vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshot.setCurrent(true);
        vmSnapshotDao.persist(vmSnapshot);
    }

    private void updateVolumePath(List<VolumeTO> volumeTOs) {
        for (VolumeTO volume : volumeTOs) {
            if (volume.getPath() != null) {
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                volumeVO.setPath(volume.getPath());
                volumeVO.setVmSnapshotChainSize(volume.getChainSize());
                volumeDao.persist(volumeVO);
            }
        }
    }

    private void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, VolumeTO volumeTo){
        VolumeVO volume = _volumeDao.findById(volumeTo.getId());
        Long diskOfferingId = volume.getDiskOfferingId();
        Long offeringId = null;
        if (diskOfferingId != null) {
            DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);
            if (offering != null
                    && (offering.getType() == DiskOfferingVO.Type.Disk)) {
                offeringId = offering.getId();
            }
        }
        UsageEventUtils.publishUsageEvent(
                type,
                vmSnapshot.getAccountId(),
                userVm.getDataCenterId(),
                userVm.getId(),
                vmSnapshot.getName(),
                offeringId,
                volume.getId(), // save volume's id into templateId field
                volumeTo.getChainSize(),
                VMSnapshot.class.getName(), vmSnapshot.getUuid());
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        userVm = _userVMDao.findById(userVm.getId());
        try {
            vmSnapshotStateTransitTo(vmSnapshotVo, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

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
    public boolean canHandle(VMSnapshot vmSnapshot) {
        return true;
    }
}
