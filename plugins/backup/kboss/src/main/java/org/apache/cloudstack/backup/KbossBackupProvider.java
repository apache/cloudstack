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

package org.apache.cloudstack.backup;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.BackupException;
import com.cloud.utils.exception.BackupProviderException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineManagerImpl;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkConstants;
import com.cloud.vm.VmWorkDeleteBackup;
import com.cloud.vm.VmWorkRestoreBackup;
import com.cloud.vm.VmWorkRestoreVolumeBackupAndAttach;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.VmWorkTakeBackup;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDetailsDao;
import org.apache.cloudstack.backup.dao.InternalBackupDataStoreDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.apache.cloudstack.backup.dao.InternalBackupServiceJobDao;
import org.apache.cloudstack.backup.dao.InternalBackupStoragePoolDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.command.BackupDeleteAnswer;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KbossTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.cloudstack.backup.dao.BackupDetailsDao.BACKUP_HASH;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.CURRENT;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.END_OF_CHAIN;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.IMAGE_STORE_ID;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.ISOLATED;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.PARENT_ID;
import static org.apache.cloudstack.backup.dao.BackupDetailsDao.SCREENSHOT_PATH;

public class KbossBackupProvider extends AdapterBase implements InternalBackupProvider, Configurable {
    protected ConfigKey<Integer> backupChainSize = new ConfigKey<>("Advanced", Integer.class, "backup.chain.size", "8", "Determines the max size of a backup chain." +
            " Currently only used by the KBOSS provider. If cloud admins set it to 1 , all the backups will be full backups. With values lower than 1, the backup chain will be " +
            "unlimited, unless it is stopped by another process. Please note that unlimited backup chains have a higher chance of getting corrupted, as new backups will be" +
            " dependant on all of the older ones.", true, ConfigKey.Scope.Zone);

    protected ConfigKey<Integer> backupTimeout = new ConfigKey<>("Advanced", Integer.class, "kboss.timeout", "43200", "Timeout, in seconds, to execute KBOSS commands. After the " +
            "command times out, the Management Server will still wait for another kboss.timeout seconds to receive a response from the Agent.", true, ConfigKey.Scope.Zone);

    @Inject
    private AsyncJobManager jobManager;
    @Inject
    private EntityManager entityManager;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Inject
    private VMSnapshotHelper vmSnapshotHelper;

    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    private VMSnapshotDao vmSnapshotDao;

    @Inject
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private InternalBackupJoinDao internalBackupJoinDao;

    @Inject
    private BackupDetailsDao backupDetailDao;

    @Inject
    private InternalBackupStoragePoolDao internalBackupStoragePoolDao;

    @Inject
    private InternalBackupDataStoreDao internalBackupDataStoreDao;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    @Inject
    private BackupOfferingDetailsDao backupOfferingDetailsDao;

    @Inject
    private HeuristicRuleHelper heuristicRuleHelper;

    @Inject
    private DataStoreManager dataStoreManager;

    @Inject
    private AgentManager agentManager;

    @Inject
    private EndPointSelector endPointSelector;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private ImageStoreDao imageStoreDao;

    @Inject
    private VolumeApiService volumeApiService;

    @Inject
    private PrimaryDataStoreDao storagePoolDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private UserVmManager userVmManager;

    @Inject
    private VolumeOrchestrationService volumeOrchestrationService;

    @Inject
    private VolumeDataFactory volumeDataFactory;
    @Inject
    private InternalBackupServiceJobDao internalBackupServiceJobDao;

    @Inject
    private BackupManager backupManager;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    @Inject
    private HypervisorGuruManager hypervisorGuruManager;

    @Inject
    private NicDao nicDao;

    @Inject
    private AlertManager alertManager;

    protected final List<Backup.Status> validChildStatesToRemoveBackup = List.of(Backup.Status.Expunged, Backup.Status.Error, Backup.Status.Failed);

    private final List<Storage.StoragePoolType> supportedStoragePoolTypes = List.of(Storage.StoragePoolType.Filesystem, Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.SharedMountPoint);

    private final List<Backup.Status> allowedBackupStatesToRemove = List.of(Backup.Status.BackedUp, Backup.Status.Failed, Backup.Status.Error);

    private final List<Backup.Status> allowedBackupStatesToCompress = List.of(Backup.Status.BackedUp, Backup.Status.Restoring);

    private final List<Backup.Status> allowedBackupStatesToValidate = List.of(Backup.Status.BackedUp, Backup.Status.Restoring);

    private final List<VirtualMachine.State> allowedVmStates = Arrays.asList(VirtualMachine.State.Running, VirtualMachine.State.Stopped);
    @Override
    public String getDescription() {
        return "Native Incremental KVM Backup Plugin";
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        return List.of();
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return true;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        logger.debug("Assigning VM [{}] to KBOSS backup offering with name:[{}], uuid: [{}].", vm.getUuid(), backupOffering.getName(), backupOffering.getUuid());
        if (!Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType())) {
            logger.error("KVM Native Incremental Backup provider is only supported for KVM.");
            return false;
        }

        for (VMSnapshotVO vmSnapshotVO : vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.Disk)) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.listDetails(vmSnapshotVO.getId());
            if (!vmSnapshotDetails.stream().allMatch(vmSnapshotDetailsVO -> vmSnapshotDetailsVO.getName().equals(VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT))) {
                logger.error("KBOSS is only supported with disk-only VM snapshots using [{}] strategy. Found a disk-only VM snapshot using another strategy for the VM.",
                        VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT);
                logger.debug("Found VM snapshot details [{}].", () -> vmSnapshotDetails.stream().map(VMSnapshotDetailsVO::getName).collect(Collectors.toList()));
                return false;
            }
        }

        return CollectionUtils.isEmpty(vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.DiskAndMemory));
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        logger.info("Removing VM [{}] from KBOSS backup offering.", vm.getUuid());

        validateVmState(vm, "remove backup offering", VirtualMachine.State.Expunging, VirtualMachine.State.Destroyed);
        if (endBackupChain(vm)) {
            return true;
        }
        UserVmVO vmVO = userVmDao.findById(vm.getId());
        logger.error("Failed to merge deltas for VM [{}] during backup offering removal process. Changing its state to [{}].", vm, VirtualMachine.State.BackupError);
        vmInstanceDetailsDao.addDetail(vm.getId(), ApiConstants.LAST_KNOWN_STATE, vmVO.getState().name(), false);
        vmVO.setState(VirtualMachine.State.BackupError);
        userVmDao.update(vmVO.getId(), vmVO);

        return false;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public Pair<Boolean, Backup> takeBackup(VirtualMachine vm, Boolean quiesceVm, boolean isolated) {
        logger.debug("Queueing backup on VM [{}].", vm.getUuid());
        Outcome<?> outcome = createBackupThroughJobQueue(vm, ObjectUtils.defaultIfNull(quiesceVm, false), isolated);

        try {
            outcome.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job takeBackup due to [%s]. VM [%s].", e.getMessage(), vm.getUuid()), e);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        if (jobResult instanceof BackupProviderException) {
            throw (BackupProviderException) jobResult;
        } else if (jobResult instanceof Throwable) {
            throw new CloudRuntimeException(String.format("Exception while taking KVM native incremental backup for VM [%s]. Check the logs for more information.", vm.getUuid()));
        }

        Pair<Boolean, Long> result = (Pair<Boolean, Long>)jobResult;
        Pair<Boolean, Backup> returnValue = new Pair<>(result.first(), null);
        if (result.first()) {
            returnValue.second(backupDao.findById(result.second()));
        }
        return returnValue;
    }

    @Override
    public Pair<Boolean, Long> orchestrateTakeBackup(Backup backup, boolean quiesceVm, boolean isolated) {
        BackupVO backupVO = (BackupVO) backup;
        long vmId = backup.getVmId();
        VirtualMachine userVm = virtualMachineManager.findById(vmId);
        Long hostId = vmSnapshotHelper.pickRunningHost(vmId);
        HostVO hostVO = hostDao.findById(hostId);

        if (hostVO.getStatus() != Status.Up || hostVO.getResourceState() != ResourceState.Enabled) {
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);

            logger.error("No available host found to create backup [{}] of VM [{}]. Setting the backup as Failed.", backupVO.getUuid(), userVm.getUuid());
            return new Pair<>(Boolean.FALSE,  backup.getId());
        }

        List<VolumeObjectTO> volumeTOs;
        try {
            validateVmState(userVm, "take backup");
            volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
            validateStorages(volumeTOs, userVm.getUuid());
        } catch (Exception e) {
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);
            throw e;
        }

        logger.info("Starting VM backup process for VM [{}].", userVm.getUuid());

        BackupOfferingVO backupOfferingVO = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());

        backupVO.setDate(new Date());
        List<InternalBackupJoinVO> backupChain = getBackupJoinParents(backupVO, true);
        InternalBackupJoinVO parentBackup = null;
        if (isolated) {
            setBackupAsIsolated(backupVO);
        } else {
            parentBackup = getParentAndSetEndOfChain(backupVO, backupChain, backupOfferingVO);
        }
        InternalBackupJoinVO newBackupJoin = internalBackupJoinDao.findById(backup.getId());
        boolean fullBackup = parentBackup == null;
        List<InternalBackupStoragePoolVO> parentBackupDeltasOnPrimary = new ArrayList<>();
        List<InternalBackupDataStoreVO> parentBackupDeltasOnSecondary = new ArrayList<>();
        List<String> chainImageStoreUrls = null;
        List<KbossTO> kbossTOS = new ArrayList<>();
        HashMap<String, InternalBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef = new HashMap<>();
        HashMap<String, InternalBackupDataStoreVO> volumeUuidToDeltaSecondaryRef = new HashMap<>();

        if (!fullBackup) {
            parentBackupDeltasOnPrimary = internalBackupStoragePoolDao.listByBackupId(parentBackup.getId());
            parentBackupDeltasOnSecondary = internalBackupDataStoreDao.listByBackupId(parentBackup.getId());

            chainImageStoreUrls = getChainImageStoreUrls(backupChain);
        }

        boolean runningVm = userVm.getState() == VirtualMachine.State.Running;
        transitVmStateWithoutThrow(userVm, VirtualMachine.Event.BackupRequested, hostId);
        updateBackupStatusToBackingUp(volumeTOs, backupVO);

        DataStore imageStore = getImageStoreForBackup(userVm.getDataCenterId(), backupVO);
        createBasicBackupDetails(imageStore.getId(), fullBackup ? 0L : parentBackup.getId(), backupVO);

        List<VMSnapshotVO> succeedingVmSnapshotList = getSucceedingVmSnapshotList(parentBackup);
        VMSnapshotVO succeedingVmSnapshot = succeedingVmSnapshotList.isEmpty() ? null : succeedingVmSnapshotList.get(0);

        Map<Long, List<SnapshotDataStoreVO>> volumeIdToSnapshotDataStoreList = mapVolumesToVmSnapshotReferences(volumeTOs, succeedingVmSnapshotList);
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            KbossTO kbossTO = new KbossTO(volumeObjectTO, volumeIdToSnapshotDataStoreList.getOrDefault(volumeObjectTO.getId(), new ArrayList<>()));
            kbossTOS.add(kbossTO);
            createDeltaReferences(fullBackup, !succeedingVmSnapshotList.isEmpty(), runningVm, backup, parentBackupDeltasOnSecondary,
                    parentBackupDeltasOnPrimary, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, succeedingVmSnapshot, kbossTO);
        }

        TakeKbossBackupCommand command = new TakeKbossBackupCommand(quiesceVm, runningVm, newBackupJoin.getEndOfChain(), userVm.getInstanceName(), imageStore.getUri(),
                chainImageStoreUrls, kbossTOS, isolated);

        Answer answer = sendBackupCommand(hostId, command);

        if (answer == null || !answer.getResult()) {
            processBackupFailure(answer, userVm, hostId, runningVm, backupVO);
            return new Pair<>(Boolean.FALSE, null);
        }

        processBackupSuccess(runningVm, volumeTOs, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, (TakeKbossBackupAnswer)answer, parentBackupDeltasOnPrimary,
                succeedingVmSnapshotList, backupVO, fullBackup, userVm, hostId, newBackupJoin.getEndOfChain(), isolated);

        if (!isolated) {
            updateCurrentBackup(newBackupJoin);
        }

        if (offeringSupportsCompression(newBackupJoin)) {
            compressBackupAsync(newBackupJoin, backup.getZoneId(), userVm.getAccountId());
        } else {
            validateBackupAsyncIfHasOfferingSupport(newBackupJoin, backup.getZoneId(), userVm.getAccountId());
        }
        return new Pair<>(Boolean.TRUE, backupVO.getId());
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        logger.debug("Queueing backup [{}] deletion.", backup.getUuid());
        Outcome<Boolean> outcome = deleteBackupThroughJobQueue(backup, forced);

        try {
            outcome.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job deleteBackup due to [%s]. Backup [%s].", e.getMessage(), backup.getUuid()), e);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        if (jobResult instanceof Throwable) {
            if (jobResult instanceof BackupProviderException) {
                throw (BackupProviderException) jobResult;
            }
            throw new CloudRuntimeException(String.format("Exception while deleting KVM native incremental backup [%s]. Check the logs for more information.", backup.getUuid()));
        }

        return BooleanUtils.isTrue((Boolean) jobResult);
    }

    @Override
    public Boolean orchestrateDeleteBackup(Backup backup, boolean forced) {
        BackupVO backupVO = (BackupVO) backup;

        VirtualMachine virtualMachine = virtualMachineManager.findById(backup.getVmId());

        if (virtualMachine != null) {
            validateVmState(virtualMachine, "delete backup", VirtualMachine.State.Destroyed);
        }

        logger.info("Starting delete process for backup [{}].", backupVO);

        if (!validateBackupStateForRemoval(backupVO.getId())) {
            return false;
        }

        checkErrorBackup(backupVO, virtualMachine);
        if (deleteFailedBackup(backupVO)) {
            return true;
        }

        InternalBackupJoinVO childBackup = internalBackupJoinDao.findByParentId(backup.getId());

        if (childBackup != null && !validChildStatesToRemoveBackup.contains(childBackup.getStatus())) {
            logger.debug("Backup [{}] has children that are not in one of the following states [{}]; will mark it as removed on the database but the files will not be deleted " +
                    "from secondary storage until the children are also expunged.", backup.getUuid(), validChildStatesToRemoveBackup);
            backupVO.setStatus(Backup.Status.Removed);
            backupDao.update(backupVO.getId(), backupVO);
            return true;
        }

        InternalBackupJoinVO backupJoinVO = internalBackupJoinDao.findById(backup.getId());
        if (backupJoinVO.getCurrent()) {
            if (!mergeCurrentBackupDeltas(backupJoinVO)) {
                return false;
            }
            InternalBackupJoinVO parent = internalBackupJoinDao.findById(backupJoinVO.getParentId());
            if (parent != null && parent.getStatus() == Backup.Status.BackedUp) {
                backupDetailDao.persist(new BackupDetailVO(parent.getId(), END_OF_CHAIN, Boolean.TRUE.toString(), false));
            }
        }

        Commands deleteCommands = new Commands(Command.OnError.Continue);

        DataStore dataStore = addBackupDeltasToDeleteCommand(backup.getId(), deleteCommands);
        Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> backupParentsToBeRemovedAndLastAliveBackup = getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVO,
                deleteCommands);

        EndPoint endPoint = endPointSelector.select(dataStore);
        if (endPoint == null) {
            logger.error("Unable to find SSVM to delete backup [{}]. Check if SSVM is up for the zone.", backup);
            throw new CloudRuntimeException(String.format("Unable to delete backup [%s]. Please check the logs.", backup.getUuid()));
        }
        Answer[] deleteAnswers;
        try {
            deleteAnswers = sendBackupCommands(endPoint.getId(), deleteCommands);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(e);
        }

        List<Long> removedBackupIds = backupParentsToBeRemovedAndLastAliveBackup.first().stream().map(InternalBackupJoinVO::getId).collect(Collectors.toList());
        removedBackupIds.add(backup.getId());

        boolean isFailedSetEmpty = processRemoveBackupFailures(forced, deleteAnswers, removedBackupIds, backupJoinVO);

        processRemovedBackups(removedBackupIds);

        if (backupParentsToBeRemovedAndLastAliveBackup.second() != null) {
            backupDetailDao.persist(new BackupDetailVO(backupParentsToBeRemovedAndLastAliveBackup.second().getId(), END_OF_CHAIN, Boolean.TRUE.toString(), false));
        }

        return isFailedSetEmpty;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup, boolean quickRestore, Long hostId) {
        logger.debug("Queueing backup [{}] restore for VM [{}].", backup.getUuid(), vm.getUuid());
        validateQuickRestore(backup, quickRestore);

        Outcome<Boolean> outcome = restoreVMFromBackupThroughJobQueue(vm, backup, quickRestore, hostId);

        try {
            outcome.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job restoreVMFromBackup due to [%s]. Backup [%s].", e.getMessage(), backup.getUuid()), e);
        } finally {
            BackupVO backupVO = backupDao.findById(backup.getId());
            backupVO.setStatus(Backup.Status.BackedUp);
            backupDao.update(backupVO.getId(), backupVO);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        handleRestoreException(backup, vm, jobResult);

        return BooleanUtils.isTrue((Boolean) jobResult);
    }

    @Override
    public Boolean orchestrateRestoreVMFromBackup(Backup backup, VirtualMachine vm, boolean quickRestore, Long hostId, boolean sameVmAsBackup) {
        logger.info("Starting restore backup process for VM [{}] and backup [{}].", vm.getUuid(), backup);
        validateNoVmSnapshots(vm);
        long backupId = backup.getId();
        Pair<Boolean, BackupVO> isValidStateAndBackupVo = validateCompressionStateForRestoreAndGetBackup(backupId);

        if (!isValidStateAndBackupVo.first()) {
            return false;
        }

        InternalBackupJoinVO backupJoinVO = internalBackupJoinDao.findById(backupId);
        InternalBackupJoinVO currentBackup = sameVmAsBackup ? internalBackupJoinDao.findCurrent(vm.getId()) : null;
        List<InternalBackupStoragePoolVO> deltasOnPrimary = new ArrayList<>();
        if (currentBackup != null) {
            deltasOnPrimary = internalBackupStoragePoolDao.listByBackupId(currentBackup.getId());
        }
        List<InternalBackupDataStoreVO> deltasOnSecondary = internalBackupDataStoreDao.listByBackupId(backupId);
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vm.getId());

        Set<BackupDeltaTO> deltasToRemove = new HashSet<>();

        List<InternalBackupDataStoreVO> backupsWithoutVolumes = sameVmAsBackup ? getBackupsWithoutVolumes(deltasOnSecondary, volumeTOs) : List.of();

        HostVO host;
        try {
            host = getHostToRestore(vm, quickRestore, hostId);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException(e);
        }

        BackupVO backupVO = isValidStateAndBackupVo.second();
        List<Backup.VolumeInfo> volumeInfos = backupVO.getBackedUpVolumes();
        if (sameVmAsBackup) {
            createAndAttachVolumes(volumeInfos, backupsWithoutVolumes, vm, host);
            // Get new volume references
            volumeTOs = vmSnapshotHelper.getVolumeTOList(vm.getId());
        }

        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs = generateBackupAndVolumePairsToRestore(deltasOnSecondary, volumeTOs, backupJoinVO, sameVmAsBackup);

        List<DeltaMergeTreeTO> deltasToBeMerged = List.of();
        if (sameVmAsBackup) {
            List<VolumeObjectTO> volumesNotPartOfTheBackup = getVolumesThatAreNotPartOfTheBackup(volumeTOs, deltasOnSecondary);
            deltasToBeMerged = populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(deltasOnPrimary, deltasToRemove, volumeTOs, volumesNotPartOfTheBackup,
                    vm.getUuid());
        }
        Set<String> secondaryStorageUrls = getParentSecondaryStorageUrls(backupVO);

        Commands commands = new Commands(Command.OnError.Stop);
        commands.addCommand(new RestoreKbossBackupCommand(deltasToRemove, backupAndVolumePairs, secondaryStorageUrls, quickRestore));
        commands.addCommand(new MergeDiskOnlyVmSnapshotCommand(deltasToBeMerged, vm.getState().equals(VirtualMachine.State.Running), vm.getInstanceName()));

        Answer[] answers;

        try {
            answers = sendBackupCommands(host.getId(), commands);
        } catch (OperationTimedoutException | AgentUnavailableException e) {
            throw new CloudRuntimeException(e);
        }

        if (answers == null) {
            logger.error("Failed to restore backup [{}] due to no answer from host.", backup);
            return false;
        }

        if (!processRestoreAnswers(vm, answers, quickRestore)) {
            return false;
        }

        updateVolumePathsAndSizeIfNeeded(vm, volumeTOs, volumeInfos, deltasToBeMerged, sameVmAsBackup);

        if (currentBackup != null) {
            internalBackupStoragePoolDao.expungeByBackupId(currentBackup.getId());
            setEndOfChainAndRemoveCurrentForBackup(currentBackup);
        }

        if (quickRestore) {
            List<VolumeInfo> volumesToConsolidate = getVolumesToConsolidate(vm, deltasOnSecondary, volumeTOs, host.getId(), sameVmAsBackup);
            return finalizeQuickRestore(vm, volumesToConsolidate, host.getId());
        }

        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, String dataStoreUuid,
            Pair<String, VirtualMachine.State> vmNameAndState, VirtualMachine vm, boolean quickRestore) {
        logger.debug("Queueing backup [{}] volume [{}] restore for VM [{}].", backup.getUuid(), backupVolumeInfo, vm.getUuid());
        validateQuickRestore(backup, quickRestore);
        Outcome<Boolean> outcome = restoreBackedUpVolumeThroughJobQueue(vm, backup, backupVolumeInfo, hostIp, quickRestore);

        try {
            outcome.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job restoreBackedUpVolume due to [%s]. Backup [%s].", e.getMessage(), backup.getUuid()), e);
        } finally {
            BackupVO backupVO = backupDao.findById(backup.getId());
            backupVO.setStatus(Backup.Status.BackedUp);
            backupDao.update(backupVO.getId(), backupVO);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        handleRestoreException(backup, vm, jobResult);

        if (!(jobResult instanceof Pair)) {
            throw new CloudRuntimeException(String.format("Unexpected answer from restoreBackupVolume job. Got [%s].", jobResult));
        }
        return (Pair<Boolean, String>) jobResult;
    }

    @Override
    public Pair<Boolean, String> orchestrateRestoreBackedUpVolume(Backup backup, VirtualMachine vm, Backup.VolumeInfo backupVolumeInfo, String hostIp, boolean quickRestore) {
        BackupVO backupVO = (BackupVO) backup;
        Pair<Boolean, BackupVO> isValidStateAndBackupVo = validateCompressionStateForRestoreAndGetBackup(backup.getId());

        if (!isValidStateAndBackupVo.first()) {
            return new Pair<>(false, null);
        }

        VolumeVO backedUpVolume = volumeDao.findByUuidIncludingRemoved(backupVolumeInfo.getUuid());
        HostVO hostVo = hostDao.findByIp(hostIp);
        VolumeInfo volumeInfo = duplicateAndCreateVolume(vm, hostVo, backupVolumeInfo);

        VolumeObjectTO volumeObjectTO = (VolumeObjectTO) volumeInfo.getTO();
        InternalBackupDataStoreVO deltaOnSecondary = internalBackupDataStoreDao.findByBackupIdAndVolumeId(backup.getId(), backedUpVolume.getId());
        InternalBackupJoinVO internalBackupJoinVO = internalBackupJoinDao.findById(backup.getId());
        Pair<BackupDeltaTO, VolumeObjectTO> backupAndVolumePair = generateBackupAndVolumePairForSingleNewVolume(deltaOnSecondary, volumeObjectTO, internalBackupJoinVO);
        Set<String> secondaryStorageUrls = getParentSecondaryStorageUrls(backupVO);

        RestoreKbossBackupCommand cmd = new RestoreKbossBackupCommand(Set.of(), Set.of(backupAndVolumePair), secondaryStorageUrls, quickRestore);

        Answer answer = sendBackupCommand(hostVo.getId(), cmd);

        if (!processRestoreAnswers(vm, new Answer[] {answer}, quickRestore)) {
            throw new CloudRuntimeException("Bad answer from agent");
        }

        VolumeVO newVolume = (VolumeVO)volumeInfo.getVolume();
        volumeDao.update(newVolume.getId(), newVolume);

        Volume attachedVolume = volumeApiService.attachVolumeToVM(vm.getId(), newVolume.getId(), null, false, false);

        if (quickRestore) {
            ArrayList<VolumeInfo> volumeToConsolidate = new ArrayList<>();
            volumeToConsolidate.add(volumeDataFactory.getVolume(attachedVolume.getId()));
            return new Pair<>(finalizeQuickRestore(vm, volumeToConsolidate, hostVo.getId()), attachedVolume.getUuid());
        }

        return new Pair<>(true, attachedVolume.getUuid());
    }

    @Override
    public boolean startBackupCompression(long backupId, long hostId) {
        Pair<Boolean, BackupVO> validCompressAndBackupVO = validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);

        if (!validCompressAndBackupVO.first()) {
            return false;
        }

        InternalBackupJoinVO backup = internalBackupJoinDao.findById(backupId);
        InternalBackupJoinVO parentBackup = internalBackupJoinDao.findById(backup.getParentId());

        List<InternalBackupDataStoreVO> backupDeltas = internalBackupDataStoreDao.listByBackupId(backupId);
        List<InternalBackupDataStoreVO> parentBackupDeltas = parentBackup != null ? internalBackupDataStoreDao.listByBackupId(backup.getParentId()) : List.of();

        DataStoreTO imageStoreTo = dataStoreManager.getDataStore(backup.getImageStoreId(), DataStoreRole.Image).getTO();
        DataStoreTO parentStoreTo = parentBackup != null ? dataStoreManager.getDataStore(parentBackup.getImageStoreId(), DataStoreRole.Image).getTO() : null;

        List<DeltaMergeTreeTO> deltasToCompressAndParents = new ArrayList<>();
        for (InternalBackupDataStoreVO delta : backupDeltas) {
            BackupDeltaTO backupDeltaTO = new BackupDeltaTO(imageStoreTo, Hypervisor.HypervisorType.KVM, delta.getBackupPath());
            InternalBackupDataStoreVO parentDataStore = parentBackupDeltas.stream().filter(parent -> parent.getVolumeId() == delta.getVolumeId()).findFirst().orElse(null);
            BackupDeltaTO parentDeltaTO = parentDataStore != null ? new BackupDeltaTO(parentStoreTo, Hypervisor.HypervisorType.KVM, parentDataStore.getBackupPath()) : null;
            deltasToCompressAndParents.add(new DeltaMergeTreeTO(null, parentDeltaTO, backupDeltaTO, null));
        }

        HostVO hostVO = hostDao.findById(hostId);
        BackupVO backupVO = validCompressAndBackupVO.second();

        long minFreeStorage = Math.round(backupVO.getSize() * backupCompressionMinimumFreeStorage.valueIn(hostVO.getDataCenterId()));

        BackupOfferingVO backupOfferingVO = backupOfferingDao.findByIdIncludingRemoved(backupVO.getBackupOfferingId());
        BackupOfferingDetailsVO detail = backupOfferingDetailsDao.findDetail(backupOfferingVO.getId(), ApiConstants.COMPRESSION_LIBRARY);
        List<InternalBackupJoinVO> backupChain = getBackupJoinParents(backupVO, true);
        List<String> chainImageStoreUrls = getChainImageStoreUrls(backupChain);
        CompressBackupCommand cmd = new CompressBackupCommand(deltasToCompressAndParents, chainImageStoreUrls, minFreeStorage, detail == null ? null :
                Backup.CompressionLibrary.valueOf(detail.getValue()), backupCompressionCoroutines.valueIn(hostVO.getClusterId()),
                backupCompressionRateLimit.valueIn(hostVO.getClusterId()));
        cmd.setWait(backupCompressionTimeout.valueIn(hostVO.getClusterId()));
        Answer answer = agentManager.easySend(hostId, cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("Failed to compress backup [{}] due to {}.", backup.getUuid(), answer == null ? "no answer" : answer.getDetails());
            backupVO.setCompressionStatus(Backup.CompressionStatus.CompressionError);
            backupDao.update(backupId, backupVO);
            return false;
        }

        logger.info("Successfully completed the first step of the backup compression process for backup [{}]. Will launch a new compression job to finalize the compression.",
                backup.getUuid());

        internalBackupServiceJobDao.persist(new InternalBackupServiceJobVO(backupVO.getId(), backupVO.getZoneId(), backupVO.getVmId(), backupVO.getAccountId(),
                InternalBackupServiceJobType.FinalizeCompression));

        return true;
    }

    @Override
    public boolean finalizeBackupCompression(long backupId, long hostId) {
        Pair<Boolean, BackupVO> shouldContinueProcessAndBackupVo = validateBackupStateForFinalizeCompression(backupId);
        if (!shouldContinueProcessAndBackupVo.first()) {
            return false;
        }
        BackupVO backupVO = shouldContinueProcessAndBackupVo.second();

        List<BackupDeltaTO> deltaTOs = getBackupDeltaTOList(backupId);

        FinalizeBackupCompressionCommand cmd = new FinalizeBackupCompressionCommand(backupVO.getStatus() != Backup.Status.BackedUp, deltaTOs);
        HostVO hostVO = hostDao.findById(hostId);
        cmd.setWait(backupCompressionTimeout.valueIn(hostVO.getClusterId()));
        Answer answer = agentManager.easySend(hostId, cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("Failed to finish compression of backup [{}] due to {}.", backupVO.getUuid(), answer == null ? "no answer" : answer.getDetails());
            backupVO.setCompressionStatus(Backup.CompressionStatus.CompressionError);
            backupDao.update(backupId, backupVO);
            return false;
        }

        if (cmd.isCleanup()) {
            logger.info("Successfully cleaned up backup compression of backup [{}].", backupVO);
            return true;
        }

        backupVO.setCompressionStatus(Backup.CompressionStatus.Compressed);
        backupVO.setUncompressedSize(backupVO.getSize());
        backupVO.setSize(Long.parseLong(answer.getDetails()));
        backupDao.update(backupVO.getId(), backupVO);

        logger.info("Finalized compression for backup [{}], old size was [{}], compressed size is [{}].", backupVO.getUuid(), backupVO.getUncompressedSize(), backupVO.getSize());

        validateBackupAsyncIfHasOfferingSupport(internalBackupJoinDao.findById(backupId), backupVO.getZoneId(), backupVO.getAccountId());
        return true;
    }

    @Override
    public boolean validateBackup(long backupId, long hostId) {
        if (!validateBackupStateForValidation(backupId)) {
            return false;
        }
        BackupVO backupVO = backupDao.findById(backupId);
        backupVO.setValidationStatus(Backup.ValidationStatus.Validating);
        backupDao.update(backupId, backupVO);
        BackupDetailVO hashDetail = backupDetailDao.findDetail(backupId, BACKUP_HASH);
        if (hashDetail != null) {
            return validateWithHash(backupId, backupVO, hashDetail);
        } else {
            return validateWithValidationVm(backupId, hostId, backupVO);
        }
    }

    @Override
    public Pair<Boolean, String> restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid, boolean quickRestore) {
        Pair<Boolean, Backup.Status> shouldRestoreAndOldStatus = validateBackupStateForRestoreBackupToVM(backup.getId());
        if (!shouldRestoreAndOldStatus.first()) {
            return new Pair<>(false, "Backup is not in the right state.");
        }

        boolean result = false;
        try {
            result = orchestrateRestoreVMFromBackup(backup, vm, quickRestore, null, false);
        } catch (Exception exception) {
            handleRestoreException(backup, vm, exception);
        } finally {
            Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Boolean>) transactionStatus -> {
                BackupVO backupVO = backupDao.findById(backup.getId());
                backupVO.setStatus(shouldRestoreAndOldStatus.second());
                backupDao.update(backupVO.getId(), backupVO);
                return true;
            });
        }

        return new Pair<>(result, null);
    }

    @Override
    public boolean finishBackupChain(VirtualMachine virtualMachine) {
        UserVmVO userVmVO = userVmDao.findById(virtualMachine.getId());
        if (allowedVmStates.contains(userVmVO.getState())) {
            return endBackupChain(userVmVO);
        }
        if (userVmVO.getState() != VirtualMachine.State.BackupError) {
            logger.error("VM [{}] is not in the right state to finish backup chain. It can only be in states [Running, Stopped and BackupError].", userVmVO.getUuid());
            return false;
        }
        return normalizeBackupErrorAndFinishChain(userVmVO);
    }

    @Override
    public void syncBackupMetrics(Long zoneId) {
    }

    @Override
    public Backup createNewBackupEntryForRestorePoint(Backup.RestorePoint rp, VirtualMachine vm) {
        return null;
    }

    @Override
    public Pair<Long, Long> getBackupStorageStats(Long zoneId) {
        return new Pair<>(0L, 0L);
    }

    @Override
    public void syncBackupStorageStats(Long zoneId) {
    }

    @Override
    public boolean supportsInstanceFromBackup() {
        return true;
    }

    @Override
    public boolean supportsMemoryVmSnapshot() {
        return false;
    }

    @Override
    public void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine) {
        logger.info("Preparing volume [{}] for detach.", volume.getUuid());
        mergeCurrentDeltaIntoVolume(volume, virtualMachine, "detach", virtualMachine.getState().equals(VirtualMachine.State.Running));
    }

    @Override
    public void prepareVolumeForMigration(Volume volume, VirtualMachine vm) {
        if (VirtualMachine.State.Migrating.equals(vm.getState())) {
            logger.info("Preparing volume [{}] for live migration.", volume.getUuid());
            mergeCurrentDeltaIntoVolume(volume, vm, "live migration", true);
        }
    }

    @Override
    public void updateVolumeId(VirtualMachine virtualMachine, long oldVolumeId, long newVolumeId) {
        internalBackupDataStoreDao.updateVolumeId(oldVolumeId, newVolumeId);
    }

    @Override
    public void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot, VirtualMachine virtualMachine) {
        InternalBackupJoinVO currentBackup = internalBackupJoinDao.findCurrent(virtualMachine.getId());

        if (currentBackup == null) {
            logger.debug("There is no current backup delta, the VM [{}] is already prepared for VM snapshot revert.", virtualMachine.getUuid());
            return;
        }
        if (currentBackup.getDate().before(vmSnapshot.getCreated())) {
            logger.debug("The current backup delta was taken before [{}] the VM snapshot being reverted [{}], no need to prepare the VM.", currentBackup.getDate(),
                    vmSnapshot.getCreated());
            return;
        }

        logger.debug("Preparing VM [{}] for VM snapshot reversion.", virtualMachine.getUuid());

        List<VolumeObjectTO> volumeObjectTOs = vmSnapshotHelper.getVolumeTOList(virtualMachine.getId());
        VMSnapshotVO vmSnapshotSucceedingCurrentBackup = getSucceedingVmSnapshot(currentBackup);

        List<DeltaMergeTreeTO> deltaMergeTreeTOList = new ArrayList<>();
        Commands commands = new Commands(Command.OnError.Stop);
        List<InternalBackupStoragePoolVO> deletedDeltas = new ArrayList<>();

        createDeleteCommandsAndMergeTrees(volumeObjectTOs, commands, deletedDeltas, vmSnapshotSucceedingCurrentBackup, deltaMergeTreeTOList);

        if (!deltaMergeTreeTOList.isEmpty()) {
            commands.addCommand(new MergeDiskOnlyVmSnapshotCommand(deltaMergeTreeTOList, false, virtualMachine.getInstanceName()));
        }

        Long hostId = vmSnapshotHelper.pickRunningHost(virtualMachine.getId());

        Answer[] answers;
        try {
            answers = sendBackupCommands(hostId, commands);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(e);
        }

        if (answers == null || Arrays.stream(answers).anyMatch(answer -> !answer.getResult())) {
            logger.error("Error while trying to prepare VM [{}] for VM snapshot reversion. Got [{}] as answers from host.", virtualMachine.getUuid(),
                    answers != null ? Arrays.stream(answers).filter(answer -> !answer.getResult()).map(Answer::getDetails) : null);
            throw new CloudRuntimeException(String.format("Unable to prepare VM [%s] for VM snapshot reversion.", virtualMachine.getUuid()));
        }

        List<SnapshotDataStoreVO> snapRefsSucceedingCurrentBackup = new ArrayList<>();

        if (vmSnapshotSucceedingCurrentBackup != null) {
            snapRefsSucceedingCurrentBackup = vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshotSucceedingCurrentBackup.getId());
        }

        updateReferencesAfterPrepareForSnapshotRevert(deltaMergeTreeTOList, snapRefsSucceedingCurrentBackup, deletedDeltas, currentBackup);
    }

    /**
     * Get the secondary storage URLs of the backups that are backing files of this VM. This is only useful for Validation VMs currently, which are created with backing files on
     * the secondary storage.
     * */
    @Override
    public Set<String> getSecondaryStorageUrls(UserVm userVm) {
        VMInstanceDetailVO detailVO = vmInstanceDetailsDao.findDetail(userVm.getId(), ApiConstants.BACKUP_ID);
        if (detailVO == null) {
            return Set.of();
        }
        BackupVO backupVO = backupDao.findByUuid(detailVO.getValue());
        Set<String> secondaryStorageUrls = getParentSecondaryStorageUrls(backupVO);
        InternalBackupJoinVO internalBackupJoinVO = internalBackupJoinDao.findById(backupVO.getId());
        secondaryStorageUrls.add(imageStoreDao.findById(internalBackupJoinVO.getImageStoreId()).getUrl());
        return secondaryStorageUrls;
    }

    @Override
    public Boolean crossZoneInstanceCreationEnabled(BackupOffering backupOffering) {
        return false;
    }

    @Override
    public List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        return null;
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupChainSize, backupTimeout, backupCompressionTimeout, backupCompressionMinimumFreeStorage, backupCompressionRateLimit,
                backupCompressionCoroutines};
    }

    protected Outcome<?> createBackupThroughJobQueue(VirtualMachine vm, boolean quiesceVm, boolean isolated) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        long vmId = vm.getId();

        BackupVO backup = new BackupVO(String.format("%s-%s", vm.getHostName(), DateUtil.getDateInSystemTimeZone()), vmId, vm.getBackupOfferingId(), accountId,
                vm.getDomainId(), vm.getDataCenterId(), 0, Backup.Status.Queued, null);

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkTakeBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkTakeBackup workInfo = new VmWorkTakeBackup(userId, accountId, vmId, backupDao.persist(backup).getId(), VM_WORK_JOB_HANDLER, quiesceVm, isolated);

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        jobManager.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vmId);
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new OutcomeImpl<>(Pair.class, workJob, VirtualMachineManagerImpl.VmJobCheckInterval.value(), new Predicate() {
            @Override
            public boolean checkCondition() {
                AsyncJobVO jobVo = entityManager.findById(AsyncJobVO.class, workJob.getId());
                return jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS;
            }
        }, AsyncJob.Topics.JOB_STATE);
    }

    protected Outcome<Boolean> deleteBackupThroughJobQueue(Backup backup, boolean forced) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        VirtualMachine userVm = userVmDao.findByIdIncludingRemoved(backup.getVmId());
        long vmId = userVm.getId();

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkDeleteBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkDeleteBackup workInfo = new VmWorkDeleteBackup(userId, accountId, vmId, VM_WORK_JOB_HANDLER, backup.getId(), forced);

        return submitWorkJob(workJob, workInfo, vmId);
    }

    protected Outcome<Boolean> restoreVMFromBackupThroughJobQueue(VirtualMachine vm, Backup backup, boolean quickRestore, Long hostId) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        long vmId = vm.getId();

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkRestoreBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkRestoreBackup workInfo = new VmWorkRestoreBackup(userId, accountId, vmId, VM_WORK_JOB_HANDLER, backup.getId(), quickRestore, hostId);

        return submitWorkJob(workJob, workInfo, vmId);
    }

    protected Outcome<Boolean> restoreBackedUpVolumeThroughJobQueue(VirtualMachine vm, Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, boolean quickRestore) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        long vmId = vm.getId();

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkRestoreVolumeBackupAndAttach.class.getName(), vmId,
                VirtualMachine.Type.Instance, VmWorkJobVO.Step.Starting);
        VmWorkRestoreVolumeBackupAndAttach workInfo = new VmWorkRestoreVolumeBackupAndAttach(userId, accountId, vmId, VM_WORK_JOB_HANDLER, backup.getId(),
                backupVolumeInfo, hostIp, quickRestore);

        return submitWorkJob(workJob, workInfo, vmId);
    }

    protected OutcomeImpl<Boolean> submitWorkJob(VmWorkJobVO workJob, VmWork workInfo, long vmId) {
        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        jobManager.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vmId);
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new OutcomeImpl<>(Boolean.class, workJob, VirtualMachineManagerImpl.VmJobCheckInterval.value(), new Predicate() {
            @Override
            public boolean checkCondition() {
                AsyncJobVO jobVo = entityManager.findById(AsyncJobVO.class, workJob.getId());
                return jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS;
            }
        }, AsyncJob.Topics.JOB_STATE);
    }

    protected void validateBackupAsyncIfHasOfferingSupport(InternalBackupJoinVO backupJoinVO, long zoneId, long accountId) {
        if (!offeringSupportsValidation(backupJoinVO)) {
            return;
        }

        logger.info("Queuing backup validation job for backup [{}].", backupJoinVO.getUuid());
        internalBackupServiceJobDao.persist(new InternalBackupServiceJobVO(backupJoinVO.getId(), zoneId, backupJoinVO.getVmId(), accountId, InternalBackupServiceJobType.BackupValidation));
    }

    protected void compressBackupAsync(InternalBackupJoinVO backupJoinVO, long zoneId, long accountId) {
        logger.info("Queuing backup compression job for backup [{}].", backupJoinVO.getUuid());
        internalBackupServiceJobDao.persist(new InternalBackupServiceJobVO(backupJoinVO.getId(), zoneId, backupJoinVO.getVmId(), accountId, InternalBackupServiceJobType.StartCompression));
    }

    protected boolean finalizeQuickRestore(VirtualMachine vm, List<VolumeInfo> volumesToConsolidate, long hostId) {
        logger.info("Finalizing quick restore for VM [{}].", vm.getUuid());

        UserVmVO userVmVO = userVmDao.findById(vm.getId());
        if (userVmVO.getState() == VirtualMachine.State.Stopped) {
            try {
                logger.info("Starting VM [{}] as part of the quick restore process.", vm.getName());
                userVmManager.startVirtualMachine(userVmVO.getId(), hostId, new HashMap<>(), null, true);
            } catch (Exception e) {
                logger.error("Caught [{}] while trying to quick restore VM [{}]. Throwing BackupException.", e, vm);
                throw new BackupException(String.format("Exception while trying to start VM [%s] as part of the quick restore process.", userVmVO.getUuid()), e, false);
            }
        }

        return consolidateVolumes(vm, hostId, volumesToConsolidate);
    }

    protected boolean validateWithHash(long backupId, BackupVO backupVO, BackupDetailVO hashDetail) {
        List<BackupDeltaTO> backupDeltaTOList = getBackupDeltaTOList(backupId);
        TakeBackupHashCommand hashCommand = new TakeBackupHashCommand(backupDeltaTOList, backupVO.getUuid());
        List<HostVO> hosts = hostDao.listAllHostsUpByZoneAndHypervisor(backupVO.getZoneId(), Hypervisor.HypervisorType.KVM);
        String message;
        if (CollectionUtils.isEmpty(hosts)) {
            message = String.format("No Up and Enabled host found in zone [%s]. Cannot validate backup [%s]. Will try again later.", backupVO.getZoneId(), backupVO.getUuid());
            logger.error(message);
            setBackupUnableToValidateAndSendAlert(backupVO, message);
            return false;
        }
        Collections.shuffle(hosts);
        Answer answer = sendBackupCommand(hosts.get(0).getId(), hashCommand);
        if (!answer.getResult()) {
            message = String.format("Unable to get hash of backup [%s] due to [%s]. Will try again later.", backupVO.getUuid(), answer.getDetails());
            logger.warn(message);
            setBackupUnableToValidateAndSendAlert(backupVO, message);
            return false;
        }

        if (!hashDetail.getValue().equals(answer.getDetails())) {
            message = String.format("Current xxHash128 of backup [%s] is different from previous validated hash. This backup has changed and might be corrupt." +
                    "The old hash is [%s]; the new hash is [%s].", backupVO.getUuid(), hashDetail.getValue(), answer.getDetails());
            logger.error(message);
            setBackupAsInvalidAndSendAlert(backupVO, message);
            return false;
        }

        logger.info("xxHash128 of backup [{}] is the same as when it was validated. This backup is still valid.", backupVO.getUuid());
        backupVO.setValidationStatus(Backup.ValidationStatus.Valid);
        backupDao.update(backupId, backupVO);
        return true;
    }

    protected boolean validateWithValidationVm(long backupId, long hostId, BackupVO backupVO) {
        UserVmVO validationVm;
        validationVm = allocateValidationVm(backupId, backupVO);
        if (validationVm == null) {
            return false;
        }

        HostVO hostVo = hostDao.findById(hostId);

        List<VolumeObjectTO> volumeToList = new ArrayList<>();
        boolean startedVm = false;
        boolean validationPrepared = false;
        VirtualMachineTO vmTO = null;
        List<VolumeVO> volumeVOs = volumeDao.findByInstance(validationVm.getId());
        try {
            createValidationVolumesOnPrimaryStorage(volumeVOs, validationVm, backupVO, hostVo, volumeToList);

            List<InternalBackupDataStoreVO> backupDeltas = internalBackupDataStoreDao.listByBackupId(backupId);
            InternalBackupJoinVO backupJoinVO = internalBackupJoinDao.findById(backupId);
            Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupDeltaAndVolumePairs = generateBackupAndVolumePairsToRestore(backupDeltas, volumeToList, backupJoinVO, false);
            if (!prepareForValidation(hostId, backupDeltaAndVolumePairs, backupVO, validationVm)) {
                return false;
            }
            validationPrepared = true;

            userVmManager.startVirtualMachine(validationVm, null);
            startedVm = true;
            //refresh info
            validationVm = userVmDao.findById(validationVm.getId());
            hostVo = hostDao.findById(validationVm.getHostId());

            HypervisorGuru hvGuru = hypervisorGuruManager.getGuru(validationVm.getHypervisorType());
            VirtualMachineProfileImpl profile = new VirtualMachineProfileImpl(validationVm);
            vmTO = hvGuru.implement(profile);

            if (!validateBackup(backupId, vmTO, backupDeltaAndVolumePairs, backupVO, validationVm, hostVo)) {
                endBackupChainIfConfigured(backupVO);
                return false;
            }
            calculateAndSaveHash(backupDeltaAndVolumePairs, backupVO, hostVo.getId());
            return true;
        } catch (Exception ex) {
            logger.error("Encountered an exception during the validation process of backup [{}]. Will cleanup now.", backupVO.getUuid(), ex);
            setBackupUnableToValidateAndSendAlert(backupVO, "Failed to validate due to unexpected exception: " + ex.getMessage());
            return false;
        } finally {
            cleanupValidation(startedVm, validationVm, backupVO, volumeVOs, validationPrepared);
        }
    }

    /**
     * If backupValidationEndChainOnFail is true for the account, and the backup being validated is part of the current chain, we end the current chain.
     * */

    protected void endBackupChainIfConfigured(BackupVO backupVO) {
        if (!getValidationEndChainOnFail(backupVO)) {
            return;
        }
        List<InternalBackupJoinVO> backupChildren = getBackupJoinChildren(backupVO);

        // Get updated record
        InternalBackupJoinVO backupJoinVO = internalBackupJoinDao.findById(backupVO.getId());
        if (backupJoinVO.getCurrent() || (!backupChildren.isEmpty() && backupChildren.get(backupChildren.size() -1).getCurrent())) {
            logger.info("As [{}] is true, we are ending the backup chain for VM [{}]. The next backup will be a full backup.",
                    BackupValidationServiceController.backupValidationEndChainOnFail.toString());
            endBackupChain(userVmDao.findById(backupVO.getVmId()));
        }
    }

    /**
     * This method was created to facilitate testing
     * */
    protected Boolean getValidationEndChainOnFail(BackupVO backupVO) {
        return BackupValidationServiceController.backupValidationEndChainOnFail.valueIn(backupVO.getAccountId());
    }

    protected boolean normalizeBackupErrorAndFinishChain(UserVmVO userVmVO) {
        VMInstanceDetailVO detail = vmInstanceDetailsDao.findDetail(userVmVO.getId(), ApiConstants.LAST_KNOWN_STATE);
        boolean runningVM = detail == null || VirtualMachine.State.valueOf(detail.getValue()) == VirtualMachine.State.Running;

        BackupVO backupVO = backupDao.findLatestByStatusAndVmId(Backup.Status.Error, userVmVO.getId());
        InternalBackupJoinVO internalBackupJoinVO = internalBackupJoinDao.findById(backupVO.getId());
        ImageStoreVO imageStoreVO = imageStoreDao.findById(internalBackupJoinVO.getImageStoreId());

        List<KbossTO> kbossTOS = new ArrayList<>();
        List<InternalBackupStoragePoolVO> deltasOnPrimary = internalBackupStoragePoolDao.listByBackupId(internalBackupJoinVO.getId());
        InternalBackupJoinVO parent = internalBackupJoinDao.findById(internalBackupJoinVO.getParentId());

        // There is a possibility that the cleanup step of the backup creation was executed, and thus we would have to merge with the old parent's parent
        List<InternalBackupStoragePoolVO> parentDeltasOnPrimary = new ArrayList<>();
        if (parent != null) {
            parentDeltasOnPrimary = internalBackupStoragePoolDao.listByBackupId(parent.getId());
        }

        List<InternalBackupDataStoreVO> deltasOnSecondary = internalBackupDataStoreDao.listByBackupId(internalBackupJoinVO.getId());
        configureKbossTosForCleanup(userVmVO, deltasOnPrimary, deltasOnSecondary, runningVM, parentDeltasOnPrimary, kbossTOS);
        CleanupKbossBackupErrorCommand command = new CleanupKbossBackupErrorCommand(runningVM, userVmVO.getInstanceName(), imageStoreVO.getUrl(), kbossTOS);

        long hostId = userVmVO.getHostId() != null ? userVmVO.getHostId() : vmSnapshotHelper.pickRunningHost(userVmVO.getId());
        Answer answer = sendBackupCommand(hostId, command);
        if (answer == null || !answer.getResult()) {
            logger.error("Unable to finish backup chain for VM [{}]. The host [{}] logs will have more information on why this happened.", userVmVO.getUuid(), hostId);
            return false;
        }

        boolean chainAlreadyEnded = processCleanupBackupErrorAnswer(userVmVO, answer);

        if (!chainAlreadyEnded) {
            return endBackupChain(userVmVO);
        }
        InternalBackupJoinVO current = internalBackupJoinDao.findCurrent(userVmVO.getId());
        internalBackupStoragePoolDao.expungeByBackupId(current.getId());
        setEndOfChainAndRemoveCurrentForBackup(current);

        return true;
    }

    protected boolean processCleanupBackupErrorAnswer(UserVmVO userVmVO, Answer answer) {
        boolean runningVM;
        CleanupKbossBackupErrorAnswer cleanAnswer = (CleanupKbossBackupErrorAnswer) answer;
        logger.info("Successfully finished chain for VM [{}] and normalizing the BackupError state. Cleaning up metadata.", userVmVO.getUuid());

        boolean chainAlreadyEnded = false;
        for (VolumeObjectTO volumeObjectTO : cleanAnswer.getVolumeObjectTos()) {
            VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
            if (!volumeObjectTO.getPath().equals(volumeVO.getPath())) {
                volumeVO.setPath(volumeObjectTO.getPath());
                volumeDao.update(volumeVO.getId(), volumeVO);
                chainAlreadyEnded = true;
            }
        }

        runningVM = cleanAnswer.isVmRunning();
        userVmVO.setState(runningVM ? VirtualMachine.State.Running : VirtualMachine.State.Stopped);
        userVmDao.update(userVmVO.getId(), userVmVO);
        vmInstanceDetailsDao.removeDetail(userVmVO.getId(), ApiConstants.LAST_KNOWN_STATE);
        return chainAlreadyEnded;
    }

    protected void calculateAndSaveHash(Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupDeltaAndVolumePairs, BackupVO backupVO, long hostId) {
        TakeBackupHashCommand cmd = new TakeBackupHashCommand(backupDeltaAndVolumePairs.stream().map(Pair::first).collect(Collectors.toList()), backupVO.getUuid());
        Answer answer = sendBackupCommand(hostId, cmd);

        if (answer.getResult() && answer.getDetails() != null) {
            logger.debug("Got xxHash128 [{}] of backup [{}].", answer.getDetails(), backupVO.getUuid());
            backupDetailDao.addDetail(backupVO.getId(), BackupDetailsDao.BACKUP_HASH, answer.getDetails(), false);
            return;
        }
        logger.warn("Unable to get hash of backup [{}] due to [{}].", backupVO.getUuid(), answer.getDetails());
    }


    /**
     * Return a list of BackupDeltaTO of the given backup.
     * */
    protected List<BackupDeltaTO> getBackupDeltaTOList(long backupId) {
        InternalBackupJoinVO backupJoinVO = internalBackupJoinDao.findById(backupId);
        DataStoreTO imageStoreTo = dataStoreManager.getDataStore(backupJoinVO.getImageStoreId(), DataStoreRole.Image).getTO();
        List<InternalBackupDataStoreVO> deltas = internalBackupDataStoreDao.listByBackupId(backupId);

        return deltas.stream()
                .map(delta -> new BackupDeltaTO(imageStoreTo, Hypervisor.HypervisorType.KVM, delta.getBackupPath()))
                .collect(Collectors.toList());
    }

    protected void cleanupValidation(boolean startedVm, UserVmVO validationVm, BackupVO backupVO, List<VolumeVO> volumeVOs, boolean validationPrepared) {
        if (startedVm) {
            userVmManager.stopVirtualMachine(validationVm.getId(), true);
        }
        DestroyVMCmd destroyVMCmd = new DestroyVMCmd(validationVm.getId(), true);
        StringBuilder errorMessage = new StringBuilder("Cleanup failed due to:");
        boolean sendMail = false;
        try {
            userVmManager.destroyVm(destroyVMCmd, false);
        } catch (Exception e) {
            errorMessage.append("\nGot an unexpected exception while trying to destroy validation VM.");
            sendMail = true;
            logger.error("Got an error while trying to cleanup validation of backup [{}].", backupVO.getUuid(), e);
        }
        for (VolumeVO volume : volumeVOs) {
            if (volume.getVolumeType() == Volume.Type.ROOT) {
                continue;
            }
            Volume vol = volumeApiService.destroyVolume(volume.getId(), CallContext.current().getCallingAccount(), true, true, null);
            if (vol == null) {
                sendMail = true;
                errorMessage.append(String.format("\nWe were unable to destroy volume [%s].", volume.getUuid()));
            }
        }

        if (validationPrepared) {
            CleanupKbossValidationCommand cleanupKbossValidationCommand = new CleanupKbossValidationCommand(validationVm.getName(), getSecondaryStorageUrls(validationVm));
            Answer answer = agentManager.easySend(validationVm.getHostId(), cleanupKbossValidationCommand);
            if (answer == null || !answer.getResult()) {
                logger.error("Failed to cleanup post validation of backup [{}]. Got answer [{}]", backupVO.getUuid(), answer == null ? null : answer.getDetails());
                HostVO host = hostDao.findById(validationVm.getHostId());
                sendMail = true;
                errorMessage.append(String.format("\nFailed to cleanup secondary storage mount at host [%s].", host.getUuid()));
            }
        }

        if (sendMail) {
            sendCleanupFailedEmail(backupVO, errorMessage.toString());
        }
    }

    protected boolean validateBackup(long backupId, VirtualMachineTO vmTO, Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupDeltaAndVolumePairs, BackupVO backupVO, UserVmVO validationVm,
            HostVO hostVo) {
        Answer answer;
        ValidateKbossVmCommand validateKbossVmCommand = new ValidateKbossVmCommand(vmTO, backupDeltaAndVolumePairs.stream().findFirst().get().first());
        configureValidationSteps(validateKbossVmCommand, backupVO);
        answer = agentManager.easySend(validationVm.getHostId(), validateKbossVmCommand);

        boolean result = processValidationAnswer(answer, backupVO, validationVm, hostVo, validateKbossVmCommand);
        if (result) {
            backupVO.setValidationStatus(Backup.ValidationStatus.Valid);
            backupDao.update(backupId, backupVO);
        }
        return result;
    }

    protected boolean prepareForValidation(long hostId, Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupDeltaAndVolumePairs, BackupVO backupVO, UserVmVO validationVm) {
        PrepareValidationCommand prepareCommand = new PrepareValidationCommand(new ArrayList<>(backupDeltaAndVolumePairs), getParentSecondaryStorageUrls(backupVO));

        Answer answer = agentManager.easySend(hostId, prepareCommand);

        if (answer == null || !answer.getResult()) {
            String msg = String.format("Failed to prepare dummy VM [%s] for validation of %s. %s", validationVm.getName(), backupVO.getUuid(), answer != null ?
                    "Details: "+ answer.getDetails(): "");
            logger.error(msg);
            setBackupUnableToValidateAndSendAlert(backupVO, msg);
            return false;
        }
        return true;
    }

    protected void createValidationVolumesOnPrimaryStorage(List<VolumeVO> volumeVOs, UserVmVO validationVm, BackupVO backupVO, HostVO hostVo, List<VolumeObjectTO> volumeToList) throws NoTransitionException {
        for (VolumeVO volume : volumeVOs) {
            VolumeInfo volumeInfo = volumeDataFactory.getVolume(volume.getId());
            volumeInfo = volumeOrchestrationService.createVolumeOnPrimaryStorage(validationVm, volumeInfo, Hypervisor.HypervisorType.KVM, null, hostVo.getClusterId(),
                    hostVo.getPodId());
            validateCorrectStorageType(backupVO, volume, volumeInfo);
            volumeToList.add((VolumeObjectTO)volumeInfo.getTO());
        }
    }

    protected UserVmVO allocateValidationVm(long backupId, BackupVO backupVO) {
        UserVmVO validationVm;
        try {
            validationVm = (UserVmVO) userVmManager.allocateVMForValidation(backupId, Hypervisor.HypervisorType.KVM);
            NicVO nic = nicDao.findDefaultNicForVM(validationVm.getId());
            virtualMachineManager.updateVmNic(validationVm, nic, false);
            validationVm.setDataCenterId(backupVO.getZoneId());
        } catch (InsufficientCapacityException | ResourceAllocationException | ResourceUnavailableException e) {
            String msg = String.format("Unable to allocate dummy VM to validate %s due to %s.", backupVO.getUuid(), e.getMessage());
            logger.error(msg, e);
            setBackupUnableToValidateAndSendAlert(backupVO, msg);
            return null;
        }
        return validationVm;
    }

    protected List<VolumeInfo> getVolumesToConsolidate(VirtualMachine vm, List<InternalBackupDataStoreVO> deltasOnSecondary, List<VolumeObjectTO> volumeObjectTOS, long hostId,
            boolean sameVmAsBackup) {
        List<VolumeInfo> volumesToConsolidate = new ArrayList<>();

        transitVmStateWithoutThrow(vm, VirtualMachine.Event.RestoringSuccess, hostId);
        for (VolumeObjectTO volume : volumeObjectTOS) {
            VolumeInfo volumeInfo = volumeDataFactory.getVolume(volume.getVolumeId());
            transitVolumeStateWithoutThrow(volumeInfo.getVolume(), Volume.Event.RestoreSucceeded);

            if (!sameVmAsBackup || deltasOnSecondary.stream().anyMatch(delta -> delta.getVolumeId() == volume.getVolumeId())) {
                volumesToConsolidate.add(volumeInfo);
            }
        }
        return volumesToConsolidate;
    }

    protected boolean consolidateVolumes(VirtualMachine vm, long hostId, List<VolumeInfo> volumesToConsolidate) {
        for (VolumeInfo volumeInfo : volumesToConsolidate) {
            transitVolumeStateWithoutThrow(volumeInfo.getVolume(), Volume.Event.ConsolidationRequested);
        }

        VMInstanceDetailVO uuids = vmInstanceDetailsDao.findDetail(vm.getId(), VmDetailConstants.LINKED_VOLUMES_SECONDARY_STORAGE_UUIDS);
        List<String> secondaryStorageUuids = uuids != null ? List.of(uuids.getValue().split(",")) : List.of();
        ConsolidateVolumesCommand cmd = new ConsolidateVolumesCommand(volumesToConsolidate, secondaryStorageUuids, vm.getInstanceName());
        Answer answer = sendBackupCommand(hostId, cmd);

        String logError = String.format("Failed to consolidate volumes [%s] of VM [%s]. Answer details: [%s].",
                volumesToConsolidate, vm.getName(), answer != null ? answer.getDetails() : "null");
        if (!(answer instanceof ConsolidateVolumesAnswer)) {
            logger.error(logError);
            throw new BackupException(logError, false);
        }
        ConsolidateVolumesAnswer cAnswer = (ConsolidateVolumesAnswer)answer;
        processConsolidateAnswer(cAnswer, volumesToConsolidate, vm);

        logger.info("Volume consolidation answer: [{}].", cAnswer.getResult());
        return cAnswer.getResult();
    }

    /**
     * Validates the Backup status:<br/>
     * - If it is Error and The VM is in BackupError, will throw an exception;<br/>
     * - If it is in Error but the VM is not in BackupError, will set the backup as Failed so that it may be removed with {@code deleteFailedBackup(BackupVO backupVO)};<br/>
     * - If it is not in Error, does nothing.
     * */
    protected void checkErrorBackup(BackupVO backupVO, VirtualMachine virtualMachine) {
        if (backupVO.getStatus() != Backup.Status.Error) {
            return;
        }
        if (virtualMachine != null && virtualMachine.getState() == VirtualMachine.State.BackupError) {
            logger.error("Unable to delete backup [{}] as it is in Error state and the associated VM [{}] is in BackupError state. You must read the backup creation logs," +
                    " normalize the VM's volumes in the hypervisor/storage and update the VM state in the database before trying to delete the backup. Try again when the VM is not " +
                    "in this state.", backupVO, virtualMachine.getUuid());
            throw new InvalidParameterValueException(String.format("Unable to delete backup [%s]. Please check the logs.", backupVO.getUuid()));
        }
        logger.debug("Assuming VM and storage are normalized and setting backup [{}] as failed so its metadata is deleted.");
        backupVO.setStatus(Backup.Status.Failed);
    }

    /**
     * Deletes a Failed backup metadata and sets the backup as Expunged.
     * */
    protected boolean deleteFailedBackup(BackupVO backupVO) {
        if (backupVO.getStatus() == Backup.Status.Failed) {
            long backupId = backupVO.getId();

            backupVO.setStatus(Backup.Status.Expunged);
            backupDao.update(backupId, backupVO);
            internalBackupStoragePoolDao.expungeByBackupId(backupId);
            internalBackupDataStoreDao.expungeByBackupId(backupId);
            backupDetailDao.removeDetails(backupId);
            return true;
        }
        return false;
    }

    /**
     * Merges the current delta on primary storage, if any, into the given volume. If the backup has no more deltas on primary storage, will set the backup as end_of_chain.
     * */
    protected void mergeCurrentDeltaIntoVolume(Volume volume, VirtualMachine virtualMachine, String operation, boolean isVmRunning) {
        InternalBackupStoragePoolVO delta = internalBackupStoragePoolDao.findOneByVolumeId(volume.getId());
        if (delta == null) {
            logger.debug("Volume [{}] has no deltas to merge, doing nothing.", volume.getUuid());
            return;
        }
        InternalBackupJoinVO internalBackupJoinVO = internalBackupJoinDao.findById(delta.getBackupId());
        VMSnapshotVO succeedingVmSnapshotVO = getSucceedingVmSnapshot(internalBackupJoinVO);

        DataStore store = dataStoreManager.getDataStore(volume.getPoolId(), DataStoreRole.Primary);
        VolumeObject volumeObject = VolumeObject.getVolumeObject(store, (VolumeVO)volume);

        DeltaMergeTreeTO deltaMergeTreeTO = createDeltaMergeTree(succeedingVmSnapshotVO == null, isVmRunning, delta, (VolumeObjectTO)volumeObject.getTO(), succeedingVmSnapshotVO);
        MergeDiskOnlyVmSnapshotCommand cmd = new MergeDiskOnlyVmSnapshotCommand(List.of(deltaMergeTreeTO), isVmRunning, virtualMachine.getInstanceName());

        Answer answer = sendBackupCommand(vmSnapshotHelper.pickRunningHost(virtualMachine.getId()), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("Error while trying to prepare volume [{}] for {}. Got [{}] as answer from host.", volume.getUuid(), operation, answer != null ? answer.getDetails() : null);
            throw new CloudRuntimeException(String.format("Unable to prepare volume [%s] for [%s].", volume.getUuid(), operation));
        }

        if (succeedingVmSnapshotVO == null) {
            VolumeVO volumeVO = volumeDao.findById(volumeObject.getId());
            volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
            volumeDao.update(volumeVO.getId(), volumeVO);
        }

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(List.of(delta), succeedingVmSnapshotVO);

        List<InternalBackupStoragePoolVO> backupDeltas = internalBackupStoragePoolDao.listByBackupId(delta.getBackupId());
        if (backupDeltas.isEmpty()) {
            logger.debug("Backup [{}] has no more deltas on primary storage due to prepare volume [{}] for {} operation. Will set it as end of chain and not current.",
                    internalBackupJoinVO.getUuid(), volume.getUuid(), operation);
            setEndOfChainAndRemoveCurrentForBackup(internalBackupJoinVO);
        }
    }

    /**
     * Creates the necessary delta references on both primary and secondary storage. Also maps the volume to the parent delta backup and create the delta merge tree.
     * */
    protected void createDeltaReferences(boolean fullBackup, boolean hasVmSnapshotSucceedingLastBackup, boolean runningVm, Backup backup,
            List<InternalBackupDataStoreVO> parentBackupDeltasOnSecondary, List<InternalBackupStoragePoolVO> parentBackupDeltasOnPrimary,
            HashMap<String, InternalBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef, HashMap<String, InternalBackupDataStoreVO> volumeUuidToDeltaSecondaryRef,
            VMSnapshotVO succeedingVmSnapshot, KbossTO kbossTO) {
        VolumeObjectTO volumeObjectTO = kbossTO.getVolumeObjectTO();
        logger.debug("Creating delta references for backup [{}] of volume [{}].", backup.getUuid(), volumeObjectTO.getUuid());

        String filename = UUID.randomUUID().toString();
        String relativePathOnSecondary = String.format("%s%s%s%s%s%s%s", "backups", File.separator, volumeObjectTO.getAccountId(), File.separator, volumeObjectTO.getId(),
                File.separator, filename);
        kbossTO.setDeltaPathOnPrimary(filename);
        kbossTO.setDeltaPathOnSecondary(relativePathOnSecondary);

        InternalBackupDataStoreVO deltaSecondaryRef = new InternalBackupDataStoreVO(backup.getId(), volumeObjectTO.getVolumeId(), volumeObjectTO.getDeviceId(), relativePathOnSecondary);
        if (!fullBackup) {
            InternalBackupStoragePoolVO parentDeltaOnPrimary = createDeltaMergeTreeForVolume(false, runningVm, parentBackupDeltasOnPrimary, succeedingVmSnapshot, kbossTO);
            findAndSetParentBackupPath(parentBackupDeltasOnSecondary, parentDeltaOnPrimary, kbossTO);
        }

        InternalBackupDataStoreVO referenceOnSecondary = internalBackupDataStoreDao.persist(deltaSecondaryRef);
        logger.trace("Created reference [{}] for backup [{}] of volume [{}].", referenceOnSecondary, backup, volumeObjectTO);
        volumeUuidToDeltaSecondaryRef.put(volumeObjectTO.getUuid(), referenceOnSecondary);

        InternalBackupStoragePoolVO deltaPrimaryRef = new InternalBackupStoragePoolVO(backup.getId(), volumeObjectTO.getPoolId(), volumeObjectTO.getVolumeId(), filename,
                volumeObjectTO.getPath());

        if (kbossTO.getDeltaMergeTreeTO() != null && !hasVmSnapshotSucceedingLastBackup) {
            deltaPrimaryRef.setBackupDeltaParentPath(kbossTO.getDeltaMergeTreeTO().getParent().getPath());
        } else if (hasVmSnapshotSucceedingLastBackup) {
            deltaPrimaryRef.setBackupDeltaParentPath(volumeObjectTO.getPath());
        }

        InternalBackupStoragePoolVO referenceOnPrimary = internalBackupStoragePoolDao.persist(deltaPrimaryRef);
        logger.trace("Created reference [{}] for backup [{}] of volume [{}].", referenceOnPrimary, backup, volumeObjectTO);
        volumeUuidToDeltaPrimaryRef.put(volumeObjectTO.getUuid(), referenceOnPrimary);
    }

    protected HostVO getHostToRestore(VirtualMachine vm, boolean quickRestore, Long hostId) throws AgentUnavailableException {
        HostVO host;
        if (quickRestore) {
            if (hostId == null) {
                hostId = vm.getLastHostId();
            }
            if (hostId == null) {
                logger.error("Cannot quick restore if the VM has no last host and no hostId was informed. You may try to start it in an available host and stop it before quick" +
                        " restoring. Otherwise, use the normal restore.");
                throw new AgentUnavailableException(String.format("No host found to quick restore VM [%s]. Please check the logs.", vm.getUuid()), -1);
            }
            host = hostDao.findByIdIncludingRemoved(hostId);
            if (host.getStatus() != Status.Up || host.isInMaintenanceStates() || host.getResourceState() != ResourceState.Enabled) {
                logger.error("Cannot quick restore if the VM's last host is in maintenance, not Up, or disabled. You may try to start it in an available host and stop it before quick" +
                        " restoring. Otherwise, use the normal restore.");
                throw new AgentUnavailableException(String.format("No host found to quick restore VM [%s]. Please check the logs.", vm.getUuid()), -1);
            }
        } else {
            hostId = vmSnapshotHelper.pickRunningHost(vm.getId());
            host = hostDao.findByIdIncludingRemoved(hostId);
        }
        return host;
    }

    /**
     * Returns ordered list of disk-only VM snapshots taken after the last backup. The list is ordered from oldest to newest.
     * */
    protected List<VMSnapshotVO> getSucceedingVmSnapshotList(InternalBackupJoinVO backup) {
        List<VMSnapshotVO> vmSnapshotVOs = new ArrayList<>();
        if (backup == null) {
            return vmSnapshotVOs;
        }

        VMSnapshotVO currentSnapshotVO = vmSnapshotDao.findCurrentSnapshotByVmId(backup.getVmId());
        if (currentSnapshotVO == null || currentSnapshotVO.getCreated().before(backup.getDate())) {
            return vmSnapshotVOs;
        }
        vmSnapshotVOs.add(0, currentSnapshotVO);

        while (currentSnapshotVO.getParent() != null && currentSnapshotVO.getParent() != 0) {
            VMSnapshotVO parentSnap = vmSnapshotDao.findById(currentSnapshotVO.getParent());
            if (parentSnap.getCreated().before(backup.getDate())){
                break;
            }
            currentSnapshotVO = parentSnap;
            vmSnapshotVOs.add(0, currentSnapshotVO);
        }

        logger.debug("Found the following VM snapshots that succeed the backup [{}]: [{}].", backup.getUuid(), vmSnapshotVOs);

        return vmSnapshotVOs;
    }

    /**
     * Returns the disk-only VM snapshot taken after the last backup, if any.
     * */
    protected VMSnapshotVO getSucceedingVmSnapshot(InternalBackupJoinVO backup) {
        List<VMSnapshotVO> snaps = getSucceedingVmSnapshotList(backup);
        if (snaps.isEmpty()) {
            return null;
        }
        return snaps.get(0);
    }

    /**
     * Given a VM snapshot, returns a map of volume id to list of snapshot references of the children of the VM snapshot.
     * */
    protected Map<Long, List<SnapshotDataStoreVO>> gatherSnapshotReferencesOfChildrenSnapshot(List<VolumeObjectTO> volumeObjectTOs, VMSnapshot vmSnapshotVO) {
        Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs = new HashMap<>();

        if (vmSnapshotVO == null) {
            return volumeToSnapshotRefs;
        }

        List<VMSnapshotVO> snapshotChildren = vmSnapshotDao.listByParent(vmSnapshotVO.getId());

        if (CollectionUtils.isEmpty(snapshotChildren)) {
            return volumeToSnapshotRefs;
        }

        List<SnapshotDataStoreVO> snapshotDataStoreVOS = new ArrayList<>();
        snapshotChildren.stream()
                .map(snapshotVo -> vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(snapshotVo.getId()))
                .forEach(snapshotDataStoreVOS::addAll);

        mapVolumesToSnapshotReferences(volumeObjectTOs, snapshotDataStoreVOS, volumeToSnapshotRefs);

        if (logger.isDebugEnabled()) {
            StringBuilder log = new StringBuilder(String.format("Found the following snapshot references that succeed the VM snapshot [%s].", vmSnapshotVO.getUuid()));
            for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
                log.append(String.format(" Volume [%s]; Snapshot references [%s].", volumeObjectTO.getUuid(), volumeToSnapshotRefs.get(volumeObjectTO.getId())));
            }
            logger.debug(log.toString());
        }

        return volumeToSnapshotRefs;
    }

    /**
     * Given a list of volumes and VM snapshots, maps the volumes to the snapshot references of the VM snapshots.
     * */
    protected Map<Long, List<SnapshotDataStoreVO>> mapVolumesToVmSnapshotReferences(List<VolumeObjectTO> volumeObjectTOs, List<VMSnapshotVO> vmSnapshotVOList) {
        Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs = new HashMap<>();
        if (vmSnapshotVOList.isEmpty()) {
            logger.trace("No VM snapshot to map to any volume, returning.");
            return volumeToSnapshotRefs;
        }

        ArrayList<SnapshotDataStoreVO> allRefs = new ArrayList<>();
        for (VMSnapshotVO vmSnapshotVO : vmSnapshotVOList) {
            allRefs.addAll(vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshotVO.getId()));
        }
        mapVolumesToSnapshotReferences(volumeObjectTOs, allRefs, volumeToSnapshotRefs);
        logger.trace("Given volume objects [{}] and VM snapshots [{}], created the following map [{}].", volumeObjectTOs, vmSnapshotVOList, volumeToSnapshotRefs);
        return volumeToSnapshotRefs;
    }

    protected void mapVolumesToSnapshotReferences(List<VolumeObjectTO> volumeObjectTOs, List<SnapshotDataStoreVO> snapshotDataStoreVOS, Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs) {
        for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
            List<SnapshotDataStoreVO> associatedSnapshots = snapshotDataStoreVOS.stream()
                    .filter(snapRef -> Objects.equals(snapRef.getVolumeId(), volumeObjectTO.getVolumeId()))
                    .collect(Collectors.toList());
            volumeToSnapshotRefs.put(volumeObjectTO.getId(), associatedSnapshots);
        }
    }

    /**
     * Updates the necessary references on the database. Also calculates the backup's physical size.
     * */
    protected long updateDeltaReferencesAndCalculateBackupPhysicalSize(VolumeObjectTO volumeObjectTO, HashMap<String, InternalBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef,
            HashMap<String, InternalBackupDataStoreVO> volumeUuidToDeltaSecondaryRef, TakeKbossBackupAnswer answer, long physicalBackupSize, boolean endChain, boolean isolated,
            BackupVO backupVO) {
        String volumeUuid = volumeObjectTO.getUuid();
        InternalBackupStoragePoolVO deltaPrimaryRef = volumeUuidToDeltaPrimaryRef.get(volumeUuid);
        if (endChain || isolated) {
            logger.trace("Since backup [{}] is [{}]. We will delete the delta reference on primary at [{}] as it does not exist anymore.", backupVO.getUuid(), endChain ?
                    "end of chain" : "isolated", deltaPrimaryRef.getBackupDeltaPath());
            internalBackupStoragePoolDao.expunge(deltaPrimaryRef.getId());
        }

        InternalBackupDataStoreVO deltaSecondaryRef = volumeUuidToDeltaSecondaryRef.get(volumeUuid);

        String newVolumePath = answer.getMapVolumeUuidToNewVolumePath().get(volumeUuid);

        VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
        volumeVO.setPath(newVolumePath);
        logger.trace("Updating volume [{}] path to [{}].", volumeVO.getUuid(), newVolumePath);
        volumeDao.update(volumeVO.getId(), volumeVO);

        Pair<String, Long> deltaPathOnSecondaryAndSize = answer.getMapVolumeUuidToDeltaPathOnSecondaryAndSize().get(volumeUuid);
        logger.trace("Updating delta reference on secondary [{}] path to [{}].", deltaSecondaryRef, deltaPathOnSecondaryAndSize.first());
        deltaSecondaryRef.setBackupPath(deltaPathOnSecondaryAndSize.first());
        internalBackupDataStoreDao.update(deltaSecondaryRef.getId(), deltaSecondaryRef);

        physicalBackupSize += deltaPathOnSecondaryAndSize.second();
        return physicalBackupSize;
    }

    /**
     * Expunge the old backup deltas and if there were disk-only VM snapshot deltas after the last backup, update their paths.
     * */
    protected void expungeOldDeltasAndUpdateVmSnapshotIfNeeded(List<InternalBackupStoragePoolVO> oldDeltasOnPrimary, VMSnapshot vmSnapshot) {
        List<SnapshotDataStoreVO> snapshotRefs = vmSnapshot == null ? List.of() : vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshot.getId());
        for (InternalBackupStoragePoolVO oldBackupDelta : oldDeltasOnPrimary) {
            logger.trace("Expunging old backup delta [{}].", oldBackupDelta);
            internalBackupStoragePoolDao.expunge(oldBackupDelta.getId());
            SnapshotDataStoreVO snapshotDataStoreVO = snapshotRefs.stream().filter(ref -> ref.getVolumeId() == oldBackupDelta.getVolumeId()).findFirst().orElse(null);
            if (snapshotDataStoreVO == null) {
                continue;
            }
            snapshotDataStoreVO.setInstallPath(oldBackupDelta.getBackupDeltaParentPath());
            logger.debug("Updating snapshot delta [{}] path to [{}].", snapshotDataStoreVO.getId(), oldBackupDelta.getBackupDeltaParentPath());
            snapshotDataStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);
        }
    }

    /**
     * Create a {@link DeltaMergeTreeTO} for the volume if it has a delta on primary and add it to the list.
     *
     * @return the delta on primary of the volume. Null if no delta.
     * */
    protected InternalBackupStoragePoolVO createDeltaMergeTreeForVolume(boolean childIsVolume, boolean runningVm, List<InternalBackupStoragePoolVO> deltasOnPrimary, VMSnapshotVO succeedingVmSnapshot,
            KbossTO kbossTO) {
        VolumeObjectTO volumeObjectTO = kbossTO.getVolumeObjectTO();

        InternalBackupStoragePoolVO deltaOnPrimary = deltasOnPrimary.stream()
                .filter(delta -> delta.getVolumeId() == volumeObjectTO.getVolumeId())
                .findFirst()
                .orElse(null);
        if (deltaOnPrimary == null) {
            logger.debug("Volume [{}] has no delta on primary storage.", volumeObjectTO);
            return null;
        }

        logger.debug("Volume [{}] has a backup delta on primary storage [{}].", volumeObjectTO.getUuid(), deltaOnPrimary);

        kbossTO.setDeltaMergeTreeTO(createDeltaMergeTree(childIsVolume, runningVm, deltaOnPrimary, volumeObjectTO, succeedingVmSnapshot));
        return deltaOnPrimary;
    }

    protected DeltaMergeTreeTO createDeltaMergeTree(boolean childIsVolume, boolean runningVm, InternalBackupStoragePoolVO deltaOnPrimary,
            VolumeObjectTO volumeObjectTO, VMSnapshotVO succeedingVmSnapshot) {
        DataStore store = dataStoreManager.getDataStore(deltaOnPrimary.getStoragePoolId(), DataStoreRole.Primary);
        DataTO deltaChild;
        if (childIsVolume) {
            deltaChild = volumeObjectTO;
        } else {
            deltaChild = new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaPath());
        }

        BackupDeltaTO deltaParent = new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaParentPath());

        List<String> succeedingDeltaPaths = new ArrayList<>();
        if (succeedingVmSnapshot != null) {
            succeedingDeltaPaths = gatherSnapshotReferencesOfChildrenSnapshot(List.of(volumeObjectTO), succeedingVmSnapshot).getOrDefault(volumeObjectTO.getVolumeId(), List.of())
                    .stream().map(SnapshotDataStoreVO::getInstallPath).collect(Collectors.toList());

            if (!childIsVolume && !runningVm && succeedingDeltaPaths.isEmpty()) {
                succeedingDeltaPaths = List.of(volumeObjectTO.getPath());
                logger.debug("Since the last backup delta of volume [{}] is succeeded by a snapshot and the delta created by this snapshot is also the volume, it will have to be" +
                        " rebased. Setting it as the grand-child.", volumeObjectTO.getUuid());
            }
        }



        List<DataTO> deltaGrandchildren = succeedingDeltaPaths.stream()
                .map(deltaPath -> new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaPath))
                .collect(Collectors.toList());

        DeltaMergeTreeTO deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, deltaParent, deltaChild, deltaGrandchildren);

        logger.debug("Mapped the following delta merge tree for volume [{}]: [{}].", volumeObjectTO.getUuid(), deltaMergeTreeTO);
        return deltaMergeTreeTO;
    }

    /**
     * Sets on the {@code kbossTO} the backupParentOnSecondary path based on the list of InternalBackupDataStoreVO.
     *
     * @param parentBackupDeltasOnSecondary
     *         List of deltas on secondary;
     * @param parentDeltaOnPrimary
     * @param kbossTO
     *         KbossTO to be configured;
     */
    protected void findAndSetParentBackupPath(List<InternalBackupDataStoreVO> parentBackupDeltasOnSecondary, InternalBackupStoragePoolVO parentDeltaOnPrimary, KbossTO kbossTO) {
        VolumeObjectTO volumeObjectTO = kbossTO.getVolumeObjectTO();
        if (parentDeltaOnPrimary == null) {
            logger.debug("Volume [{}] has no parent on primary, thus its backup cannot be incremental.", volumeObjectTO);
            return;
        }

        InternalBackupDataStoreVO parentOnSecondary = parentBackupDeltasOnSecondary.stream()
                .filter(backupDataStoreVo -> volumeObjectTO.getVolumeId() == backupDataStoreVo.getVolumeId())
                .findFirst()
                .orElse(null);

        if (parentOnSecondary == null) {
            return;
        }

        logger.debug("Volume [{}] already has a backup [{}].", volumeObjectTO.getUuid(), parentOnSecondary.getBackupId());

        kbossTO.setPathBackupParentOnSecondary(parentOnSecondary.getBackupPath());
    }

    /**
     * Verify if the data center has heuristic rules for allocating backups; if there is then returns the {@link DataStore} returned by the JS script.
     * Otherwise, returns a {@link DataStore} with free capacity.
     */
    protected DataStore getImageStoreForBackup(Long dataCenterId, BackupVO backupVO) {
        DataStore imageStore = heuristicRuleHelper.getImageStoreIfThereIsHeuristicRule(dataCenterId, HeuristicType.BACKUP, backupVO);

        if (imageStore == null) {
            imageStore = dataStoreManager.getImageStoreWithFreeCapacity(dataCenterId);
        }

        if (imageStore == null) {
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);
            throw new CloudRuntimeException(String.format("Unable to find secondary storage for backup [%s].", backupVO));
        }

        logger.debug("Backup [{}] will use secondary storage [{}].", backupVO.getUuid(), imageStore.getUuid());
        return imageStore;
    }

    protected void setBackupAsIsolated(BackupVO backup) {
        logger.debug("Setting backup [{}] as isolated.", backup.getUuid());
        backupDetailDao.persist(new BackupDetailVO(backup.getId(), ISOLATED, Boolean.TRUE.toString(), true));
    }

    /**
     * Gets the parent for newBackup. Will set the newBackup as the end of chain if needed.<br/>
     * - If no backups are found, returns null. <br/>
     * - If the last backup was the end of the chain, returns null. <br/>
     *
     * @param newBackup the new backup being created.
     * @param backupChain newBackup's ancestors.
     * */
    protected InternalBackupJoinVO getParentAndSetEndOfChain(BackupVO newBackup, List<InternalBackupJoinVO> backupChain, BackupOfferingVO offering) {
        int chainSize = getChainSizeForBackup(offering, newBackup.getZoneId());
        if (CollectionUtils.isEmpty(backupChain)) {
            setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(chainSize, chainSize, newBackup.getId(), newBackup.getUuid());
            return null;
        }

        int remainingChainSize = chainSize - backupChain.size();
        setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(remainingChainSize, chainSize, newBackup.getId(), newBackup.getUuid());

        InternalBackupJoinVO parent = backupChain.get(0);
        return parent.getStatus().equals(Backup.Status.BackedUp) ? parent : null;
    }

    /**
     * For every restore point, maps a volume to it.
     * @throws CloudRuntimeException If cannot map restore point to any volume.
     * */
    protected Set<Pair<BackupDeltaTO, VolumeObjectTO>> generateBackupAndVolumePairsToRestore(List<InternalBackupDataStoreVO> backupDeltas, List<VolumeObjectTO> volumeTOs,
            InternalBackupJoinVO backupJoinVO, boolean sameVmAsBackup) {
        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs = new HashSet<>();
        DataStore dataStore = dataStoreManager.getDataStore(backupJoinVO.getImageStoreId(), DataStoreRole.Image);
        for (InternalBackupDataStoreVO backupDataStoreVO : backupDeltas) {
            VolumeObjectTO volumeObjectTO = volumeTOs.stream()
                    .filter(volumeTO -> sameVmAsBackup ? volumeTO.getVolumeId() == backupDataStoreVO.getVolumeId() : volumeTO.getDeviceId() == backupDataStoreVO.getDeviceId())
                    .findFirst()
                    .orElse(null);

            if (volumeObjectTO == null) {
                logger.error("All backups should have a corresponding volume at this point, however, backup delta [{}] does not.", backupDataStoreVO.getId());
                throw new CloudRuntimeException("Error while restoring backup. Please check the logs.");
            }

            backupAndVolumePairs.add(new Pair<>(new BackupDeltaTO(dataStore.getTO(), Hypervisor.HypervisorType.KVM, backupDataStoreVO.getBackupPath()), volumeObjectTO));
        }
        logger.debug("Generated the following list of pairs of backup deltas and volumes: [{}].", backupAndVolumePairs);
        return backupAndVolumePairs;
    }

    protected Pair<BackupDeltaTO, VolumeObjectTO> generateBackupAndVolumePairForSingleNewVolume(InternalBackupDataStoreVO backupDeltaVo, VolumeObjectTO volumeTO,
            InternalBackupJoinVO backupJoinVO) {
        DataStore dataStore = dataStoreManager.getDataStore(backupJoinVO.getImageStoreId(), DataStoreRole.Image);
        Pair<BackupDeltaTO, VolumeObjectTO> backupAndVolumePair = new Pair<>(new BackupDeltaTO(dataStore.getTO(), Hypervisor.HypervisorType.KVM, backupDeltaVo.getBackupPath()), volumeTO);

        logger.debug("Paired volume [{}] with backup delta [{}].", volumeTO, backupAndVolumePair.first());
        return backupAndVolumePair;
    }

    /**
     * For every volume, maps deltas that should be deleted, if there are any. If a volume has a delta but is not part of backup being restored, it will be mapped to be merged.
     *
     * @return List of deltas to be merged.
     * */
    protected List<DeltaMergeTreeTO> populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(List<InternalBackupStoragePoolVO> deltasOnPrimary, Set<BackupDeltaTO> deltasToRemove, List<VolumeObjectTO> volumeTOs,
            List<VolumeObjectTO> volumesNotPartOfTheBackupBeingRestored, String vmUuid) {
        List<DeltaMergeTreeTO> deltasToBeMerged = new ArrayList<>();
        for (InternalBackupStoragePoolVO deltaOnPrimary : deltasOnPrimary) {
            Optional<VolumeObjectTO> optional = volumeTOs.stream().filter(volumeTO -> volumeTO.getVolumeId() == deltaOnPrimary.getVolumeId()).findFirst();
            if (optional.isEmpty()) {
                logger.error("Failed to find volume that matches delta [{}] with path [{}]. Please check for inconsistencies on the database or if there are leftover" +
                        " deltas on storage.", deltaOnPrimary.getId(), deltaOnPrimary.getBackupDeltaPath());
                throw new CloudRuntimeException(String.format("Failed to restore VM [%s]. Please check the logs.", vmUuid));
            }
            VolumeObjectTO volumeObjectTO = optional.get();

            if (volumesNotPartOfTheBackupBeingRestored.contains(volumeObjectTO)) {
                deltasToBeMerged.add(createDeltaMergeTree(true, false, deltaOnPrimary, volumeObjectTO, null));
                continue;
            }

            DataStore dataStore = dataStoreManager.getDataStore(deltaOnPrimary.getStoragePoolId(), DataStoreRole.Primary);
            BackupDeltaTO backupDeltaTO = new BackupDeltaTO(dataStore.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaPath());
            logger.debug("Mapped the following backup delta on primary to be removed since the volume [{}] is not part of the backup being restored [{}].",
                    volumeObjectTO.getUuid(), backupDeltaTO);
            deltasToRemove.add(backupDeltaTO);
            volumeObjectTO.setPath(deltaOnPrimary.getBackupDeltaParentPath());
        }
        if (!deltasToBeMerged.isEmpty()) {
            logger.debug("The following deltaMergeTrees [{}] were created to merge volumes [{}] that have no backups.", deltasToBeMerged, volumesNotPartOfTheBackupBeingRestored);
        }
        return deltasToBeMerged;
    }

    protected void updateVolumePathsAndSizeIfNeeded(VirtualMachine vm, List<VolumeObjectTO> volumeTOs, List<Backup.VolumeInfo> volumeInfos,
            List<DeltaMergeTreeTO> deltaMergeTreeTOList, boolean sameVmAsBackup) {
        List<VolumeVO> volumeVOs = volumeDao.findByInstance(vm.getId());

        for (VolumeVO volumeVO : volumeVOs) {
            VolumeObjectTO volumeTO = volumeTOs.stream().filter(volumeObjectTO -> volumeObjectTO.getVolumeId() == volumeVO.getId()).findFirst().get();

            String log = "Volume [%s] path was updated as part of the backup restore process. New path: [%s].";
            DeltaMergeTreeTO deltaMergeTreeTO = deltaMergeTreeTOList.stream().filter(delta -> delta.getChild().getId() == volumeTO.getId()).findFirst().orElse(null);
            if (!volumeVO.getPath().equals(volumeTO.getPath())) {
                volumeVO.setPath(volumeTO.getPath());
                logger.debug(() -> String.format(log, volumeVO.getUuid(), volumeVO.getPath()));
            } else if (deltaMergeTreeTO != null) {
                volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
                logger.debug(() -> String.format(log, volumeVO.getUuid(), volumeVO.getPath()));
            }

            Backup.VolumeInfo volumeInfo = volumeInfos.stream()
                    .filter(info -> sameVmAsBackup ? volumeVO.getUuid().equals(info.getUuid()) : volumeVO.getDeviceId().equals(info.getDeviceId()))
                    .findFirst().orElse(null);
            if (volumeInfo != null && !Objects.equals(volumeInfo.getSize(), volumeVO.getSize())) {
                logger.debug("Volume [{}] size was restored as part of the backup restore process. Old size is [{}] new size is [{}].", volumeVO.getUuid(),
                        volumeVO.getSize(), volumeInfo.getSize());
                volumeVO.setSize(volumeInfo.getSize());
            }

            volumeDao.update(volumeVO.getId(), volumeVO);
        }
    }

    protected void createAndAttachVolumes(List<Backup.VolumeInfo> volumeInfos, List<InternalBackupDataStoreVO> backupDeltas, VirtualMachine vm, HostVO host) {
        logger.info("Found the following backup deltas that have no volume correspondence [{}]. Will create new volumes and attach them to VM [{}].", backupDeltas.stream()
                .map(InternalBackupDataStoreVO::getId).collect(Collectors.toList()), vm.getUuid());
        for (InternalBackupDataStoreVO delta : backupDeltas) {
            VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(delta.getVolumeId());
            Backup.VolumeInfo backupVolumeInfo = volumeInfos.stream().filter(info -> volumeVO.getUuid().equals(info.getUuid())).findFirst().orElseThrow();
            VolumeInfo volumeInfo = duplicateAndCreateVolume(vm, host, backupVolumeInfo);
            Volume volume = volumeApiService.attachVolumeToVM(vm.getId(), volumeInfo.getId(), null, false, true);
            transitVolumeStateWithoutThrow(volume, Volume.Event.RestoreRequested);
            delta.setVolumeId(volume.getId());
        }
    }

    protected VolumeInfo duplicateAndCreateVolume(VirtualMachine vm, HostVO hostVo, Backup.VolumeInfo backupVolumeInfo) {
        VolumeVO newVolume = duplicateVolume(backupVolumeInfo);
        VolumeInfo volumeInfo = volumeDataFactory.getVolume(newVolume.getId());

        try {
            volumeInfo = volumeOrchestrationService.createVolumeOnPrimaryStorage(vm, volumeInfo, Hypervisor.HypervisorType.KVM, null, hostVo.getClusterId(), hostVo.getPodId());
            validateCorrectStorageType(null, newVolume, volumeInfo);
        } catch (NoTransitionException ex) {
            logger.error("Exception while creating volume to restore.", ex);
            throw new CloudRuntimeException(ex);
        }

        return volumeInfo;
    }

    protected VolumeVO duplicateVolume(Backup.VolumeInfo backupVolumeInfo) {
        VolumeVO volumeVO = volumeDao.findByUuidIncludingRemoved(backupVolumeInfo.getUuid());
        VolumeVO duplicateVO = new VolumeVO(volumeVO);
        DiskOfferingVO diskOfferingVO = diskOfferingDao.findByUuidIncludingRemoved(backupVolumeInfo.getDiskOfferingId());
        duplicateVO.setDiskOfferingId(diskOfferingVO.getId());
        duplicateVO.setSize(backupVolumeInfo.getSize());
        duplicateVO.setMinIops(backupVolumeInfo.getMinIops());
        duplicateVO.setMaxIops(backupVolumeInfo.getMaxIops());
        duplicateVO.setAttached(null);
        duplicateVO.setVolumeType(Volume.Type.DATADISK);
        duplicateVO.setInstanceId(null);
        duplicateVO.setPoolId(null);
        duplicateVO.setPath(null);
        return volumeDao.persist(duplicateVO);
    }

    protected List<InternalBackupDataStoreVO> getBackupsWithoutVolumes(List<InternalBackupDataStoreVO> backups, List<VolumeObjectTO> volumes) {
        List<InternalBackupDataStoreVO> deltasOnSecondaryWithNoVolumes = new ArrayList<>();
        for (InternalBackupDataStoreVO backup : backups) {
            VolumeObjectTO volumeObjectTO = volumes.stream().filter(volumeTO -> volumeTO.getVolumeId() == backup.getVolumeId())
                    .findFirst()
                    .orElse(null);

            if (volumeObjectTO == null) {
                deltasOnSecondaryWithNoVolumes.add(backup);
            }
        }
        return deltasOnSecondaryWithNoVolumes;
    }

    protected List<VolumeObjectTO> getVolumesThatAreNotPartOfTheBackup(List<VolumeObjectTO> volumeObjectTOS, List<InternalBackupDataStoreVO> deltasOnSecondary) {
        List<VolumeObjectTO> volumesWithNoBackups = new ArrayList<>();
        for (VolumeObjectTO volume : volumeObjectTOS) {
            if (deltasOnSecondary.stream().noneMatch(delta -> delta.getVolumeId() == volume.getVolumeId())) {
                volumesWithNoBackups.add(volume);
            }
        }
        logger.debug("Found the following volumes that are not part of the backup being restored [{}].", volumesWithNoBackups);
        return volumesWithNoBackups;
    }

    protected void processBackupSuccess(boolean runningVm, List<VolumeObjectTO> volumeTOs, HashMap<String, InternalBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef,
            HashMap<String, InternalBackupDataStoreVO> volumeUuidToDeltaSecondaryRef, TakeKbossBackupAnswer answer, List<InternalBackupStoragePoolVO> parentBackupDeltasOnPrimary,
            List<VMSnapshotVO> succeedingVmSnapshots, BackupVO backupVO, boolean fullBackup, VirtualMachine userVm, Long hostId, boolean endChain, boolean isolated) {
        long physicalBackupSize = 0;
        logger.debug("Processing backup [{}] success.", backupVO.getUuid());
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            physicalBackupSize = updateDeltaReferencesAndCalculateBackupPhysicalSize(volumeObjectTO, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, answer,
                    physicalBackupSize, endChain, isolated, backupVO);
        }

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(parentBackupDeltasOnPrimary, succeedingVmSnapshots.isEmpty() ? null : succeedingVmSnapshots.get(0));

        backupVO.setSize(physicalBackupSize);
        backupVO.setStatus(Backup.Status.BackedUp);
        backupVO.setBackedUpVolumes(backupManager.createVolumeInfoFromVolumes(new ArrayList<>(volumeDao.findByInstance(userVm.getId()))));
        backupDao.loadDetails(backupVO);
        backupVO.getDetails().putAll(backupManager.getBackupDetailsFromVM(userVm));
        backupVO.setType(fullBackup ? "FULL" : "INCREMENTAL");
        backupDao.update(backupVO.getId(), backupVO);

        transitVmStateWithoutThrow(userVm, runningVm ? VirtualMachine.Event.BackupSucceededRunning : VirtualMachine.Event.BackupSucceededStopped, hostId);
    }

    protected void processBackupFailure(Answer answer, VirtualMachine vm, long hostId, boolean runningVm, BackupVO backupVO) {
        if (answer instanceof TakeKbossBackupAnswer && ((TakeKbossBackupAnswer) answer).isVmConsistent()) {
            logger.info("Backup [{}] of VM [{}] failed. However, the VM is still consistent, so we will roll back its state.", backupVO.getUuid(), vm.getUuid());
            backupVO.setStatus(Backup.Status.Failed);

            transitVmStateWithoutThrow(vm, runningVm ? VirtualMachine.Event.OperationFailedToRunning : VirtualMachine.Event.OperationFailedToStopped, hostId);
        } else {
            logger.info("Backup [{}] of VM [{}] ended in error. We are not sure if the VM is consistent; thus, we will set it as BackupError.", backupVO.getUuid(), vm.getUuid());
            transitVmStateWithoutThrow(vm, VirtualMachine.Event.OperationFailedToError, hostId);
            vmInstanceDetailsDao.addDetail(vm.getId(), ApiConstants.LAST_KNOWN_STATE, runningVm ? VirtualMachine.State.Running.name() : VirtualMachine.State.Stopped.name(), false);
            backupVO.setStatus(Backup.Status.Error);
        }

        backupDao.update(backupVO.getId(), backupVO);
    }

    protected void processRemovedBackups(List<Long> removedBackupIds) {
        for (Long removedBackupId : removedBackupIds) {
            BackupVO removedBackupVO = backupDao.findByIdIncludingRemoved(removedBackupId);
            removedBackupVO.setStatus(Backup.Status.Expunged);
            backupDao.update(removedBackupId, removedBackupVO);
            internalBackupDataStoreDao.expungeByBackupId(removedBackupId);
            backupDetailDao.removeDetailsExcept(removedBackupId, END_OF_CHAIN);
        }
    }

    /**
     * For every backup, except for the one which the command was issued, will set them as Expunged regardless and hope operators will look
     * at the logs. For the current one, if forced=false, will set it as error, otherwise, will set it as Expunged as well.
     * */
    protected boolean processRemoveBackupFailures(boolean forced, Answer[] deleteAnswers, List<Long> removedBackupIds, InternalBackupJoinVO backupJoinVO) {
        List<Answer> failures = Arrays.stream(deleteAnswers).filter(answer -> !answer.getResult()).collect(Collectors.toList());
        Set<Long> failedToRemoveBackupIdSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(failures)) {
            StringBuilder failureStringBuilder = new StringBuilder("Encountered the following failures during backup removal, all will be marked as Expunged and need to be" +
                    " manually deleted from storage. ");
            for (Answer answer : failures) {
                failedToRemoveBackupIdSet.add(((BackupDeleteAnswer)answer).getBackupId());
                failureStringBuilder.append(answer.getDetails());
            }
            logger.error(failureStringBuilder.toString());
        }

        removedBackupIds.removeAll(failedToRemoveBackupIdSet);

        boolean result = failedToRemoveBackupIdSet.isEmpty();
        if (!forced && failedToRemoveBackupIdSet.remove(backupJoinVO.getId())) {
            BackupVO failedVO = backupDao.findByIdIncludingRemoved(backupJoinVO.getId());
            logger.info("Since backup delete command was not forced, will not set the main backup [{}] as Expunged, will set it as error instead.", failedVO.getUuid());
            failedVO.setStatus(Backup.Status.Error);
            backupDao.update(failedVO.getId(), failedVO);
        }

        for (Long failedToRemove : failedToRemoveBackupIdSet) {
            BackupVO failedVO = backupDao.findByIdIncludingRemoved(failedToRemove);
            failedVO.setStatus(Backup.Status.Expunged);
            logger.error("Setting backup [{}] as expunged, even though there was an error when deleting it from storage. Please look at the logs and check if it was deleted from" +
                    " storage.", failedVO.getUuid());
            backupDao.update(failedToRemove, failedVO);
        }

        return result;
    }

    protected void processConsolidateAnswer(ConsolidateVolumesAnswer cAnswer, List<VolumeInfo> volumesToConsolidate, VirtualMachine vm) {
        for (VolumeObjectTO volumeObjectTO : cAnswer.getSuccessfullyConsolidatedVolumes()) {
            VolumeInfo volumeInfo = volumesToConsolidate.stream().filter(vol -> vol.getId() == volumeObjectTO.getVolumeId()).findFirst().orElseThrow();
            transitVolumeStateWithoutThrow(volumeInfo.getVolume(), Volume.Event.OperationSucceeded);
            volumesToConsolidate.remove(volumeInfo);
        }
        volumesToConsolidate.forEach(volumeInfo -> transitVolumeStateWithoutThrow(volumeInfo, Volume.Event.OperationFailed));
        if (cAnswer.getResult()) {
            vmInstanceDetailsDao.removeDetail(vm.getId(), VmDetailConstants.LINKED_VOLUMES_SECONDARY_STORAGE_UUIDS);
        } else {
            throw new BackupException(String.format("Failed to consolidate all volumes necessary of VM [%s]. Missing volumes are [%s].", vm.getUuid(), volumesToConsolidate), false);
        }
    }

    protected boolean processRestoreAnswers(VirtualMachine vm, Answer[] answers, boolean quickRestore) {
        boolean cmdSucceeded = true;
        for (Answer answer : answers) {
            if (answer == null || !answer.getResult()) {
                cmdSucceeded = false;
                logger.error("Failed to restore backup due to: [{}].", answer == null ? "null answer" : answer.getDetails());
            }
            if (answer instanceof RestoreKbossBackupAnswer && quickRestore) {
                RestoreKbossBackupAnswer restoreAnswer = (RestoreKbossBackupAnswer) answer;
                vmInstanceDetailsDao.addDetail(vm.getId(), VmDetailConstants.LINKED_VOLUMES_SECONDARY_STORAGE_UUIDS, StringUtils.join(restoreAnswer.getSecondaryStorageUuids(), ","), false);
            }
        }
        return cmdSucceeded;
    }

    protected boolean processValidationAnswer(Answer answer, BackupVO backupVO, UserVmVO validationVm, HostVO hostVo, ValidateKbossVmCommand validateKbossVmCommand) {
        if (answer == null) {
            String msg = String.format("Backup [%s] was validated using dummy VM [%s]. The backup was deemed invalid due to: Null answer from host [%s]", backupVO.getUuid(),
                    validationVm.getName(), hostVo.getName());
            logger.error(msg);
            setBackupAsInvalidAndSendAlert(backupVO, msg);
            return false;
        }
        if (!answer.getResult()) {
            String msg = String.format("Backup [%s] was validated using dummy VM [%s]. The backup was deemed invalid due to: %s", backupVO.getUuid(),
                    validationVm.getName(), answer.getDetails());
            logger.error(msg);
            setBackupAsInvalidAndSendAlert(backupVO, msg);
            return false;
        }
        if (answer instanceof ValidateKbossVmAnswer) {
            ValidateKbossVmAnswer validateKbossVmAnswer = (ValidateKbossVmAnswer)answer;
            boolean result = true;
            String msg = String.format("Backup [%s] was validated using dummy VM [%s]. The backup was deemed invalid due to: ", backupVO.getUuid(), validationVm.getName());
            if (validateKbossVmCommand.isWaitForBoot() && !validateKbossVmAnswer.isBootValidated()) {
                result = false;
                msg += "\n - The VM did not boot within the expected time.";
            }
            if (validateKbossVmCommand.isExecuteScript() && validateKbossVmAnswer.getScriptResult() != null) {
                result = false;
                msg += "\n - The script did not output the expected output. Captured output: " + validateKbossVmAnswer.getScriptResult();
            }
            if (validateKbossVmCommand.isTakeScreenshot() && validateKbossVmAnswer.getScreenshotPath() == null) {
                result = false;
                msg += "\n - We were unable to take a screenshot of the VM.";
            } else if (validateKbossVmCommand.isTakeScreenshot()) {
                logger.debug("Saving validation screenshot path [{}] to the backup details of backup [{}].", validateKbossVmAnswer.getScreenshotPath(), backupVO.getUuid());
                backupDetailDao.addDetail(backupVO.getId(), SCREENSHOT_PATH, validateKbossVmAnswer.getScreenshotPath(), false);
            }
            if (!result) {
                setBackupAsInvalidAndSendAlert(backupVO, msg);
            }

            return result;
        }
        return false;
    }

    protected void handleBackupExceptionInRestore(VirtualMachine vm, BackupException jobResult) {
        if (!jobResult.isVmConsistent()) {
            UserVmVO vmVO = userVmDao.findById(vm.getId());
            vmVO.setState(VirtualMachine.State.RestoreError);
            userVmDao.update(vmVO.getId(), vmVO);
            for (VolumeVO vol : volumeDao.findByInstance(vmVO.getId())) {
                vol.setState(Volume.State.RestoreError);
                volumeDao.update(vol.getId(), vol);
            }
        }
    }

    protected void handleRestoreException(Backup backup, VirtualMachine vm, Object jobResult) {
        if (!(jobResult instanceof Throwable)) {
            return;
        }
        if (jobResult instanceof BackupException) {
            handleBackupExceptionInRestore(vm, (BackupException)jobResult);
        } else if (jobResult instanceof BackupProviderException) {
            throw (BackupProviderException) jobResult;
        }
        throw new CloudRuntimeException(String.format("Exception while restoring KVM native incremental backup [%s]. Check the logs for more information.", backup.getUuid()), ((Throwable)jobResult).getCause());
    }

    protected boolean endBackupChain(VirtualMachine vm) {
        InternalBackupJoinVO current = internalBackupJoinDao.findCurrent(vm.getId());
        if (current == null) {
            logger.debug("There is no current active chain, no need to do anything.");
            return true;
        }

        if (mergeCurrentBackupDeltas(current)) {
            setEndOfChainAndRemoveCurrentForBackup(current);
            return true;
        }
        return false;
    }

    /**
     * Merges the backup deltas related to the passed {@code InternalBackupJoinVO}.
     *
     * @return true if the merge was successful and false otherwise.
     * */
    protected boolean mergeCurrentBackupDeltas(InternalBackupJoinVO backupJoinVO) {
        VirtualMachine userVm = userVmDao.findById(backupJoinVO.getVmId());

        VMSnapshotVO succeedingVmSnapshot = getSucceedingVmSnapshot(backupJoinVO);
        MergeDiskOnlyVmSnapshotCommand cmd = buildMergeDiskOnlyVmSnapshotCommandForCurrentBackup(backupJoinVO, userVm, succeedingVmSnapshot);
        Long hostId = vmSnapshotHelper.pickRunningHost(backupJoinVO.getVmId());

        Answer answer = sendBackupCommand(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            logger.error("Failed to remove backup [{}]. Tried to merge the current deltas to cleanup the VM but failed due to [{}].",
                    backupJoinVO.getUuid(), answer != null ? answer.getDetails() : "no answer");
            return false;
        }

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(internalBackupStoragePoolDao.listByBackupId(backupJoinVO.getId()), succeedingVmSnapshot);

        if (succeedingVmSnapshot != null) {
            return true;
        }

        for (DeltaMergeTreeTO deltaMergeTreeTO : cmd.getDeltaMergeTreeToList()) {
            VolumeVO volumeVO = volumeDao.findById(deltaMergeTreeTO.getVolumeObjectTO().getVolumeId());
            volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
            logger.debug("Updating volume [{}] path to [{}] as part of the backup delete cleanup process.", volumeVO.getUuid(), volumeVO.getPath());
            volumeDao.update(volumeVO.getId(), volumeVO);
        }

        return true;
    }

    protected void createDeleteCommandsAndMergeTrees(List<VolumeObjectTO> volumeObjectTOs, Commands commands, List<InternalBackupStoragePoolVO> deletedDeltas,
            VMSnapshotVO vmSnapshotSucceedingCurrentBackup, List<DeltaMergeTreeTO> deltaMergeTreeTOList) {
        for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
            InternalBackupStoragePoolVO delta = internalBackupStoragePoolDao.findOneByVolumeId(volumeObjectTO.getVolumeId());
            if (delta == null) {
                continue;
            }
            if (delta.getBackupDeltaPath().equals(volumeObjectTO.getPath())) {
                commands.addCommand(new DeleteCommand(new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, delta.getBackupDeltaParentPath())));
                deletedDeltas.add(delta);
                logger.debug("Volume [{}] has a backup delta that will be deleted as part of the preparation to revert a VM snapshot.", volumeObjectTO.getUuid());
            } else {
                deltaMergeTreeTOList.add(createDeltaMergeTree(false, false, delta, volumeObjectTO, vmSnapshotSucceedingCurrentBackup));
            }
        }
    }

    /***
     * Gets the list of parents that should be expunged. Will also create delete commands for them and add them to the list deleteCommands object.
     *
     * @param backupVO backup being expunged
     * @param deleteCommands Commands object that will be appended with the delete commands for the parent backups.
     * @return A pair which contains the list of backups that will be expunged, and the reference to the last backup of the chain that is still alive, if it exists.
     */
    protected Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(BackupVO backupVO, Commands deleteCommands) {
        logger.debug("Searching for removed parents of [{}] that should be expunged.", backupVO);
        List<InternalBackupJoinVO> backupParents = getBackupJoinParents(backupVO, true);
        List<InternalBackupJoinVO> backupParentsToBeExpunged = null;
        InternalBackupJoinVO lastAliveBackup = null;
        for (int i = 0; i < backupParents.size(); i++) {
            InternalBackupJoinVO backupParent = backupParents.get(i);
            if (Backup.Status.Removed.equals(backupParent.getStatus())) {
                addBackupDeltasToDeleteCommand(backupParent.getId(), deleteCommands);
            } else {
                backupParentsToBeExpunged = backupParents.subList(0, i);
                lastAliveBackup = backupParents.get(i);
                break;
            }
        }
        if (backupParentsToBeExpunged == null) {
            backupParentsToBeExpunged = backupParents;
        }
        logger.debug("Found [{}] removed parents of [{}] that should be expunged: [{}].", backupParentsToBeExpunged.size(), backupVO, backupParentsToBeExpunged);
        return new Pair<>(backupParentsToBeExpunged, lastAliveBackup);
    }

    protected MergeDiskOnlyVmSnapshotCommand buildMergeDiskOnlyVmSnapshotCommandForCurrentBackup(InternalBackupJoinVO backupJoinVO, VirtualMachine userVm, VMSnapshotVO vmSnapshot) {
        List<DeltaMergeTreeTO> deltaMergeTreeTOs = new ArrayList<>();

        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(backupJoinVO.getVmId());
        Map<Long, List<SnapshotDataStoreVO>> volumeIdToSnapshotDataStoreList = gatherSnapshotReferencesOfChildrenSnapshot(volumeTOs, vmSnapshot);
        List<InternalBackupStoragePoolVO> deltasOnPrimary = internalBackupStoragePoolDao.listByBackupId(backupJoinVO.getId());

        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            KbossTO kbossTO = new KbossTO(volumeObjectTO, volumeIdToSnapshotDataStoreList.getOrDefault(volumeObjectTO.getId(), new ArrayList<>()));
            createDeltaMergeTreeForVolume(vmSnapshot == null, userVm.getState() == VirtualMachine.State.Running, deltasOnPrimary, vmSnapshot, kbossTO);
            if (kbossTO.getDeltaMergeTreeTO() != null) {
                deltaMergeTreeTOs.add(kbossTO.getDeltaMergeTreeTO());
            } else {
                logger.debug("Volume [{}] does not have any deltas to merge as part of the backup delete process.", volumeObjectTO.getUuid());
            }
        }

        return new MergeDiskOnlyVmSnapshotCommand(deltaMergeTreeTOs, userVm.getState().equals(VirtualMachine.State.Running), userVm.getInstanceName());
    }

    protected DataStore addBackupDeltasToDeleteCommand(long backupId, Commands deleteCommands) {
        InternalBackupJoinVO internalBackupJoinVO = internalBackupJoinDao.findById(backupId);
        List<InternalBackupDataStoreVO> internalBackupDataStoreVOS = internalBackupDataStoreDao.listByBackupId(backupId);
        DataStore dataStore = dataStoreManager.getDataStore(internalBackupJoinVO.getImageStoreId(), DataStoreRole.Image);
        DataStoreTO dataStoreTO = dataStore.getTO();
        BackupDetailVO screenshotPath = backupDetailDao.findDetail(backupId, SCREENSHOT_PATH);
        for (InternalBackupDataStoreVO internalBackupDataStoreVO : internalBackupDataStoreVOS) {
            BackupDeltaTO backupDeltaTO = new BackupDeltaTO(dataStoreTO, Hypervisor.HypervisorType.KVM, internalBackupDataStoreVO.getBackupPath());
            backupDeltaTO.setId(backupId);
            if (screenshotPath != null) {
                backupDeltaTO.setScreenshotPath(screenshotPath.getValue());
                screenshotPath = null;
            }
            DeleteCommand deleteCommand = new DeleteCommand(backupDeltaTO);
            deleteCommands.addCommand(deleteCommand);
        }
        return dataStore;
    }

    protected Set<String> getParentSecondaryStorageUrls(BackupVO backupVO) {
        List<InternalBackupJoinVO> parentBackups = getBackupJoinParents(backupVO, true);
        Set<Long> secondaryStorageIds = parentBackups.stream().map(InternalBackupJoinVO::getImageStoreId).collect(Collectors.toSet());
        return secondaryStorageIds.stream().map(id -> imageStoreDao.findById(id).getUrl()).collect(Collectors.toSet());
    }

    protected List<String> getChainImageStoreUrls(List<InternalBackupJoinVO> backupChain) {
        List<String> chainImageStoreUrls;
        LinkedHashSet<Long> imageStoreIdSet = backupChain.stream().map(InternalBackupJoinVO::getImageStoreId).collect(Collectors.toCollection(LinkedHashSet::new));
        chainImageStoreUrls = imageStoreIdSet.stream().map(id -> imageStoreDao.findById(id).getUrl()).collect(Collectors.toList());
        return chainImageStoreUrls;
    }

    /**
     * Gets the list of backup parents of a given BackupVO.
     * @param backupVO the backup in question.
     * @param includeRemoved whether to include removed (but not expunged) parents or not.
     * @return list of parents, or an empty list if no parents found.
     * */
    protected List<InternalBackupJoinVO> getBackupJoinParents(BackupVO backupVO, boolean includeRemoved) {
        List<InternalBackupJoinVO> ancestorBackups;

        if (includeRemoved) {
            ancestorBackups = internalBackupJoinDao.listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(backupVO.getVmId(), backupVO.getDate());
        } else {
            ancestorBackups = internalBackupJoinDao.listByBackedUpAndVmIdAndDateBeforeOrAfterOrderBy(backupVO.getVmId(), backupVO.getDate(), true, false);
        }

        for (int i = 0; i < ancestorBackups.size(); i++) {
            if (ancestorBackups.get(i).getEndOfChain()) {
                return ancestorBackups.subList(0, i);
            }
        }

        logger.debug("Found the following backup chain ancestors of backup [{}]: [{}].", backupVO, ancestorBackups);
        return ancestorBackups;
    }

    protected int getChainSizeForBackup(BackupOfferingVO offering, long zoneId) {
        BackupOfferingDetailsVO detailsVO = backupOfferingDetailsDao.findDetail(offering.getId(), ApiConstants.BACKUP_CHAIN_SIZE);
        if (detailsVO != null) {
            return Integer.parseInt(detailsVO.getValue());
        }
        return backupChainSize.valueIn(zoneId);
    }

    /**
     * Gets the list of backup children of a given backupVO. In ascending created order.
     *
     * @return list of children, or and empty list if no children found.
     * */
    protected List<InternalBackupJoinVO> getBackupJoinChildren(BackupVO backupVO) {
        List<InternalBackupJoinVO> children = internalBackupJoinDao.listByBackedUpAndVmIdAndDateBeforeOrAfterOrderBy(backupVO.getVmId(), backupVO.getDate(), false, true);

        long parentId = backupVO.getId();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getParentId() != parentId) {
                return children.subList(0, i);
            }
            parentId = children.get(i).getId();
        }

        return children;
    }

    /**
     * Creates a detail for the given BackupVO if the remaining chain size is one or less and the value of backupChainSize is greater than 0.
     * */
    protected void setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(int remainingChainSize, int chainSize, long backupId, String backupUuid) {
        if (remainingChainSize <= 1 && chainSize > 0) {
            logger.debug("Setting backup [{}] as end of chain.", backupUuid);
            backupDetailDao.persist(new BackupDetailVO(backupId, END_OF_CHAIN, Boolean.TRUE.toString(), true));
        }
    }

    protected void setBackupVirtualSize(List<VolumeObjectTO> volumeTOs, BackupVO backupVO) {
        long virtualSize = 0;
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            virtualSize += volumeObjectTO.getSize();
        }

        backupVO.setProtectedSize(virtualSize);
    }

    protected void updateBackupStatusToBackingUp(List<VolumeObjectTO> volumeTOs, BackupVO backupVO) {
        setBackupVirtualSize(volumeTOs, backupVO);
        backupVO.setStatus(Backup.Status.BackingUp);
        backupDao.update(backupVO.getId(), backupVO);
    }

    /**
     * Retrieves the current backup and removes the CURRENT detail. If the informed backup is not the end of chain, sets is as the new CURRENT
     * */
    protected void updateCurrentBackup(InternalBackupJoinVO backup) {
        InternalBackupJoinVO current = internalBackupJoinDao.findCurrent(backup.getVmId());

        if (current != null) {
            backupDetailDao.removeDetail(current.getId(), CURRENT);
        }

        if (!backup.getEndOfChain()) {
            backupDetailDao.persist(new BackupDetailVO(backup.getId(), CURRENT, Boolean.TRUE.toString(), true));
        }
    }

    /**
     * Given a backup, removes the CURRENT detail, and if the snapshot is not set as END_OF_CHAIN, sets it as END_OF_CHAIN.
     * */
    protected void setEndOfChainAndRemoveCurrentForBackup(InternalBackupJoinVO currentBackup) {
        backupDetailDao.removeDetail(currentBackup.getId(), CURRENT);
        if (!currentBackup.getEndOfChain()) {
            backupDetailDao.persist(new BackupDetailVO(currentBackup.getId(), END_OF_CHAIN, Boolean.TRUE.toString(), true));
        }
    }

    protected void setBackupUnableToValidateAndSendAlert(BackupVO backupVO, String msg) {
        backupVO.setValidationStatus(Backup.ValidationStatus.UnableToValidate);
        backupDao.update(backupVO.getId(), backupVO);
        alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_BACKUP_VALIDATION_UNABLE_TO_VALIDATE, backupVO.getZoneId(), null, String.format("Unable to validate backup [%s]",
                backupVO.getName()), msg);
    }

    protected void sendCleanupFailedEmail(BackupVO backupVO, String msg) {
        alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_BACKUP_VALIDATION_CLEANUP_FAILED, backupVO.getZoneId(), null, String.format("Cleanup of validation of backup " +
                        "[%s] failed",
                backupVO.getName()), msg);
    }

    protected void setBackupAsInvalidAndSendAlert(BackupVO backupVO, String msg) {
        backupVO.setValidationStatus(Backup.ValidationStatus.NotValid);
        backupDao.update(backupVO.getId(), backupVO);
        alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_BACKUP_VALIDATION_FAILED, backupVO.getZoneId(), null, String.format("Backup [%s] is not valid",
                backupVO.getName()), msg);
    }

    protected void configureKbossTosForCleanup(UserVmVO userVmVO, List<InternalBackupStoragePoolVO> deltasOnPrimary, List<InternalBackupDataStoreVO> deltasOnSecondary, boolean runningVM,
            List<InternalBackupStoragePoolVO> parentDeltasOnPrimary, List<KbossTO> kbossTOS) {
        for (VolumeObjectTO volumeObjectTO : vmSnapshotHelper.getVolumeTOList(userVmVO.getId())) {
            InternalBackupStoragePoolVO deltaOnPrimary = deltasOnPrimary.stream()
                            .filter(delta -> delta.getVolumeId() == volumeObjectTO.getVolumeId()).findFirst().orElseThrow();
            volumeObjectTO.setPath(deltaOnPrimary.getBackupDeltaPath());

            InternalBackupDataStoreVO deltaOnSecondary =
                    deltasOnSecondary.stream().filter(delta -> delta.getVolumeId() == volumeObjectTO.getVolumeId()).findFirst().orElseThrow();
            KbossTO kbossTO = new KbossTO(volumeObjectTO, deltaOnPrimary.getBackupDeltaParentPath(), deltaOnSecondary.getBackupPath());

            parentDeltasOnPrimary.stream()
                    .filter(delta -> delta.getVolumeId() == volumeObjectTO.getVolumeId()).findFirst()
                    .ifPresent(parentDelta -> kbossTO.setParentDeltaPathOnPrimary(parentDelta.getBackupDeltaParentPath()));

            kbossTOS.add(kbossTO);
        }
    }

    protected void configureValidationSteps(ValidateKbossVmCommand cmd, BackupVO backup) {
        BackupOfferingVO offeringVO = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        BackupOfferingDetailsVO detailsVO = backupOfferingDetailsDao.findDetail(offeringVO.getId(), ApiConstants.VALIDATION_STEPS);
        String validationSteps = detailsVO.getValue();
        for (String step : validationSteps.split(",")) {
            Backup.ValidationSteps enumStep = Backup.ValidationSteps.valueOf(step);
            switch (enumStep) {
                case screenshot:
                    cmd.setTakeScreenshot(true);
                    VMInstanceDetailVO screenshotWait = vmInstanceDetailsDao.findDetail(backup.getVmId(), VmDetailConstants.VALIDATION_SCREENSHOT_WAIT);
                    cmd.setScreenshotWait(screenshotWait != null ? Integer.valueOf(screenshotWait.getValue()) :
                            BackupValidationServiceController.backupValidationScreenshotDefaultWait.valueIn(backup.getAccountId()));
                    break;
                case wait_for_boot:
                    cmd.setWaitForBoot(true);
                    VMInstanceDetailVO bootTimeout = vmInstanceDetailsDao.findDetail(backup.getVmId(), VmDetailConstants.VALIDATION_BOOT_TIMEOUT);
                    cmd.setBootTimeout(bootTimeout != null ? Integer.valueOf(bootTimeout.getValue()) :
                            BackupValidationServiceController.backupValidationBootDefaultTimeout.valueIn(backup.getAccountId()));
                    break;
                case execute_command:
                    configureValidationScript(cmd, backup.getVmId(), backup.getAccountId());
                    break;
            }
        }
    }

    protected void configureValidationScript(ValidateKbossVmCommand cmd, long vmId, long accountId) {
        VMInstanceDetailVO script = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND);
        if (script == null) {
            return;
        }
        cmd.setExecuteScript(true);
        cmd.setScriptToExecute(script.getValue());
        VMInstanceDetailVO scriptArguments = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND_ARGUMENTS);
        cmd.setScriptArguments(scriptArguments != null ? scriptArguments.getValue() : null);
        VMInstanceDetailVO scriptExpectedResult = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND_EXPECTED_RESULT);
        cmd.setExpectedResult(scriptExpectedResult != null ? scriptExpectedResult.getValue() : "0");
        VMInstanceDetailVO scriptTimeout = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND_TIMEOUT);
        cmd.setScriptTimeout(scriptTimeout != null ? Integer.valueOf(scriptTimeout.getValue()) :
                BackupValidationServiceController.backupValidationScriptDefaultTimeout.valueIn(accountId));
    }

    protected void createBasicBackupDetails(Long imageStoreId, Long parentId, BackupVO backupVO) {
        backupDetailDao.persist(new BackupDetailVO(backupVO.getId(), IMAGE_STORE_ID, imageStoreId.toString(), false));
        backupDetailDao.persist(new BackupDetailVO(backupVO.getId(), PARENT_ID, parentId.toString(), false));
    }

    protected void updateReferencesAfterPrepareForSnapshotRevert(List<DeltaMergeTreeTO> deltaMergeTreeTOList, List<SnapshotDataStoreVO> snapRefsSucceedingCurrentBackup,
            List<InternalBackupStoragePoolVO> deletedDeltas, InternalBackupJoinVO backupVO) {
        for (DeltaMergeTreeTO deltaMergeTreeTO : deltaMergeTreeTOList) {
            SnapshotDataStoreVO snapshotRef = snapRefsSucceedingCurrentBackup.stream()
                    .filter(ref -> Objects.equals(ref.getVolumeId(), deltaMergeTreeTO.getVolumeObjectTO().getVolumeId()))
                    .findFirst()
                    .orElse(null);
            if (snapshotRef != null) {
                snapshotRef.setInstallPath(deltaMergeTreeTO.getParent().getPath());
                logger.debug("Updating snapshot reference [{}] path to [{}] as part of the preparation to restore a VM snapshot.", snapshotRef.getId(), snapshotRef.getInstallPath());
                snapshotDataStoreDao.update(snapshotRef.getId(), snapshotRef);
            }
            internalBackupStoragePoolDao.expungeByVolumeId(deltaMergeTreeTO.getVolumeObjectTO().getVolumeId());
        }

        for (InternalBackupStoragePoolVO delta : deletedDeltas) {
            internalBackupStoragePoolDao.expungeByVolumeId(delta.getVolumeId());
        }

        setEndOfChainAndRemoveCurrentForBackup(backupVO);
    }

    protected Answer sendBackupCommand(long hostId, Command cmd) {
        cmd.setWait(backupTimeout.value());
        return agentManager.easySend(hostId, cmd);
    }

    protected Answer[] sendBackupCommands(Long hostId, Commands cmds) throws OperationTimedoutException, AgentUnavailableException {
        for (Command cmd : cmds) {
            cmd.setWait(backupTimeout.value());
        }
        return agentManager.send(hostId, cmds);
    }

    protected void validateCorrectStorageType(BackupVO backupVO, VolumeVO volume, VolumeInfo volumeInfo) {
        StoragePoolVO storagePoolVO = storagePoolDao.findById(volumeInfo.getDataStore().getId());
        if (!supportedStoragePoolTypes.contains(storagePoolVO.getPoolType())) {
            logger.error("Error while trying create volume [{}]. It was created in a storage that is not supported. Make sure that the disk offerings of VMs with backup " +
                    "offerings can only be allocated to file-based storages ({}).", backupVO, volume, supportedStoragePoolTypes);
            throw new CloudRuntimeException(String.format("Unable to create volume [%s] due to a failure to allocate the volume. Please check the logs.", backupVO.getUuid()));
        }
    }

    protected void validateQuickRestore(Backup backup, boolean quickRestore) {
        BackupOfferingVO backupOfferingVO = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        BackupOfferingDetailsVO detail = backupOfferingDetailsDao.findDetail(backupOfferingVO.getId(), ApiConstants.QUICK_RESTORE);
        if (quickRestore && (detail == null || !Boolean.parseBoolean(detail.getValue()))) {
            throw new BackupProviderException(String.format("Unable to quick restore backup [%s] using offering [%s] as the offering does not support quick restoration.",
                    backup.getUuid(), backupOfferingVO.getUuid()));
        }
    }

    protected boolean offeringSupportsValidation(InternalBackupJoinVO backup) {
        BackupOfferingVO backupOfferingVO = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        BackupOfferingDetailsVO detail = backupOfferingDetailsDao.findDetail(backupOfferingVO.getId(), ApiConstants.VALIDATE);
        if (detail == null || !Boolean.parseBoolean(detail.getValue())) {
            logger.debug("Backup [{}] will not be validated as offering [{}] does not support it.", backup, backupOfferingVO.getUuid());
            return false;
        }
        return true;
    }

    protected boolean offeringSupportsCompression(InternalBackupJoinVO backup) {
        BackupOfferingVO backupOfferingVO = backupOfferingDao.findByIdIncludingRemoved(backup.getBackupOfferingId());
        BackupOfferingDetailsVO detail = backupOfferingDetailsDao.findDetail(backupOfferingVO.getId(), ApiConstants.COMPRESS);
        if (detail == null || !Boolean.parseBoolean(detail.getValue())) {
            logger.debug("Backup [{}] will not be compressed as offering [{}] does not support it.", backup, backupOfferingVO.getUuid());
            return false;
        }
        return true;
    }

    protected void validateVmState(VirtualMachine vm, String operation, VirtualMachine.State... additionalStates) {
        List<VirtualMachine.State> allowedStates = new ArrayList<>(this.allowedVmStates);
        allowedStates.addAll(Arrays.asList(additionalStates));
        if (!allowedStates.contains(vm.getState())) {
            throw new BackupProviderException(String.format("VM [%s] is not in the right state to %s. It must be in one of these states: %s", vm.getUuid(), operation,
                    allowedStates));
        }
    }

    protected Pair<Boolean, BackupVO> validateCompressionStateForRestoreAndGetBackup(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Pair<Boolean, BackupVO>>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to get lock on backup [{}]. Cannot restore it.", backupId);
                    return new Pair<>(false, null);
                }

                if (backupVO.getCompressionStatus() == Backup.CompressionStatus.FinalizingCompression) {
                    logger.error("We cannot restore backups that are finalizing the compression process. Please wait for the process to end and try again later.",
                            allowedBackupStatesToCompress, backupVO.getStatus());
                    return new Pair<>(false, null);
                }
                backupVO.setStatus(Backup.Status.Restoring);
                backupDao.update(backupId, backupVO);
                return new Pair<>(true, backupVO);
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    /**
     * Validates that the backup is in a valid state. This is synchronized with the backup compression check. We get a new backup reference to make sure the compression has not
     * changed the backup compression state.
     * */
    protected boolean validateBackupStateForRemoval(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Boolean>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to acquire lock for backup [{}]. Cannot remove it.", backupId);
                    return false;
                }

                if (!allowedBackupStatesToRemove.contains(backupVO.getStatus())) {
                    logger.error("Backup [{}] is not in a state allowed to be removed. Current state is [{}]; allowed states are [{}]", backupVO, backupVO.getStatus(),
                            allowedBackupStatesToRemove);
                    return false;
                }

                if (Backup.CompressionStatus.Compressing.equals(backupVO.getCompressionStatus())) {
                    logger.error("Backup [{}] is being compressed, we cannot delete it. Please wait for the compress process to end and try again later.", backupVO.getUuid());
                    return false;
                }

                if (Backup.ValidationStatus.Validating.equals(backupVO.getValidationStatus())) {
                    logger.error("Backup [{}] is being validated, we cannot delete it. Please wait for the validation process to end and try again later.");
                    return false;
                }
                return true;
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    /**
     * Validates that the backup is in a valid state to start the compression. This is synchronized with the backup removal check. We get a new backup reference to make sure the
     * delete process has not changed the backup state.
     * */
    protected Pair<Boolean, BackupVO> validateBackupStateForStartCompressionAndUpdateCompressionStatus(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Pair<Boolean, BackupVO>>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to get lock on backup [{}]. Will abort the start of the compression process. We might try again later.", backupId);
                    return new Pair<>(false, null);
                }

                if (!allowedBackupStatesToCompress.contains(backupVO.getStatus())) {
                    logger.error("We can only compress backups that are on states [{}]. Current backup state is [{}].", allowedBackupStatesToCompress, backupVO.getStatus());
                    return new Pair<>(false, null);
                }

                logger.info("Compressing backup [{}].", backupVO.getUuid());
                backupVO.setCompressionStatus(Backup.CompressionStatus.Compressing);
                backupDao.update(backupVO.getId(), backupVO);
                return new Pair<>(true, backupVO);
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    /**
     * Validates that the backup is in a valid state to finalize the compression. This is synchronized with the backup restore check. We get a new backup reference to make sure
     * the restore process has not changed the backup state.
     * */
    protected Pair<Boolean, BackupVO> validateBackupStateForFinalizeCompression(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Pair<Boolean, BackupVO>>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to get lock on backup [{}]. Will abort the finalize compression process. We might try again later.", backupId);
                    return new Pair<>(false, null);
                }

                List<InternalBackupJoinVO> children = getBackupJoinChildren(backupVO);
                if (Backup.Status.Restoring == backupVO.getStatus() || children.stream().anyMatch(backup -> backup.getStatus() == Backup.Status.Restoring)) {
                    logger.warn(
                            "Backup [{}] not in right state to finish compression. We can only finish compression process if backup is in [{}] state and no children are being " + "restored. Will try again later",
                            backupVO, Backup.Status.BackedUp);
                    return new Pair<>(false, null);
                }

                if (Backup.Status.BackedUp == backupVO.getStatus()) {
                    logger.info("Backup [{}] is in the right state to finish compression. Will start the process.", backupVO.getUuid());
                    backupVO.setCompressionStatus(Backup.CompressionStatus.FinalizingCompression);
                    backupDao.update(backupId, backupVO);
                } else {
                    logger.warn(
                            "Backup [{}] is in [{}] state. Aborting compression and cleaning up compressed data. We can only finish compression process if backup is in [{}] " + "state.",
                            backupVO.getUuid(), backupVO.getStatus(), Backup.Status.BackedUp);
                    backupVO.setCompressionStatus(Backup.CompressionStatus.CompressionError);
                    backupDao.update(backupId, backupVO);
                }
                return new Pair<>(true, backupVO);
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    protected Pair<Boolean, Backup.Status> validateBackupStateForRestoreBackupToVM(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Pair<Boolean, Backup.Status>>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to get lock on backup [{}]. Cannot create VM from this backup right now.", backupId);
                    return new Pair<>(false, null);
                }

                if (Backup.Status.BackedUp == backupVO.getStatus() || Backup.Status.Restoring == backupVO.getStatus()) {
                    logger.debug("Backup [{}] is in the right state to create VM from it. Will start the process.", backupVO.getUuid());
                    Backup.Status oldStatus = backupVO.getStatus();
                    backupVO.setStatus(Backup.Status.Restoring);
                    backupDao.update(backupId, backupVO);
                    return new Pair<>(true, oldStatus);
                } else {
                    logger.warn(
                            "Backup [{}] is in [{}] state. Aborting restore. We can only restore the backup if backup is in [{}] states.",
                            backupVO.getUuid(), backupVO.getStatus(), List.of(Backup.Status.BackedUp, Backup.Status.Restoring));
                    return new Pair<>(false, null);
                }
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    /**
     * Validates that the backup is in a valid state to validate. This is synchronized with the backup removal check. We get a new backup reference to make sure the removal process
     * has not changed the backup state.
     * */
    protected boolean validateBackupStateForValidation(long backupId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<Boolean>) result -> {
            try {
                BackupVO backupVO = lockBackup(backupId);
                if (backupVO == null) {
                    logger.warn("Unable to acquire lock for backup [{}]. Cannot validate it.", backupId);
                    return false;
                }

                if (!allowedBackupStatesToValidate.contains(backupVO.getStatus())) {
                    logger.error("Backup [{}] is not in a state allowed to be validated. Current state is [{}]; allowed states are [{}]", backupVO, backupVO.getStatus(),
                            allowedBackupStatesToValidate);
                    return false;
                }
                return true;
            } finally {
                releaseBackup(backupId);
            }
        });
    }

    protected void validateStorages(List<VolumeObjectTO> volumeTOs, String vmUuid) {
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            StoragePoolVO storagePoolVO = storagePoolDao.findById(volumeObjectTO.getPoolId());
            if (!supportedStoragePoolTypes.contains(storagePoolVO.getPoolType())) {
                logger.error("Only able to take backups of VMs with volumes in the following storage types [{}]. Throwing an exception.", supportedStoragePoolTypes);
                throw new BackupProviderException(String.format("Unable to take backup of VM [%s], please check the logs.", vmUuid));
            }
        }
    }

    protected void validateNoVmSnapshots(VirtualMachine vm) {
        List<VMSnapshotVO> vmSnapshotVOs = vmSnapshotDao.findByVm(vm.getId());
        if (!vmSnapshotVOs.isEmpty()) {
            throw new BackupProviderException(String.format("Restoring VM [%s] would remove the current VM snapshots it has. Please remove the VM snapshots [%s] before" +
                    " restoring the backup.", vm.getUuid(), vmSnapshotVOs.stream().map(VMSnapshotVO::getUuid).collect(Collectors.toList())));
        }
    }

    protected BackupVO lockBackup(long backupId) {
        return backupDao.acquireInLockTable(backupId, 300);
    }

    protected void releaseBackup(long backupId) {
        backupDao.releaseFromLockTable(backupId);
    }

    protected void transitVmStateWithoutThrow(VirtualMachine vm, VirtualMachine.Event event, long hostId) {
        try {
            virtualMachineManager.stateTransitTo(vm, event, hostId);
        } catch (NoTransitionException e) {
            String msg = String.format("Failed to change VM [%s] state with event [%s].", vm.getUuid(), event.toString());
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected void transitVolumeStateWithoutThrow(Volume volume, Volume.Event event) {
        try {
            volumeApiService.stateTransitTo(volume, event);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
