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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.event.UsageEventVO;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class DefaultVMSnapshotStrategy extends ManagerBase implements VMSnapshotStrategy {
    private static final Logger s_logger = Logger.getLogger(DefaultVMSnapshotStrategy.class);
    @Inject
    VMSnapshotHelper vmSnapshotHelper;
    @Inject
    GuestOSDao guestOSDao;
    @Inject
    GuestOSHypervisorDao guestOsHypervisorDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    int _wait;
    @Inject
    ConfigurationDao configurationDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volumeDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    HostDao hostDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String value = configurationDao.getValue("vmsnapshot.create.wait");
        _wait = NumbersUtil.parseInt(value, 1800);
        return true;
    }

    @Override
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
        boolean result = false;
        try {
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());

            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            long prev_chain_size = 0;
            long virtual_size=0;
            for (VolumeObjectTO volume : volumeTOs) {
                virtual_size += volume.getSize();
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null)
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            VMSnapshotOptions options = ((VMSnapshotVO)vmSnapshot).getOptions();
            boolean quiescevm = true;
            if (options != null)
                quiescevm = options.needQuiesceVM();
            VMSnapshotTO target =
                new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(), null, vmSnapshot.getDescription(), false, current, quiescevm);
            if (current == null)
                vmSnapshotVO.setParent(null);
            else
                vmSnapshotVO.setParent(current.getId());

            HostVO host = hostDao.findById(hostId);
            GuestOSHypervisorVO guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), host.getHypervisorType().toString(), host.getHypervisorVersion());

            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(userVm.getInstanceName(), userVm.getUuid(), target, volumeTOs, guestOS.getDisplayName());
            if (guestOsMapping == null) {
                ccmd.setPlatformEmulator(null);
            } else {
                ccmd.setPlatformEmulator(guestOsMapping.getGuestOsName());
            }
            ccmd.setWait(_wait);

            answer = (CreateVMSnapshotAnswer)agentMgr.send(hostId, ccmd);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer, hostId);
                s_logger.debug("Create vm snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());
                result = true;
                long new_chain_size=0;
                for (VolumeObjectTO volumeTo : answer.getVolumeTOs()) {
                    publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, volumeTo);
                    new_chain_size += volumeTo.getSize();
                }
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm, new_chain_size - prev_chain_size, virtual_size);
                return vmSnapshot;
            } else {
                String errMsg = "Creating VM snapshot: " + vmSnapshot.getName() + " failed";
                if (answer != null && answer.getDetails() != null)
                    errMsg = errMsg + " due to " + answer.getDetails();
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (OperationTimedoutException e) {
            s_logger.debug("Creating VM snapshot: " + vmSnapshot.getName() + " failed: " + e.toString());
            throw new CloudRuntimeException("Creating VM snapshot: " + vmSnapshot.getName() + " failed: " + e.toString());
        } catch (AgentUnavailableException e) {
            s_logger.debug("Creating VM snapshot: " + vmSnapshot.getName() + " failed", e);
            throw new CloudRuntimeException("Creating VM snapshot: " + vmSnapshot.getName() + " failed: " + e.toString());
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    s_logger.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
        }
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException("Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }

        try {
            Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());

            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());

            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();
            VMSnapshotTO vmSnapshotTO =
                new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(), vmSnapshot.getCreated().getTime(), vmSnapshot.getDescription(),
                    vmSnapshot.getCurrent(), parent, true);
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            DeleteVMSnapshotCommand deleteSnapshotCommand = new DeleteVMSnapshotCommand(vmInstanceName, vmSnapshotTO, volumeTOs, guestOS.getDisplayName());

            Answer answer = agentMgr.send(hostId, deleteSnapshotCommand);

            if (answer != null && answer.getResult()) {
                DeleteVMSnapshotAnswer deleteVMSnapshotAnswer = (DeleteVMSnapshotAnswer)answer;
                processAnswer(vmSnapshotVO, userVm, answer, hostId);
                long full_chain_size=0;
                for (VolumeObjectTO volumeTo : deleteVMSnapshotAnswer.getVolumeTOs()) {
                    publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
                    full_chain_size += volumeTo.getSize();
                }
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
                return true;
            } else {
                String errMsg = (answer == null) ? null : answer.getDetails();
                s_logger.error("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + errMsg);
                throw new CloudRuntimeException("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + errMsg);
            }
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + e.getMessage());
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName() + " failed due to " + e.getMessage());
        }
    }

    @DB
    protected void processAnswer(final VMSnapshotVO vmSnapshot, UserVm userVm, final Answer as, Long hostId) {
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<NoTransitionException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws NoTransitionException {
                    if (as instanceof CreateVMSnapshotAnswer) {
                        CreateVMSnapshotAnswer answer = (CreateVMSnapshotAnswer)as;
                        finalizeCreate(vmSnapshot, answer.getVolumeTOs());
                        vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
                    } else if (as instanceof RevertToVMSnapshotAnswer) {
                        RevertToVMSnapshotAnswer answer = (RevertToVMSnapshotAnswer)as;
                        finalizeRevert(vmSnapshot, answer.getVolumeTOs());
                        vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);
                    } else if (as instanceof DeleteVMSnapshotAnswer) {
                        DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer)as;
                        finalizeDelete(vmSnapshot, answer.getVolumeTOs());
                        vmSnapshotDao.remove(vmSnapshot.getId());
                    }
                }
            });
        } catch (Exception e) {
            String errMsg = "Error while process answer: " + as.getClass() + " due to " + e.getMessage();
            s_logger.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    protected void finalizeDelete(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeTOs) {
        // update volumes path
        updateVolumePath(volumeTOs);

        // update children's parent snapshots
        List<VMSnapshotVO> children = vmSnapshotDao.listByParent(vmSnapshot.getId());
        for (VMSnapshotVO child : children) {
            child.setParent(vmSnapshot.getParent());
            vmSnapshotDao.persist(child);
        }

        // update current snapshot
        VMSnapshotVO current = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if (current != null && current.getId() == vmSnapshot.getId() && vmSnapshot.getParent() != null) {
            VMSnapshotVO parent = vmSnapshotDao.findById(vmSnapshot.getParent());
            parent.setCurrent(true);
            vmSnapshotDao.persist(parent);
        }
        vmSnapshot.setCurrent(false);
        vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeCreate(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeTOs) {
        // update volumes path
        updateVolumePath(volumeTOs);

        vmSnapshot.setCurrent(true);

        // change current snapshot
        if (vmSnapshot.getParent() != null) {
            VMSnapshotVO previousCurrent = vmSnapshotDao.findById(vmSnapshot.getParent());
            previousCurrent.setCurrent(false);
            vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshotDao.persist(vmSnapshot);
    }

    protected void finalizeRevert(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeToList) {
        // update volumes path
        updateVolumePath(volumeToList);

        // update current snapshot, current snapshot is the one reverted to
        VMSnapshotVO previousCurrent = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if (previousCurrent != null) {
            previousCurrent.setCurrent(false);
            vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshot.setCurrent(true);
        vmSnapshotDao.persist(vmSnapshot);
    }

    private void updateVolumePath(List<VolumeObjectTO> volumeTOs) {
        for (VolumeObjectTO volume : volumeTOs) {
            if (volume.getPath() != null) {
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                volumeVO.setPath(volume.getPath());
                volumeVO.setVmSnapshotChainSize(volume.getSize());
                volumeDao.persist(volumeVO);
            }
        }
    }

    private void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, VolumeObjectTO volumeTo) {
        VolumeVO volume = volumeDao.findById(volumeTo.getId());
        Long diskOfferingId = volume.getDiskOfferingId();
        Long offeringId = null;
        if (diskOfferingId != null) {
            DiskOfferingVO offering = diskOfferingDao.findById(diskOfferingId);
            if (offering != null && (offering.getType() == DiskOfferingVO.Type.Disk)) {
                offeringId = offering.getId();
            }
        }
        Map<String, String> details = new HashMap<>();
        if (vmSnapshot != null) {
            details.put(UsageEventVO.DynamicParameters.vmSnapshotId.name(), String.valueOf(vmSnapshot.getId()));
        }
        UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(), vmSnapshot.getName(), offeringId, volume.getId(), // save volume's id into templateId field
                volumeTo.getSize(), VMSnapshot.class.getName(), vmSnapshot.getUuid(), details);
    }

    private void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, Long vmSnapSize, Long virtualSize) {
        try {
            Map<String, String> details = new HashMap<>();
            if (vmSnapshot != null) {
                details.put(UsageEventVO.DynamicParameters.vmSnapshotId.name(), String.valueOf(vmSnapshot.getId()));
            }
            UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(), vmSnapshot.getName(), 0L, 0L, vmSnapSize, virtualSize,
                    VMSnapshot.class.getName(), vmSnapshot.getUuid(), details);
        } catch (Exception e) {
            s_logger.error("Failed to publis usage event " + type, e);
        }
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO)vmSnapshot;
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            VMSnapshotVO snapshot = vmSnapshotDao.findById(vmSnapshotVO.getId());
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(snapshot).getParent();

            VMSnapshotTO vmSnapshotTO =
                new VMSnapshotTO(snapshot.getId(), snapshot.getName(), snapshot.getType(), snapshot.getCreated().getTime(), snapshot.getDescription(),
                    snapshot.getCurrent(), parent, true);
            Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            RevertToVMSnapshotCommand revertToSnapshotCommand = new RevertToVMSnapshotCommand(vmInstanceName, userVm.getUuid(), vmSnapshotTO, volumeTOs, guestOS.getDisplayName());
            HostVO host = hostDao.findById(hostId);
            GuestOSHypervisorVO guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), host.getHypervisorType().toString(), host.getHypervisorVersion());
            if (guestOsMapping == null) {
                revertToSnapshotCommand.setPlatformEmulator(null);
            } else {
                revertToSnapshotCommand.setPlatformEmulator(guestOsMapping.getGuestOsName());
            }

            RevertToVMSnapshotAnswer answer = (RevertToVMSnapshotAnswer)agentMgr.send(hostId, revertToSnapshotCommand);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer, hostId);
                result = true;
            } else {
                String errMsg = "Revert VM: " + userVm.getInstanceName() + " to snapshot: " + vmSnapshotVO.getName() + " failed";
                if (answer != null && answer.getDetails() != null)
                    errMsg = errMsg + " due to " + answer.getDetails();
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (OperationTimedoutException e) {
            s_logger.debug("Failed to revert vm snapshot", e);
            throw new CloudRuntimeException(e.getMessage());
        } catch (AgentUnavailableException e) {
            s_logger.debug("Failed to revert vm snapshot", e);
            throw new CloudRuntimeException(e.getMessage());
        } finally {
            if (!result) {
                try {
                    vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    s_logger.error("Cannot set vm snapshot state due to: " + e1.getMessage());
                }
            }
        }
        return result;
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        return StrategyPriority.DEFAULT;
    }

    @Override
    public boolean deleteVMSnapshotFromDB(VMSnapshot vmSnapshot) {
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException("Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
        for (VolumeObjectTO volumeTo: volumeTOs) {
            volumeTo.setSize(0);
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
        }
        return vmSnapshotDao.remove(vmSnapshot.getId());
    }
}
