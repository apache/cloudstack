//
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
//
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Upload;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.ReflectionUse;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkDeleteBackup;
import com.cloud.vm.VmWorkJobHandler;
import com.cloud.vm.VmWorkJobHandlerProxy;
import com.cloud.vm.VmWorkRestoreBackup;
import com.cloud.vm.VmWorkRestoreVolumeBackupAndAttach;
import com.cloud.vm.VmWorkTakeBackup;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.apache.cloudstack.backup.dao.InternalBackupStoragePoolDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class InternalBackupServiceImpl extends ComponentLifecycleBase implements InternalBackupService, VmWorkJobHandler {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private InternalBackupStoragePoolDao internalBackupStoragePoolDao;

    @Inject
    private BackupManager backupManager;
    @Inject
    private BackupDao backupDao;
    @Inject
    private AsyncJobManager jobManager;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private InternalBackupJoinDao internalBackupJoinDao;
    @Inject
    private BackupDetailsDao backupDetailDao;

    @Inject
    private ImageStoreObjectDownloadDao imageStoreObjectDownloadDao;

    @Inject
    private DataStoreManager dataStoreMgr;

    private VmWorkJobHandlerProxy jobHandlerProxy = new VmWorkJobHandlerProxy(this);
    private HashMap<String, InternalBackupProvider> internalBackupProviderMap = new HashMap<>();
    private List<InternalBackupProvider> internalBackupProviders;

    public void setInternalBackupProviders(final List<InternalBackupProvider> internalBackupProviders) {
        this.internalBackupProviders = internalBackupProviders;
    }

    @Override
    public boolean start() {
        super.start();

        if (internalBackupProviders != null) {
            for (InternalBackupProvider internalBackupProvider : internalBackupProviders) {
                internalBackupProviderMap.put(internalBackupProvider.getName().toLowerCase(), internalBackupProvider);
            }
        }
        return true;
    }

    @Override
    public void configureChainInfo(DataTO volumeTo, Command cmd) {
        if (!(volumeTo instanceof VolumeObjectTO)) {
            return;
        }
        VolumeObjectTO volumeObjectTO = (VolumeObjectTO) volumeTo;
        InternalBackupStoragePoolVO backupDelta = internalBackupStoragePoolDao.findOneByVolumeId(volumeObjectTO.getVolumeId());
        if (backupDelta == null) {
            return;
        }
        volumeObjectTO.setChainInfo(backupDelta.getBackupDeltaParentPath());
        if (cmd instanceof DeleteCommand) {
            ((DeleteCommand) cmd).setDeleteChain(true);
        }
        if (cmd instanceof RevertSnapshotCommand) {
            ((RevertSnapshotCommand) cmd).setDeleteChain(true);
        }
        logger.debug("Configured chain info for volume [{}]. Set it as [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getChainInfo());
    }

    @Override
    public void cleanupBackupMetadata(long volumeId) {
        logger.debug("Cleaning up backup metadata for volume [{}].", volumeId);
        InternalBackupStoragePoolVO delta = internalBackupStoragePoolDao.findOneByVolumeId(volumeId);
        if (delta == null) {
            return;
        }
        internalBackupStoragePoolDao.expungeByVolumeId(volumeId);
        if (CollectionUtils.isNotEmpty(internalBackupStoragePoolDao.listByBackupId(delta.getBackupId()))) {
            return;
        }

        InternalBackupJoinVO joinVO = internalBackupJoinDao.findById(delta.getBackupId());
        logger.debug("Volume [{}] was the last volume with deltas in backup [{}]. Setting the backup as not current and not END_OF_CHAIN.", volumeId, joinVO.getUuid());
        backupDetailDao.removeDetail(joinVO.getId(), BackupDetailsDao.CURRENT);
        if (!joinVO.getEndOfChain()) {
            backupDetailDao.persist(new BackupDetailVO(joinVO.getId(), BackupDetailsDao.END_OF_CHAIN, Boolean.TRUE.toString(), true));
        }
    }


    @Override
    public void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine) {
        if (isBackupFrameworkDisabled(virtualMachine)) {
            return;
        }

        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(virtualMachine.getDataCenterId());
        if (internalBackupProvider == null) {
            return;
        }
        internalBackupProvider.prepareVolumeForDetach(volume, virtualMachine);
    }

    @Override
    public void prepareVolumeForMigration(Volume volume) {
        if (volume.getInstanceId() == null) {
            return;
        }
        VirtualMachine virtualMachine = virtualMachineManager.findById(volume.getInstanceId());
        if (isBackupFrameworkDisabled(virtualMachine)) {
            return;
        }
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(volume.getDataCenterId());
        if (internalBackupProvider == null) {
            return;
        }
        internalBackupProvider.prepareVolumeForMigration(volume, virtualMachine);
    }

    @Override
    public void updateVolumeId(long oldVolumeId, long newVolumeId) {
        VolumeVO volumeVO = volumeDao.findById(newVolumeId);
        if (volumeVO.getInstanceId() == null) {
            return;
        }
        VirtualMachine virtualMachine = virtualMachineManager.findById(volumeVO.getInstanceId());
        if (isBackupFrameworkDisabled(virtualMachine)) {
            return;
        }

        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(virtualMachine.getDataCenterId());
        if (internalBackupProvider == null) {
            return;
        }
        internalBackupProvider.updateVolumeId(virtualMachine, oldVolumeId, newVolumeId);
    }

    @Override
    public void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot) {
        VirtualMachine virtualMachine = virtualMachineManager.findById(vmSnapshot.getVmId());
        if (isBackupFrameworkDisabled(virtualMachine)) {
            return;
        }

        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(virtualMachine.getDataCenterId());
        if (internalBackupProvider == null) {
            return;
        }
        internalBackupProvider.prepareVmForSnapshotRevert(vmSnapshot, virtualMachine);
    }

    /**
     * Ask the backup provider to get the necessary secondary storages that must be mounted at VM start.
     * <br/>
     * Note: This is currently only used for Backup Validation VMs. As they are created with backing files that are on secondary storage.
     * */
    @Override
    public Set<String> getSecondaryStorageUrls(UserVm userVm) {
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(userVm.getDataCenterId());
        if (internalBackupProvider == null) {
            return Set.of();
        }
        return internalBackupProvider.getSecondaryStorageUrls(userVm);
    }

    @Override
    public boolean startBackupCompression(long backupId, long hostId, long zoneId) {
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(zoneId);
        if (internalBackupProvider == null) {
            return false;
        }
        return internalBackupProvider.startBackupCompression(backupId, hostId);
    }

    @Override
    public boolean finalizeBackupCompression(long backupId, long hostId, long zoneId) {
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(zoneId);
        if (internalBackupProvider == null) {
            return false;
        }
        return internalBackupProvider.finalizeBackupCompression(backupId, hostId);
    }

    @Override
    public boolean validateBackup(long backupId, long hostId, long zoneId) {
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(zoneId);
        if (internalBackupProvider == null) {
            return false;
        }
        return internalBackupProvider.validateBackup(backupId, hostId);
    }

    @Override
    public ExtractResponse downloadScreenshot(long backupId) {
        BackupDetailVO screenshotPathDetail = backupDetailDao.findDetail(backupId, BackupDetailsDao.SCREENSHOT_PATH);
        ExtractResponse response = new ExtractResponse();
        if (screenshotPathDetail == null) {
            response.setState(Upload.Status.DOWNLOAD_URL_NOT_CREATED.toString());
            return response;
        }
        BackupDetailVO imageStoreId = backupDetailDao.findDetail(backupId, BackupDetailsDao.IMAGE_STORE_ID);
        ImageStoreEntity imageStore = (ImageStoreEntity) dataStoreMgr.getDataStore(Long.parseLong(imageStoreId.getValue()), DataStoreRole.Image);
        String screenshotPath = screenshotPathDetail.getValue();
        ImageStoreObjectDownloadVO imageStoreObj = imageStoreObjectDownloadDao.findByStoreIdAndPath(Long.parseLong(imageStoreId.getValue()), screenshotPath);

        if (imageStoreObj == null) {
            String downloadUrl = imageStore.createEntityExtractUrl(screenshotPath, Storage.ImageFormat.PNG, null);
            imageStoreObj = imageStoreObjectDownloadDao.persist(new ImageStoreObjectDownloadVO(imageStore.getId(), screenshotPath, downloadUrl));
        }

        if (imageStoreObj != null) {
            response.setUrl(imageStoreObj.getDownloadUrl());
            response.setName(screenshotPath.substring(screenshotPath.lastIndexOf("/") + 1));
            response.setState(Upload.Status.DOWNLOAD_URL_CREATED.toString());
        } else {
            response.setState(Upload.Status.DOWNLOAD_URL_NOT_CREATED.toString());
        }
        return response;
    }

    @Override
    public boolean finishBackupChain(long vmId) {
        VirtualMachine vm = virtualMachineManager.findById(vmId);
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(vm.getDataCenterId());
        if (internalBackupProvider == null) {
            return false;
        }
        return internalBackupProvider.finishBackupChain(vm);
    }

    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(VmWork work) throws Exception {
        return jobHandlerProxy.handleVmWorkJob(work);
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateTakeBackup(VmWorkTakeBackup work) {
        BackupVO backupVO = backupDao.findById(work.getBackupId());
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(backupVO.getZoneId());
        if (internalBackupProvider == null) {
            return new Pair<>(JobInfo.Status.FAILED, jobManager.marshallResultObject(Boolean.FALSE));
        }
        return new Pair<>(JobInfo.Status.SUCCEEDED, jobManager.marshallResultObject(internalBackupProvider.orchestrateTakeBackup(backupVO, work.isQuiesceVm(), work.isIsolated())));
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateDeleteBackup(VmWorkDeleteBackup work) {
        BackupVO backupVO = backupDao.findById(work.getBackupId());
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(backupVO.getZoneId());
        if (internalBackupProvider == null) {
            return new Pair<>(JobInfo.Status.FAILED, jobManager.marshallResultObject(Boolean.FALSE));
        }
        return new Pair<>(JobInfo.Status.SUCCEEDED, jobManager.marshallResultObject(internalBackupProvider.orchestrateDeleteBackup(backupVO, work.isForced())));
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateRestoreVMFromBackup(VmWorkRestoreBackup work) {
        BackupVO backupVO = backupDao.findById(work.getBackupId());
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(backupVO.getZoneId());
        if (internalBackupProvider == null) {
            return new Pair<>(JobInfo.Status.FAILED, jobManager.marshallResultObject(Boolean.FALSE));
        }
        return new Pair<>(JobInfo.Status.SUCCEEDED, jobManager.marshallResultObject(internalBackupProvider.orchestrateRestoreVMFromBackup(backupVO,
                userVmDao.findById(work.getVmId()), work.isQuickRestore(), work.getHostId(), true)));
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateRestoreBackupVolumeAndAttachToVM(VmWorkRestoreVolumeBackupAndAttach work) {
        BackupVO backupVO = backupDao.findById(work.getBackupId());
        InternalBackupProvider internalBackupProvider = getInternalBackupProviderForZone(backupVO.getZoneId());
        if (internalBackupProvider == null) {
            return new Pair<>(JobInfo.Status.FAILED, jobManager.marshallResultObject(Boolean.FALSE));
        }
        return new Pair<>(JobInfo.Status.SUCCEEDED, jobManager.marshallResultObject(internalBackupProvider.orchestrateRestoreBackedUpVolume(backupVO, userVmDao.findById(work.getVmId()),
                work.getBackupVolumeInfo(), work.getHostIp(), work.isQuickRestore())));
    }

    protected InternalBackupProvider getInternalBackupProviderForZone(long zoneId) {
        return Transaction.execute(TransactionLegacy.CLOUD_DB, (TransactionCallback<InternalBackupProvider>)status -> {
            BackupProvider backupProvider = backupManager.getBackupProvider(zoneId);
            return internalBackupProviderMap.get(backupProvider.getName());
        });
    }

    protected boolean isBackupFrameworkDisabled(VirtualMachine virtualMachine) {
        return !BackupManager.BackupFrameworkEnabled.valueIn(virtualMachine.getDataCenterId());
    }
}
