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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FreezeThawVMAnswer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.AccountService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
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

@Component
public class StorageVMSnapshotStrategy extends ManagerBase implements VMSnapshotStrategy {
    private static final Logger s_logger = Logger.getLogger(StorageVMSnapshotStrategy.class);
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
    @Inject
    VolumeApiService volumeService;
    @Inject
    AccountService accountService;
    @Inject
    VolumeDataFactory volumeDataFactory;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    StorageStrategyFactory storageStrategyFactory;
    @Inject
    SnapshotDataFactory snapshotDataFactory;
    @Inject
    PrimaryDataStoreDao storagePool;
    @Inject
    DataStoreProviderManager dataStoreProviderMgr;
    @Inject
    SnapshotApiService snapshotApiService;
    private int _wait;

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
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;

        CreateVMSnapshotAnswer answer = null;
        FreezeThawVMCommand freezeCommand = null;
        FreezeThawVMAnswer freezeAnswer = null;
        FreezeThawVMCommand thawCmd = null;
        FreezeThawVMAnswer thawAnswer = null;
        List<SnapshotInfo> forRollback = new ArrayList<>();
        long startFreeze = 0;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.KVMCreateRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

            long prev_chain_size = 0;
            long virtual_size = 0;

            VMSnapshotTO current = null;
            VMSnapshotVO currentSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
            if (currentSnapshot != null) {
                current = vmSnapshotHelper.getSnapshotWithParents(currentSnapshot);
            }
            VMSnapshotOptions options = ((VMSnapshotVO) vmSnapshot).getOptions();
            boolean quiescevm = true;
            if (options != null) {
                quiescevm = options.needQuiesceVM();
            }
            VMSnapshotTO target = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(), null, vmSnapshot.getDescription(), false, current, quiescevm);
            if (current == null) {
                vmSnapshotVO.setParent(null);
            } else {
                vmSnapshotVO.setParent(current.getId());
            }
            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(userVm.getInstanceName(), userVm.getUuid(), target, volumeTOs,  guestOS.getDisplayName());
            s_logger.info("Creating VM snapshot for KVM hypervisor without memory");

            List<VolumeInfo> vinfos = new ArrayList<>();
            for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                vinfos.add(volumeDataFactory.getVolume(volumeObjectTO.getId()));
                virtual_size += volumeObjectTO.getSize();
                VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
                prev_chain_size += volumeVO.getVmSnapshotChainSize() == null ? 0 : volumeVO.getVmSnapshotChainSize();
            }

            boolean backupToSecondary = SnapshotManager.BackupSnapshotAfterTakingSnapshot.value() == null || SnapshotManager.BackupSnapshotAfterTakingSnapshot.value();

            if (!backupToSecondary) {
                for (VolumeInfo volumeInfo : vinfos) {
                   isBackupSupported(ccmd, volumeInfo);
                }
            }
            freezeCommand = new FreezeThawVMCommand(userVm.getInstanceName());
            freezeCommand.setOption("freeze");
            freezeAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, freezeCommand);
            startFreeze = System.nanoTime();

            thawCmd = new FreezeThawVMCommand(userVm.getInstanceName());
            if (freezeAnswer != null && freezeAnswer.getResult()) {
                s_logger.info("The virtual machine is frozen");
                try {
                    for (VolumeInfo vol : vinfos) {
                            long startSnapshtot = System.nanoTime();
                            SnapshotInfo snapInfo = createDiskSnapshot(vmSnapshot, forRollback, vol);
                            if (!backupToSecondary && snapInfo != null) {
                                snapInfo.markBackedUp();
                            }
                            if (snapInfo == null) {
                                throw new CloudRuntimeException("Could not take snapshot for volume with id=" + vol.getId());
                            }
                            s_logger.info(String.format("Snapshot with id=%s, took  %s miliseconds", snapInfo
                                                            .getId(), TimeUnit.MILLISECONDS.convert(elapsedTime(startSnapshtot), TimeUnit.NANOSECONDS)));
                    }
                    answer = new CreateVMSnapshotAnswer(ccmd, true, "");
                    answer.setVolumeTOs(volumeTOs);
                    thawCmd.setOption("thaw");
                    thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                    if (thawAnswer != null && thawAnswer.getResult()) {
                        s_logger.info(String.format("Virtual machne is thawed. The freeze of virtual machine took %s miliseconds.", TimeUnit.MILLISECONDS
                                                        .convert(elapsedTime(startFreeze), TimeUnit.NANOSECONDS)));
                        if (backupToSecondary) {
                            for (SnapshotInfo snapshot : forRollback) {
                                backupSnapshot(snapshot, forRollback);
                            }
                        }
                    }
                } catch (CloudRuntimeException e) {
                    throw new CloudRuntimeException(e.getMessage());
                }
            } else {
                throw new CloudRuntimeException("Could not freeze VM." + freezeAnswer.getDetails());
            }
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer);
                s_logger.debug("Create vm snapshot " + vmSnapshot.getName() + " succeeded for vm: " + userVm.getInstanceName());
                result = true;
                long new_chain_size = 0;
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
            throw new CloudRuntimeException(
                    "Creating VM snapshot: " + vmSnapshot.getName() + " failed: " + e.toString());
        } catch (AgentUnavailableException e) {
            s_logger.debug("Creating VM snapshot: " + vmSnapshot.getName() + " failed", e);
            throw new CloudRuntimeException(
                    "Creating VM snapshot: " + vmSnapshot.getName() + " failed: " + e.toString());
        } finally {
            if (thawAnswer == null && (freezeAnswer != null && freezeAnswer.getResult())) {
                s_logger.info(String.format("Freeze of virtual machine took %s miliseconds.", TimeUnit.MILLISECONDS
                                                .convert(elapsedTime(startFreeze), TimeUnit.NANOSECONDS)));
                try {
                    thawAnswer = (FreezeThawVMAnswer) agentMgr.send(hostId, thawCmd);
                } catch (AgentUnavailableException | OperationTimedoutException e) {
                    s_logger.debug("Could not unfreeze the VM. ");
                }
            }
            if (!result) {
                for (SnapshotInfo snapshotInfo : forRollback) {
                    rollbackDiskSnapshot(snapshotInfo);
                }
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
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to change vm snapshot state with event ExpungeRequested");
            throw new CloudRuntimeException(
                    "Failed to change vm snapshot state with event ExpungeRequested: " + e.getMessage());
        }

        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshot.getVmId());

        String vmInstanceName = userVm.getInstanceName();
        VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();
        VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(),
                vmSnapshot.getCreated().getTime(), vmSnapshot.getDescription(), vmSnapshot.getCurrent(), parent, true);
        GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
        DeleteVMSnapshotCommand deleteSnapshotCommand = new DeleteVMSnapshotCommand(vmInstanceName, vmSnapshotTO,
                volumeTOs, guestOS.getDisplayName());

        List<VolumeInfo> volumeInfos = new ArrayList<>();
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            volumeInfos.add(volumeDataFactory.getVolume(volumeObjectTO.getId()));
        }
        for (VolumeInfo vol : volumeInfos) {
            try {
                deleteDiskSnapshot(vmSnapshot, vol);
            } catch (CloudRuntimeException e) {
                throw new CloudRuntimeException("Could not delete snapshot for VM snapshot" + e.getMessage());
            }
        }
        Answer answer = new DeleteVMSnapshotAnswer(deleteSnapshotCommand, volumeTOs);
        if (answer != null && answer.getResult()) {
            DeleteVMSnapshotAnswer deleteVMSnapshotAnswer = (DeleteVMSnapshotAnswer) answer;
            processAnswer(vmSnapshotVO, userVm, answer);
            long full_chain_size = 0;
            for (VolumeObjectTO volumeTo : deleteVMSnapshotAnswer.getVolumeTOs()) {
                publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshot, userVm, volumeTo);
                full_chain_size += volumeTo.getSize();
            }
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshot, userVm, full_chain_size, 0L);
            return true;
        } else {
            String errMsg = (answer == null) ? null : answer.getDetails();
            s_logger.error("Delete vm snapshot " + vmSnapshot.getName() + " of vm " + userVm.getInstanceName()
                    + " failed due to " + errMsg);
            throw new CloudRuntimeException("Delete vm snapshot " + vmSnapshot.getName() + " of vm "
                    + userVm.getInstanceName() + " failed due to " + errMsg);
        }
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshotVO, VMSnapshot.Event.RevertRequested);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        boolean result = false;
        try {
         //   VMSnapshotVO snapshot = vmSnapshotDao.findById(vmSnapshotVO.getId());
            List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            String vmInstanceName = userVm.getInstanceName();
            VMSnapshotTO parent = vmSnapshotHelper.getSnapshotWithParents(vmSnapshotVO).getParent();

            VMSnapshotTO vmSnapshotTO = new VMSnapshotTO(vmSnapshotVO.getId(), vmSnapshotVO.getName(), vmSnapshotVO.getType(),
                                       vmSnapshotVO.getCreated().getTime(), vmSnapshotVO.getDescription(), vmSnapshotVO.getCurrent(), parent, true);
            Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
            GuestOSVO guestOS = guestOSDao.findById(userVm.getGuestOSId());
            RevertToVMSnapshotCommand revertToSnapshotCommand = new RevertToVMSnapshotCommand(vmInstanceName,
                    userVm.getUuid(), vmSnapshotTO, volumeTOs, guestOS.getDisplayName());
            HostVO host = hostDao.findById(hostId);
            GuestOSHypervisorVO guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(),
                    host.getHypervisorType().toString(), host.getHypervisorVersion());
            if (guestOsMapping == null) {
                revertToSnapshotCommand.setPlatformEmulator(null);
            } else {
                revertToSnapshotCommand.setPlatformEmulator(guestOsMapping.getGuestOsName());
            }
            List<VolumeInfo> volumeInfos = new ArrayList<>();
            for (VolumeObjectTO volumeObjectTO : volumeTOs) {
                volumeInfos.add(volumeDataFactory.getVolume(volumeObjectTO.getId()));
            }
            for (VolumeInfo vol : volumeInfos) {
                try {
                    revertDiskSnapshot(vmSnapshot, vol);
                } catch (Exception e) {
                    throw new CloudRuntimeException("Could not revert snapshot for VM snapshot" + e.getMessage());
                }
            }
            RevertToVMSnapshotAnswer answer = new RevertToVMSnapshotAnswer(revertToSnapshotCommand, true, "");
            answer.setVolumeTOs(volumeTOs);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshotVO, userVm, answer);
                result = true;
            } else {
                String errMsg = "Revert VM: " + userVm.getInstanceName() + " to snapshot: " + vmSnapshotVO.getName()
                        + " failed";
                if (answer != null && answer.getDetails() != null)
                    errMsg = errMsg + " due to " + answer.getDetails();
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
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

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    @DB
    protected void processAnswer(final VMSnapshotVO vmSnapshot, UserVm userVm, final Answer as) {
        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<NoTransitionException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws NoTransitionException {
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
                    }else {
                        throw new CloudRuntimeException("processAnswer - Unsupported VMSnapshotAnswer");
                    }
                }
            });
        } catch (Exception e) {
            String errMsg = "Error while process answer: " + as.getClass() + " due to " + e.getMessage();
            s_logger.error(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
       UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
       if ( SnapshotManager.VMsnapshotKVM.value() && userVm.getHypervisorType() == Hypervisor.HypervisorType.KVM
                    && vmSnapshot.getType() == VMSnapshot.Type.Disk) {
           return StrategyPriority.HYPERVISOR;
       }
       return StrategyPriority.CANT_HANDLE;
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

    private long elapsedTime(long startTime) {
        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    //If snapshot.backup.to.secondary is not enabled check if disks are on NFS
    private void isBackupSupported(CreateVMSnapshotCommand ccmd, VolumeInfo volumeInfo) {
        StoragePoolVO storage = storagePool.findById(volumeInfo.getPoolId());
        DataStoreProvider provider = dataStoreProviderMgr.getDefaultPrimaryDataStoreProvider();
        s_logger.info(String.format("Backup to secondary storage is set to false, storagePool=%s, storageProvider=%s ", storage.getPoolType(), provider.getName()));
        if (storage.getStorageProviderName().equals(provider.getName()) && storage.getPoolType() != StoragePoolType.RBD) {
            s_logger.debug("Backup to secondary should be enabled for disks on Default primary datastore provider except RBD");
            throw new CloudRuntimeException("Backup to secondary should be enabled for disks on Default primary datastore provider except RBD");
        }
    }

    //Backup to secondary storage. It is mandatory for storages which are using qemu/libvirt
    protected void backupSnapshot(SnapshotInfo snapshot, List<SnapshotInfo> forRollback) {
        try {
            SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);
            SnapshotInfo snInfo = snapshotStrategy.backupSnapshot(snapshot);
            if (snInfo == null) {
                throw new CloudRuntimeException("Could not backup snapshot for volume");
            }
            s_logger.info(String.format("Backedup snapshot with id=%s, path=%s", snInfo.getId(), snInfo.getPath()));
        } catch (Exception e) {
            forRollback.removeIf(snap -> snap.equals(snapshot));
            throw new CloudRuntimeException("Could not backup snapshot for volume " + e.getMessage());
        }
    }

    //Rollback if one of disks snapshot fails
    protected void rollbackDiskSnapshot(SnapshotInfo snapshotInfo) {
        Long snapshotID = snapshotInfo.getId();
        SnapshotVO snapshot = snapshotDao.findById(snapshotID);
        deleteSnapshotByStrategy(snapshot);
        s_logger.debug("Rollback is executed: deleting snapshot with id:" + snapshotID);
    }

    protected void deleteSnapshotByStrategy(SnapshotVO snapshot) {
        //The snapshot could not be deleted separately, that's why we set snapshot state to BackedUp for operation delete VM snapshots and rollback
        snapshot.setState(Snapshot.State.BackedUp);
        snapshotDao.persist(snapshot);
        SnapshotStrategy strategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.DELETE);
        if (strategy != null) {
            boolean snapshotForDelete = strategy.deleteSnapshot(snapshot.getId());
            if (!snapshotForDelete) {
                throw new CloudRuntimeException("Failed to delete snapshot");
            }
        }
    }

    protected void deleteDiskSnapshot(VMSnapshot vmSnapshot, VolumeInfo vol) {
        //we can find disks snapshots related to vmSnapshot only by virtual machine snapshot's UUID and Volume's UUID
        String snapshotName = vmSnapshot.getUuid() + "_" + vol.getUuid();
        SnapshotVO snapshot = findSnapshotByName(snapshotName);

        if (snapshot == null) {
            throw new CloudRuntimeException("Could not find snapshot for VM snapshot");
        }
        deleteSnapshotByStrategy(snapshot);
    }

    protected void revertDiskSnapshot(VMSnapshot vmSnapshot, VolumeInfo vol) {
        //we can find disks snapshots related to vmSnapshot only by virtual machine snapshot's UUID and Volume's UUID
        String snapshotName = vmSnapshot.getUuid() + "_" + vol.getUuid();
        SnapshotVO snapshotVO = findSnapshotByName(snapshotName);
        Snapshot snapshot= snapshotApiService.revertSnapshot(snapshotVO.getId());
        if (snapshot == null) {
            throw new CloudRuntimeException( "Failed to revert snapshot");
        }
    }

    protected SnapshotInfo createDiskSnapshot(VMSnapshot vmSnapshot, List<SnapshotInfo> forRollback, VolumeInfo vol) {
        String snapshotName = vmSnapshot.getUuid() + "_" + vol.getUuid();
        SnapshotVO createSnapshotInDB = new SnapshotVO(vol.getDataCenterId(), vol.getAccountId(), vol.getDomainId(), vol.getId(), vol.getDiskOfferingId(),
                              snapshotName, (short) SnapshotVO.MANUAL_POLICY_ID,  "MANUAL",  vol.getSize(), vol.getMinIops(),  vol.getMaxIops(), Hypervisor.HypervisorType.KVM, null);
        createSnapshotInDB.setState(Snapshot.State.AllocatedKVM);
        SnapshotVO snapshot = snapshotDao.persist(createSnapshotInDB);
        vol.addPayload(setPayload(vol, snapshot));
        SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshot.getId(), vol.getDataStore());
        snapshotInfo.addPayload(vol.getpayload());
        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshotInfo, SnapshotOperation.TAKE);
        if (snapshotStrategy == null) {
            throw new CloudRuntimeException("Could not find strategy for snapshot uuid:" + snapshotInfo.getUuid());
        }
        snapshotInfo = snapshotStrategy.takeSnapshot(snapshotInfo);
        if (snapshotInfo == null) {
            throw new CloudRuntimeException("Failed to create snapshot");
        } else {
          forRollback.add(snapshotInfo);
        }
        return snapshotInfo;
    }

    protected CreateSnapshotPayload setPayload(VolumeInfo vol, SnapshotVO snapshotCreate) {
        CreateSnapshotPayload payload = new CreateSnapshotPayload();
        payload.setSnapshotId(snapshotCreate.getId());
        payload.setSnapshotPolicyId(SnapshotVO.MANUAL_POLICY_ID);
        payload.setLocationType(snapshotCreate.getLocationType());
        payload.setAccount(accountService.getAccount(vol.getAccountId()));
        payload.setAsyncBackup(false);
        payload.setQuiescevm(false);
        return payload;
    }

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    protected void finalizeDelete(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeTOs) {
        // update volumes paths
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

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
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

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    protected void finalizeRevert(VMSnapshotVO vmSnapshot, List<VolumeObjectTO> volumeToList) {
        updateVolumePath(volumeToList);
        VMSnapshotVO previousCurrent = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshot.getVmId());
        if (previousCurrent != null) {
            previousCurrent.setCurrent(false);
            vmSnapshotDao.persist(previousCurrent);
        }
        vmSnapshot.setCurrent(true);
        vmSnapshotDao.persist(vmSnapshot);
    }

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    protected void updateVolumePath(List<VolumeObjectTO> volumeTOs) {
        for (VolumeObjectTO volume : volumeTOs) {
            if (volume.getPath() != null) {
                VolumeVO volumeVO = volumeDao.findById(volume.getId());
                volumeVO.setPath(volume.getPath());
                volumeVO.setVmSnapshotChainSize(volume.getSize());
                volumeDao.persist(volumeVO);
            }
        }
    }

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    protected void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, VolumeObjectTO volumeTo) {
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
        UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                vmSnapshot.getName(), offeringId, volume.getId(), // save volume's id into templateId field
                volumeTo.getSize(), VMSnapshot.class.getName(), vmSnapshot.getUuid(), details);
    }

    //TODO: maybe this should be moved to another helper class, because the method is the same like DefaultVMSnapshotStrategy
    protected void publishUsageEvent(String type, VMSnapshot vmSnapshot, UserVm userVm, Long vmSnapSize,
            Long virtualSize) {
        try {
            Map<String, String> details = new HashMap<>();
            if (vmSnapshot != null) {
                details.put(UsageEventVO.DynamicParameters.vmSnapshotId.name(), String.valueOf(vmSnapshot.getId()));
            }
            UsageEventUtils.publishUsageEvent(type, vmSnapshot.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                    vmSnapshot.getName(), 0L, 0L, vmSnapSize, virtualSize, VMSnapshot.class.getName(),
                    vmSnapshot.getUuid(), details);
        } catch (Exception e) {
            s_logger.error("Failed to publis usage event " + type, e);
        }
    }

    protected SnapshotVO findSnapshotByName(String snapshotName) {
        SearchBuilder<SnapshotVO> sb = snapshotDao.createSearchBuilder();
        SearchCriteria<SnapshotVO> sc = sb.create();
        sc.addAnd("name", Op.EQ, snapshotName);
        SnapshotVO snap = snapshotDao.findOneBy(sc);
        return snap;
    }
}
