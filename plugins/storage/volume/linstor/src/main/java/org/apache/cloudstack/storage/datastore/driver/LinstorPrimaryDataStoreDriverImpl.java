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
package org.apache.cloudstack.storage.datastore.driver;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.CloneWaiter;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionCloneRequest;
import com.linbit.linstor.api.model.ResourceDefinitionCloneStarted;
import com.linbit.linstor.api.model.ResourceDefinitionCreate;
import com.linbit.linstor.api.model.ResourceGroupSpawn;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.Snapshot;
import com.linbit.linstor.api.model.SnapshotRestore;
import com.linbit.linstor.api.model.VolumeDefinitionModify;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.host.Host;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

public class LinstorPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(LinstorPrimaryDataStoreDriverImpl.class);
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDetailsDao _volumeDetailsDao;
    @Inject private VMTemplatePoolDao _vmTemplatePoolDao;
    @Inject private SnapshotDao _snapshotDao;
    @Inject private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject private StorageManager _storageMgr;

    public LinstorPrimaryDataStoreDriverImpl()
    {
    }

    @Override
    public Map<String, String> getCapabilities()
    {
        Map<String, String> mapCapabilities = new HashMap<>();

        // Linstor will be restricted to only run on LVM-THIN and ZFS storage pools with ACS
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString());

        // fetch if lvm-thin or ZFS
        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());

        // CAN_CREATE_VOLUME_FROM_SNAPSHOT see note from CAN_CREATE_VOLUME_FROM_VOLUME
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString(), Boolean.TRUE.toString());

        return mapCapabilities;
    }

    @Override
    public DataTO getTO(DataObject data)
    {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store)
    {
        return null;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject)
    {
        return null;
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore)
    {
        return false;
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore)
    {
    }

    @Override
    public long getUsedBytes(StoragePool storagePool)
    {
        return 0;
    }

    @Override
    public long getUsedIops(StoragePool storagePool)
    {
        return 0;
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool)
    {
        return dataObject.getSize();
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool)
    {
        return 0;
    }

    private String getSnapshotName(String snapshotUuid) {
        return LinstorUtil.RSC_PREFIX + snapshotUuid;
    }

    private void deleteResourceDefinition(StoragePoolVO storagePoolVO, String rscDefName)
    {
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());

        try
        {
            ApiCallRcList answers = linstorApi.resourceDefinitionDelete(rscDefName);
            if (answers.hasError())
            {
                for (ApiCallRc answer : answers)
                {
                    s_logger.error(answer.getMessage());
                }
                throw new CloudRuntimeException("Linstor: Unable to delete resource definition: " + rscDefName);
            }
        } catch (ApiException apiEx)
        {
            s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private void deleteSnapshot(@Nonnull DataStore dataStore, @Nonnull String rscDefName, @Nonnull String snapshotName)
    {
        StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());

        try
        {
            ApiCallRcList answers = linstorApi.resourceSnapshotDelete(rscDefName, snapshotName);
            if (answers.hasError())
            {
                for (ApiCallRc answer : answers)
                {
                    s_logger.error(answer.getMessage());
                }
                throw new CloudRuntimeException("Linstor: Unable to delete snapshot: " + rscDefName);
            }
        } catch (ApiException apiEx)
        {
            s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private long getCsIdForCloning(long volumeId, String cloneOf) {
        VolumeDetailVO volumeDetail = _volumeDetailsDao.findDetail(volumeId, cloneOf);

        if (volumeDetail != null && volumeDetail.getValue() != null) {
            return Long.parseLong(volumeDetail.getValue());
        }

        return Long.MIN_VALUE;
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback)
    {
        s_logger.debug("deleteAsync: " + dataObject.getType() + ";" + dataObject.getUuid());
        String errMsg = null;

        final long storagePoolId = dataStore.getId();
        final StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        switch (dataObject.getType()) {
            case VOLUME:
            {
                final VolumeInfo volumeInfo = (VolumeInfo) dataObject;
                final String rscName = LinstorUtil.RSC_PREFIX + volumeInfo.getPath();
                deleteResourceDefinition(storagePool, rscName);

                long usedBytes = storagePool.getUsedBytes();
                long capacityIops = storagePool.getCapacityIops();

                usedBytes -= volumeInfo.getSize();
                if (volumeInfo.getMaxIops() != null)
                    capacityIops += volumeInfo.getMaxIops();

                storagePool.setUsedBytes(Math.max(0, usedBytes));
                storagePool.setCapacityIops(Math.max(0, capacityIops));

                _storagePoolDao.update(storagePoolId, storagePool);
            }
                break;
            case SNAPSHOT:
                final SnapshotInfo snapshotInfo = (SnapshotInfo) dataObject;
                final String rscName = LinstorUtil.RSC_PREFIX + snapshotInfo.getBaseVolume().getPath();
                deleteSnapshot(dataStore, rscName, getSnapshotName(snapshotInfo.getUuid()));
                long usedBytes = storagePool.getUsedBytes() - snapshotInfo.getSize();
                storagePool.setUsedBytes(Math.max(0, usedBytes));
                _storagePoolDao.update(storagePoolId, storagePool);
                break;
            default:
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
                s_logger.error(errMsg);
        }

        if (callback != null) {
            CommandResult result = new CommandResult();
            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void logLinstorAnswer(@Nonnull ApiCallRc answer) {
        if (answer.isError()) {
            s_logger.error(answer.getMessage());
        } else if (answer.isWarning()) {
            s_logger.warn(answer.getMessage());
        } else if (answer.isInfo()) {
            s_logger.info(answer.getMessage());
        }
    }

    private void logLinstorAnswers(@Nonnull ApiCallRcList answers) {
        answers.forEach(this::logLinstorAnswer);
    }

    private void checkLinstorAnswersThrow(@Nonnull ApiCallRcList answers) {
        logLinstorAnswers(answers);
        if (answers.hasError())
        {
            String errMsg = answers.stream()
                .filter(ApiCallRc::isError)
                .findFirst()
                .map(ApiCallRc::getMessage).orElse("Unknown linstor error");
            throw new CloudRuntimeException(errMsg);
        }
    }

    private String checkLinstorAnswers(@Nonnull ApiCallRcList answers) {
        logLinstorAnswers(answers);
        return answers.stream().filter(ApiCallRc::isError).findFirst().map(ApiCallRc::getMessage).orElse(null);
    }

    private String getDeviceName(DevelopersApi linstorApi, String rscName) throws ApiException {
        List<ResourceWithVolumes> resources = linstorApi.viewResources(
            Collections.emptyList(),
            Collections.singletonList(rscName),
            Collections.emptyList(),
            null,
            null,
            null);
        if (!resources.isEmpty() && !resources.get(0).getVolumes().isEmpty())
        {
            s_logger.info("Linstor: Created drbd device: " + resources.get(0).getVolumes().get(0).getDevicePath());
            return resources.get(0).getVolumes().get(0).getDevicePath();
        } else
        {
            s_logger.error("Linstor: viewResources didn't return resources or volumes.");
            throw new CloudRuntimeException("Linstor: viewResources didn't return resources or volumes.");
        }
    }

    private String createResource(VolumeInfo vol, StoragePoolVO storagePoolVO)
    {
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());
        final String rscGrp = storagePoolVO.getUserInfo() != null && !storagePoolVO.getUserInfo().isEmpty() ?
            storagePoolVO.getUserInfo() : "DfltRscGrp";

        ResourceGroupSpawn rscGrpSpawn = new ResourceGroupSpawn();
        final String rscName = LinstorUtil.RSC_PREFIX + vol.getUuid();
        rscGrpSpawn.setResourceDefinitionName(rscName);
        rscGrpSpawn.addVolumeSizesItem(vol.getSize() / 1024);

        try
        {
            s_logger.debug("Linstor: Spawn resource " + rscName);
            ApiCallRcList answers = linstorApi.resourceGroupSpawn(rscGrp, rscGrpSpawn);
            checkLinstorAnswersThrow(answers);

            return getDeviceName(linstorApi, rscName);
        } catch (ApiException apiEx)
        {
            s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private String cloneResource(long csCloneId, VolumeInfo volumeInfo, StoragePoolVO storagePoolVO) {
        // get the cached template on this storage
        VMTemplateStoragePoolVO tmplPoolRef = _vmTemplatePoolDao.findByPoolTemplate(
            storagePoolVO.getId(), csCloneId, null);

        if (tmplPoolRef != null) {
            final String cloneRes = LinstorUtil.RSC_PREFIX + tmplPoolRef.getLocalDownloadPath();
            final String rscName = LinstorUtil.RSC_PREFIX + volumeInfo.getUuid();
            final DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());

            try {
                s_logger.debug("Clone resource definition " + cloneRes + " to " + rscName);
                ResourceDefinitionCloneRequest cloneRequest = new ResourceDefinitionCloneRequest();
                cloneRequest.setName(rscName);
                ResourceDefinitionCloneStarted cloneStarted = linstorApi.resourceDefinitionClone(
                    cloneRes, cloneRequest);

                checkLinstorAnswersThrow(cloneStarted.getMessages());

                if (!CloneWaiter.waitFor(linstorApi, cloneStarted)) {
                    throw new CloudRuntimeException("Clone for resource " + rscName + " failed.");
                }

                return getDeviceName(linstorApi, rscName);
            } catch (ApiException apiEx) {
                s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
                throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
            }
        } else {
            throw new CloudRuntimeException(
                "Unable to find Linstor resource for the following template data-object ID: " + csCloneId);
        }
    }

    private String createResourceFromSnapshot(long csSnapshotId, String rscName, StoragePoolVO storagePoolVO) {
        final String rscGrp = storagePoolVO.getUserInfo() != null && !storagePoolVO.getUserInfo().isEmpty() ?
            storagePoolVO.getUserInfo() : "DfltRscGrp";
        final DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());

        SnapshotVO snapshotVO = _snapshotDao.findById(csSnapshotId);
        String snapName = LinstorUtil.RSC_PREFIX + snapshotVO.getUuid();
        VolumeVO volumeVO = _volumeDao.findById(snapshotVO.getVolumeId());
        String cloneRes = LinstorUtil.RSC_PREFIX + volumeVO.getPath();

        try
        {
            s_logger.debug("Create new resource definition: " + rscName);
            ResourceDefinitionCreate rdCreate = new ResourceDefinitionCreate();
            ResourceDefinition rd = new ResourceDefinition();
            rd.setName(rscName);
            rd.setResourceGroupName(rscGrp);
            rdCreate.setResourceDefinition(rd);
            ApiCallRcList answers = linstorApi.resourceDefinitionCreate(rdCreate);
            checkLinstorAnswersThrow(answers);

            SnapshotRestore snapshotRestore = new SnapshotRestore();
            snapshotRestore.toResource(rscName);

            s_logger.debug("Create new volume definition for snapshot: " + cloneRes + ":" + snapName);
            answers = linstorApi.resourceSnapshotsRestoreVolumeDefinition(cloneRes, snapName, snapshotRestore);
            checkLinstorAnswersThrow(answers);

            // restore snapshot to new resource
            s_logger.debug("Restore resource from snapshot: " + cloneRes + ":" + snapName);
            answers = linstorApi.resourceSnapshotRestore(cloneRes, snapName, snapshotRestore);
            checkLinstorAnswersThrow(answers);

            return getDeviceName(linstorApi, rscName);
        } catch (ApiException apiEx) {
            s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private String createVolume(VolumeInfo volumeInfo, StoragePoolVO storagePoolVO) {
        long csSnapshotId = getCsIdForCloning(volumeInfo.getId(), "cloneOfSnapshot");
        long csTemplateId = getCsIdForCloning(volumeInfo.getId(), "cloneOfTemplate");

        if (csSnapshotId > 0) {
            return createResourceFromSnapshot(csSnapshotId, LinstorUtil.RSC_PREFIX + volumeInfo.getUuid(), storagePoolVO);
        } else if (csTemplateId > 0) {
            return cloneResource(csTemplateId, volumeInfo, storagePoolVO);
        } else {
            return createResource(volumeInfo, storagePoolVO);
        }
    }

    private void handleSnapshotDetails(long csSnapshotId, String name, String value) {
        _snapshotDetailsDao.removeDetail(csSnapshotId, name);
        SnapshotDetailsVO snapshotDetails = new SnapshotDetailsVO(csSnapshotId, name, value, false);
        _snapshotDetailsDao.persist(snapshotDetails);
    }

    private void addTempVolumeToDb(long csSnapshotId, String tempVolumeName) {
        // TEMP_VOLUME_ID is needed, to find which temporary resource should be deleted after copying it on agent side
        handleSnapshotDetails(csSnapshotId, LinstorUtil.TEMP_VOLUME_ID, LinstorUtil.RSC_PREFIX + tempVolumeName);
        // the iqn will be used on the agent side to copy from, even though linstor doesn't have anything to do with IQN
        handleSnapshotDetails(csSnapshotId, DiskTO.IQN, tempVolumeName);
    }

    private void removeTempVolumeFromDb(long csSnapshotId) {
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(csSnapshotId, LinstorUtil.TEMP_VOLUME_ID);

        if (snapshotDetails == null || snapshotDetails.getValue() == null) {
            throw new CloudRuntimeException(
                "'removeTempVolumeId' should not be invoked unless " + LinstorUtil.TEMP_VOLUME_ID + " exists.");
        }

        String originalVolumeId = snapshotDetails.getValue();

        handleSnapshotDetails(csSnapshotId, LinstorUtil.TEMP_VOLUME_ID, originalVolumeId);

        _snapshotDetailsDao.remove(snapshotDetails.getId());
    }

    private void createVolumeFromSnapshot(SnapshotInfo snapshotInfo, StoragePoolVO storagePoolVO) {
        long csSnapshotId = snapshotInfo.getId();

        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(csSnapshotId, "tempVolume");

        if (snapshotDetails != null && snapshotDetails.getValue() != null &&
            snapshotDetails.getValue().equalsIgnoreCase("create"))
        {
            final String csName = "Temp-" + snapshotInfo.getUuid();
            final String tempRscName = LinstorUtil.RSC_PREFIX + csName;
            createResourceFromSnapshot(csSnapshotId, tempRscName, storagePoolVO);

            s_logger.debug("Temp resource created: " + tempRscName);
            addTempVolumeToDb(csSnapshotId, csName);
        }
        else if (snapshotDetails != null && snapshotDetails.getValue() != null &&
            snapshotDetails.getValue().equalsIgnoreCase("delete"))
        {
            snapshotDetails = _snapshotDetailsDao.findDetail(csSnapshotId, LinstorUtil.TEMP_VOLUME_ID);

            deleteResourceDefinition(storagePoolVO, snapshotDetails.getValue());

            s_logger.debug("Temp resource deleted: " + snapshotDetails.getValue());
            removeTempVolumeFromDb(csSnapshotId);
        }
        else {
            throw new CloudRuntimeException("Invalid state in 'createVolumeFromSnapshot(SnapshotInfo, StoragePoolVO)'");
        }
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject vol, AsyncCompletionCallback<CreateCmdResult> callback)
    {
        String devPath = null;
        String errMsg = null;
        StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

        try
        {
            switch (vol.getType())
            {
                case VOLUME:
                    VolumeInfo volumeInfo = (VolumeInfo) vol;
                    VolumeVO volume = _volumeDao.findById(volumeInfo.getId());
                    s_logger.debug("createAsync - creating volume");
                    devPath = createVolume(volumeInfo, storagePool);
                    volume.setFolder("/dev/");
                    volume.setPoolId(storagePool.getId());
                    volume.setUuid(vol.getUuid());
                    volume.setPath(vol.getUuid());

                    _volumeDao.update(volume.getId(), volume);
                    break;
                case SNAPSHOT:
                    s_logger.debug("createAsync - SNAPSHOT");
                    createVolumeFromSnapshot((SnapshotInfo) vol, storagePool);
                    break;
                case TEMPLATE:
                    errMsg = "creating template - not supported";
                    s_logger.error("createAsync - " + errMsg);
                    break;
                default:
                    errMsg = "Invalid DataObjectType (" + vol.getType() + ") passed to createAsync";
                    s_logger.error(errMsg);
            }
        } catch (Exception ex)
        {
            errMsg = ex.getMessage();

            s_logger.error("createAsync: " + errMsg);
            if (callback == null)
            {
                throw ex;
            }
        }

        if (callback != null)
        {
            CreateCmdResult result = new CreateCmdResult(devPath, new Answer(null, errMsg == null, errMsg));
            result.setResult(errMsg);
            callback.complete(result);
        }
    }

    @Override
    public void revertSnapshot(
        SnapshotInfo snapshot,
        SnapshotInfo snapshotOnPrimaryStore,
        AsyncCompletionCallback<CommandResult> callback)
    {
        s_logger.debug("Linstor: revertSnapshot");
        final VolumeInfo volumeInfo = snapshot.getBaseVolume();
        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());
        if (volumeVO == null || volumeVO.getRemoved() != null) {
            CommandResult commandResult = new CommandResult();
            commandResult.setResult("The volume that the snapshot belongs to no longer exists.");
            callback.complete(commandResult);
            return;
        }

        String resultMsg;
        try {
            final StoragePool pool = (StoragePool) snapshot.getDataStore();
            final String rscName = LinstorUtil.RSC_PREFIX + volumeInfo.getUuid();
            final String snapName = LinstorUtil.RSC_PREFIX + snapshot.getUuid();
            final DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(pool.getHostAddress());

            ApiCallRcList answers = linstorApi.resourceSnapshotRollback(rscName, snapName);
            resultMsg = checkLinstorAnswers(answers);
        } catch (ApiException apiEx) {
            s_logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            resultMsg = apiEx.getBestMessage();
        }

        if (callback != null)
        {
            CommandResult result = new CommandResult();
            result.setResult(resultMsg);
            callback.complete(result);
        }
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData)
    {
        return false;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback)
    {
        // as long as canCopy is false, this isn't called
        s_logger.debug("Linstor: copyAsync with srcdata: " + srcData.getUuid());
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback)
    {
        // as long as canCopy is false, this isn't called
        s_logger.debug("Linstor: copyAsync with srcdata: " + srcData.getUuid());
    }

    private CreateCmdResult notifyResize(
        DataObject data,
        long oldSize,
        ResizeVolumePayload resizeParameter)
    {
        VolumeObject vol = (VolumeObject) data;
        StoragePool pool = (StoragePool) data.getDataStore();

        ResizeVolumeCommand resizeCmd =
            new ResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), oldSize, resizeParameter.newSize, resizeParameter.shrinkOk,
                resizeParameter.instanceName, null);
        CreateCmdResult result = new CreateCmdResult(null, null);
        try {
            ResizeVolumeAnswer answer = (ResizeVolumeAnswer) _storageMgr.sendToPool(pool, resizeParameter.hosts, resizeCmd);
            if (answer != null && answer.getResult()) {
                s_logger.debug("Resize: notified hosts");
            } else if (answer != null) {
                result.setResult(answer.getDetails());
            } else {
                s_logger.debug("return a null answer, mark it as failed for unknown reason");
                result.setResult("return a null answer, mark it as failed for unknown reason");
            }

        } catch (Exception e) {
            s_logger.debug("sending resize command failed", e);
            result.setResult(e.toString());
        }

        return result;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback)
    {
        final VolumeObject vol = (VolumeObject) data;
        final StoragePool pool = (StoragePool) data.getDataStore();
        final DevelopersApi api = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        final ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();

        final String rscName = LinstorUtil.RSC_PREFIX + vol.getPath();
        final long oldSize = vol.getSize();

        String errMsg = null;
        VolumeDefinitionModify dfm = new VolumeDefinitionModify();
        dfm.setSizeKib(resizeParameter.newSize / 1024);
        try
        {
            ApiCallRcList answers = api.volumeDefinitionModify(rscName, 0, dfm);
            if (answers.hasError())
            {
                s_logger.error("Resize error: " + answers.get(0).getMessage());
                errMsg = answers.get(0).getMessage();
            } else
            {
                s_logger.info(String.format("Successfully resized %s to %d kib", rscName, dfm.getSizeKib()));
                vol.setSize(resizeParameter.newSize);
                vol.update();
            }

        } catch (ApiException apiExc)
        {
            s_logger.error(apiExc);
            errMsg = apiExc.getBestMessage();
        }

        CreateCmdResult result;
        if (errMsg != null)
        {
            result = new CreateCmdResult(null, new Answer(null, false, errMsg));
            result.setResult(errMsg);
        } else
        {
            // notify guests
            result = notifyResize(data, oldSize, resizeParameter);
        }

        callback.complete(result);
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(
        VolumeInfo volumeInfo,
        QualityOfServiceState qualityOfServiceState)
    {
        s_logger.debug("Linstor: handleQualityOfServiceForVolumeMigration");
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshotInfo, AsyncCompletionCallback<CreateCmdResult> callback)
    {
        s_logger.debug("Linstor: takeSnapshot with snapshot: " + snapshotInfo.getUuid());

        final VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
        final VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        final StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);
        final DevelopersApi api = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());
        final String rscName = LinstorUtil.RSC_PREFIX + volumeVO.getPath();

        Snapshot snapshot = new Snapshot();
        snapshot.setName(getSnapshotName(snapshotInfo.getUuid()));

        CreateCmdResult result;
        try
        {
            ApiCallRcList answers = api.resourceSnapshotCreate(rscName, snapshot);

            if (answers.hasError())
            {
                final String errMsg = answers.get(0).getMessage();
                s_logger.error("Snapshot error: " + errMsg);
                result = new CreateCmdResult(null, new Answer(null, false, errMsg));
                result.setResult(errMsg);
            } else
            {
                s_logger.info(String.format("Successfully took snapshot from %s", rscName));

                SnapshotObjectTO snapshotObjectTo = (SnapshotObjectTO)snapshotInfo.getTO();
                snapshotObjectTo.setPath(rscName + "-" + snapshotInfo.getName());

                result = new CreateCmdResult(null, new CreateObjectAnswer(snapshotObjectTo));
                result.setResult(null);
            }
        } catch (ApiException apiExc)
        {
            s_logger.error(apiExc);
            result = new CreateCmdResult(null, new Answer(null, false, apiExc.getBestMessage()));
            result.setResult(apiExc.getBestMessage());
        }

        callback.complete(result);
    }

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
}
