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
package org.apache.cloudstack.storage.datastore.driver;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.storage.VolumeVO;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

public class CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> caps = new HashMap<String, String>();
        caps.put(DataStoreCapabilities.VOLUME_SNAPSHOT_QUIESCEVM.toString(), Boolean.FALSE.toString());
        return caps;
    }

    private static final Logger s_logger = Logger.getLogger(CloudStackPrimaryDataStoreDriverImpl.class);
    private static final String NO_REMOTE_ENDPOINT_WITH_ENCRYPTION = "No remote endpoint to send command, unable to find a valid endpoint. Requires encryption support: %s";

    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    HostDao hostDao;
    @Inject
    StorageManager storageMgr;
    @Inject
    VMInstanceDao vmDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    @Inject
    SnapshotManager snapshotMgr;
    @Inject
    EndPointSelector epSelector;
    @Inject
    ConfigurationDao configDao;
    @Inject
    TemplateManager templateManager;
    @Inject
    TemplateDataFactory templateDataFactory;
    @Inject
    VolumeDataFactory volFactory;

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    public Answer createVolume(VolumeInfo volume) throws StorageUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + volume);
        }

        CreateObjectCommand cmd = new CreateObjectCommand(volume.getTO());
        boolean encryptionRequired = anyVolumeRequiresEncryption(volume);
        EndPoint ep = epSelector.select(volume, encryptionRequired);
        Answer answer = null;
        if (ep == null) {
            String errMsg = String.format(NO_REMOTE_ENDPOINT_WITH_ENCRYPTION, encryptionRequired);
            s_logger.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        return answer;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        return false;
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {
        return dataObject.getSize();
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        String errMsg = null;
        Answer answer = null;
        CreateCmdResult result = new CreateCmdResult(null, null);
        if (data.getType() == DataObjectType.VOLUME) {
            try {
                answer = createVolume((VolumeInfo) data);
                if ((answer == null) || (!answer.getResult())) {
                    result.setSuccess(false);
                    if (answer != null) {
                        result.setResult(answer.getDetails());
                    }
                } else {
                    result.setAnswer(answer);
                }
            } catch (Exception e) {
                s_logger.debug("failed to create volume", e);
                errMsg = e.toString();
            }
        }
        if (errMsg != null) {
            result.setResult(errMsg);
        }

        if (callback != null) {
            callback.complete(result);
        }
    }

    private boolean commandCanBypassHostMaintenance(DataObject data) {
        if (DataObjectType.VOLUME.equals(data.getType())) {
            Volume volume = (Volume)data;
            if (volume.getInstanceId() != null) {
                VMInstanceVO vm = vmDao.findById(volume.getInstanceId());
                return vm != null && (VirtualMachine.Type.SecondaryStorageVm.equals(vm.getType()) ||
                        VirtualMachine.Type.ConsoleProxy.equals(vm.getType()));
            }
        }
        return false;
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        DeleteCommand cmd = new DeleteCommand(data.getTO());
        cmd.setBypassHostMaintenance(commandCanBypassHostMaintenance(data));
        CommandResult result = new CommandResult();
        try {
            EndPoint ep = null;
            if (data.getType() == DataObjectType.VOLUME) {
                ep = epSelector.select(data, StorageAction.DELETEVOLUME);
            } else {
                ep = epSelector.select(data);
            }
            if (ep == null) {
                String errMsg = "No remote endpoint to send DeleteCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                result.setResult(errMsg);
            } else {
                Answer answer = ep.sendMessage(cmd);
                if (answer != null && !answer.getResult()) {
                    result.setResult(answer.getDetails());
                }
            }
        } catch (Exception ex) {
            s_logger.debug("Unable to destoy volume" + data.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        s_logger.debug(String.format("Copying volume %s(%s) to %s(%s)", srcdata.getId(), srcdata.getType(), destData.getId(), destData.getType()));
        boolean encryptionRequired = anyVolumeRequiresEncryption(srcdata, destData);
        DataStore store = destData.getDataStore();
        if (store.getRole() == DataStoreRole.Primary) {
            if ((srcdata.getType() == DataObjectType.TEMPLATE && destData.getType() == DataObjectType.TEMPLATE)) {
                //For CLVM, we need to copy template to primary storage at all, just fake the copy result.
                TemplateObjectTO templateObjectTO = new TemplateObjectTO();
                templateObjectTO.setPath(UUID.randomUUID().toString());
                templateObjectTO.setSize(srcdata.getSize());
                templateObjectTO.setPhysicalSize(srcdata.getSize());
                templateObjectTO.setFormat(Storage.ImageFormat.RAW);
                CopyCmdAnswer answer = new CopyCmdAnswer(templateObjectTO);
                CopyCommandResult result = new CopyCommandResult("", answer);
                callback.complete(result);
            } else if (srcdata.getType() == DataObjectType.TEMPLATE && destData.getType() == DataObjectType.VOLUME) {
                //For CLVM, we need to pass template on secondary storage to hypervisor
                int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();
                StoragePoolVO storagePoolVO = primaryStoreDao.findById(store.getId());
                DataStore imageStore = templateManager.getImageStore(storagePoolVO.getDataCenterId(), srcdata.getId());
                DataObject srcData = templateDataFactory.getTemplate(srcdata.getId(), imageStore);

                CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), primaryStorageDownloadWait, true);
                EndPoint ep = epSelector.select(srcData, destData, encryptionRequired);
                Answer answer = null;
                if (ep == null) {
                    String errMsg = String.format(NO_REMOTE_ENDPOINT_WITH_ENCRYPTION, encryptionRequired);
                    s_logger.error(errMsg);
                    answer = new Answer(cmd, false, errMsg);
                } else {
                    s_logger.debug(String.format("Sending copy command to endpoint %s, where encryption support is %s", ep.getHostAddr(), encryptionRequired ? "required" : "not required"));
                    answer = ep.sendMessage(cmd);
                }
                CopyCommandResult result = new CopyCommandResult("", answer);
                callback.complete(result);
            } else if (srcdata.getType() == DataObjectType.SNAPSHOT && destData.getType() == DataObjectType.VOLUME) {
                SnapshotObjectTO srcTO = (SnapshotObjectTO) srcdata.getTO();
                CopyCommand cmd = new CopyCommand(srcTO, destData.getTO(), StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value(), true);
                EndPoint ep = epSelector.select(srcdata, destData, encryptionRequired);
                CopyCmdAnswer answer = null;
                if (ep == null) {
                    String errMsg = String.format(NO_REMOTE_ENDPOINT_WITH_ENCRYPTION, encryptionRequired);
                    s_logger.error(errMsg);
                    answer = new CopyCmdAnswer(errMsg);
                } else {
                    answer = (CopyCmdAnswer) ep.sendMessage(cmd);
                }
                CopyCommandResult result = new CopyCommandResult("", answer);
                callback.complete(result);
            }
        }
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, callback);
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        //BUG fix for CLOUDSTACK-4618
        DataStore destStore = destData.getDataStore();
        if (destStore.getRole() == DataStoreRole.Primary && srcData.getType() == DataObjectType.TEMPLATE
                && (destData.getType() == DataObjectType.TEMPLATE || destData.getType() == DataObjectType.VOLUME)) {
            StoragePoolVO storagePoolVO = primaryStoreDao.findById(destStore.getId());
            if (storagePoolVO != null && storagePoolVO.getPoolType() == Storage.StoragePoolType.CLVM) {
                return true;
            }
        } else if (DataObjectType.SNAPSHOT.equals(srcData.getType()) && DataObjectType.VOLUME.equals(destData.getType())) {
            DataStore srcStore = srcData.getDataStore();
            if (DataStoreRole.Primary.equals(srcStore.getRole()) && DataStoreRole.Primary.equals(destStore.getRole())) {
                StoragePoolVO srcStoragePoolVO = primaryStoreDao.findById(srcStore.getId());
                StoragePoolVO dstStoragePoolVO = primaryStoreDao.findById(destStore.getId());
                if (srcStoragePoolVO != null && StoragePoolType.RBD.equals(srcStoragePoolVO.getPoolType())
                        && dstStoragePoolVO != null && (StoragePoolType.RBD.equals(dstStoragePoolVO.getPoolType())
                        || StoragePoolType.NetworkFilesystem.equals(dstStoragePoolVO.getPoolType()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        s_logger.debug("Taking snapshot of "+ snapshot);
        try {
            SnapshotObjectTO snapshotTO = (SnapshotObjectTO) snapshot.getTO();
            Object payload = snapshot.getPayload();
            if (payload != null && payload instanceof CreateSnapshotPayload) {
                CreateSnapshotPayload snapshotPayload = (CreateSnapshotPayload) payload;
                snapshotTO.setQuiescevm(snapshotPayload.getQuiescevm());
            }

            boolean encryptionRequired = anyVolumeRequiresEncryption(snapshot);
            CreateObjectCommand cmd = new CreateObjectCommand(snapshotTO);
            EndPoint ep = epSelector.select(snapshot, StorageAction.TAKESNAPSHOT, encryptionRequired);
            Answer answer = null;

            s_logger.debug("Taking snapshot of "+ snapshot + " and encryption required is " + encryptionRequired);

            if (ep == null) {
                String errMsg = "No remote endpoint to send createObjectCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                answer = new Answer(cmd, false, errMsg);
            } else {
                answer = ep.sendMessage(cmd);
            }

            result = new CreateCmdResult(null, answer);
            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
            }

            callback.complete(result);
            return;
        } catch (Exception e) {
            s_logger.debug("Failed to take snapshot: " + snapshot.getId(), e);
            result = new CreateCmdResult(null, null);
            result.setResult(e.toString());
        }
        callback.complete(result);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {
        SnapshotObjectTO dataOnPrimaryStorage = null;
        if (snapshotOnPrimaryStore != null) {
            dataOnPrimaryStorage = (SnapshotObjectTO)snapshotOnPrimaryStore.getTO();
        }
        RevertSnapshotCommand cmd = new RevertSnapshotCommand((SnapshotObjectTO)snapshot.getTO(), dataOnPrimaryStorage);

        CommandResult result = new CommandResult();
        try {
            EndPoint ep = null;
            if (snapshotOnPrimaryStore != null) {
                ep = epSelector.select(snapshotOnPrimaryStore);
            } else {
                VolumeInfo volumeInfo = volFactory.getVolume(snapshot.getVolumeId(), DataStoreRole.Primary);
                ep = epSelector.select(volumeInfo);
            }
            if ( ep == null ){
                String errMsg = "No remote endpoint to send RevertSnapshotCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                result.setResult(errMsg);
            } else {
                Answer answer = ep.sendMessage(cmd);
                if (answer != null && !answer.getResult()) {
                    result.setResult(answer.getDetails());
                }
            }
        } catch (Exception ex) {
            s_logger.debug("Unable to revert snapshot " + snapshot.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        VolumeObject vol = (VolumeObject) data;
        StoragePool pool = (StoragePool) data.getDataStore();
        ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();
        boolean encryptionRequired = anyVolumeRequiresEncryption(vol);
        long [] endpointsToRunResize = resizeParameter.hosts;

        // if hosts are provided, they are where the VM last ran. We can use that.
        if (endpointsToRunResize == null || endpointsToRunResize.length == 0) {
            EndPoint ep = epSelector.select(data, encryptionRequired);
            endpointsToRunResize = new long[] {ep.getId()};
        }
        ResizeVolumeCommand resizeCmd = new ResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), vol.getSize(),
                resizeParameter.newSize, resizeParameter.shrinkOk, resizeParameter.instanceName, vol.getChainInfo(), vol.getPassphrase(), vol.getEncryptFormat());
        if (pool.getParent() != 0) {
            resizeCmd.setContextParam(DiskTO.PROTOCOL_TYPE, Storage.StoragePoolType.DatastoreCluster.toString());
        }
        CreateCmdResult result = new CreateCmdResult(null, null);
        try {
            ResizeVolumeAnswer answer = (ResizeVolumeAnswer) storageMgr.sendToPool(pool, endpointsToRunResize, resizeCmd);
            if (answer != null && answer.getResult()) {
                long finalSize = answer.getNewSize();
                s_logger.debug("Resize: volume started at size: " + toHumanReadableSize(vol.getSize()) + " and ended at size: " + toHumanReadableSize(finalSize));

                vol.setSize(finalSize);
                vol.update();

                updateVolumePathDetails(vol, answer);
            } else if (answer != null) {
                result.setResult(answer.getDetails());
            } else {
                s_logger.debug("return a null answer, mark it as failed for unknown reason");
                result.setResult("return a null answer, mark it as failed for unknown reason");
            }

        } catch (Exception e) {
            s_logger.debug("sending resize command failed", e);
            result.setResult(e.toString());
        } finally {
            resizeCmd.clearPassphrase();
        }

        callback.complete(result);
    }

    private void updateVolumePathDetails(VolumeObject vol, ResizeVolumeAnswer answer) {
        VolumeVO volumeVO = volumeDao.findById(vol.getId());
        String datastoreUUID = answer.getContextParam("datastoreUUID");
        if (datastoreUUID != null) {
            StoragePoolVO storagePoolVO = primaryStoreDao.findByUuid(datastoreUUID);
            if (storagePoolVO != null) {
                volumeVO.setPoolId(storagePoolVO.getId());
            } else {
                s_logger.warn(String.format("Unable to find datastore %s while updating the new datastore of the volume %d", datastoreUUID, vol.getId()));
            }
        }

        String volumePath = answer.getContextParam("volumePath");
        if (volumePath != null) {
            volumeVO.setPath(volumePath);
        }

        String chainInfo = answer.getContextParam("chainInfo");
        if (chainInfo != null) {
            volumeVO.setChainInfo(chainInfo);
        }

        volumeDao.update(volumeVO.getId(), volumeVO);
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {}

    @Override
    public boolean canProvideStorageStats() {
        return false;
    }

    @Override
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        return null;
    }

    @Override
    public boolean canProvideVolumeStats() {
        return false;
    }

    @Override
    public Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumeId) {
        return null;
    }

    @Override
    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        return true;
    }

    @Override
    public boolean isVmInfoNeeded() {
        return false;
    }

    @Override
    public void provideVmInfo(long vmId, long volumeId) {
    }

    @Override
    public boolean isVmTagsNeeded(String tagKey) {
        return false;
    }

    @Override
    public void provideVmTags(long vmId, long volumeId, String tagValue) {
    }

    /**
     * Does any object require encryption support?
     */
    private boolean anyVolumeRequiresEncryption(DataObject ... objects) {
        for (DataObject o : objects) {
            // this fails code smell for returning true twice, but it is more readable than combining all tests into one statement
            if (o instanceof VolumeInfo && ((VolumeInfo) o).getPassphraseId() != null) {
                return true;
            } else if (o instanceof SnapshotInfo && ((SnapshotInfo) o).getBaseVolume().getPassphraseId() != null) {
                return true;
            }
        }
        return false;
    }
}
