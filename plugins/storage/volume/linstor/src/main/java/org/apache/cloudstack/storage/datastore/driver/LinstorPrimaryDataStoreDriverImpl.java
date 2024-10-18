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
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionCloneRequest;
import com.linbit.linstor.api.model.ResourceDefinitionCloneStarted;
import com.linbit.linstor.api.model.ResourceDefinitionCreate;
import com.linbit.linstor.api.model.ResourceGroupSpawn;
import com.linbit.linstor.api.model.ResourceMakeAvailable;
import com.linbit.linstor.api.model.Snapshot;
import com.linbit.linstor.api.model.SnapshotRestore;
import com.linbit.linstor.api.model.VolumeDefinition;
import com.linbit.linstor.api.model.VolumeDefinitionModify;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.api.storage.LinstorBackupSnapshotCommand;
import com.cloud.api.storage.LinstorRevertBackupSnapshotCommand;
import com.cloud.configuration.Config;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;
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
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorConfigurationManager;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.snapshot.SnapshotObject;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LinstorPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    protected Logger logger = LogManager.getLogger(getClass());
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDetailsDao _volumeDetailsDao;
    @Inject private VMTemplatePoolDao _vmTemplatePoolDao;
    @Inject private SnapshotDao _snapshotDao;
    @Inject private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject private StorageManager _storageMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    private HostDao _hostDao;

    public LinstorPrimaryDataStoreDriverImpl()
    {
    }

    @Override
    public Map<String, String> getCapabilities()
    {
        Map<String, String> mapCapabilities = new HashMap<>();

        // Linstor will be restricted to only run on LVM-THIN and ZFS storage pools with ACS
        // This enables template caching on our primary storage
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString());

        // fetch if lvm-thin or ZFS
        boolean system_snapshot = !LinstorConfigurationManager.BackupSnapshots.value();
        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.toString(system_snapshot));

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
                    logger.error(answer.getMessage());
                }
                throw new CloudRuntimeException("Linstor: Unable to delete resource definition: " + rscDefName);
            }
            logger.info(String.format("Linstor: Deleted resource %s", rscDefName));
        } catch (ApiException apiEx)
        {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private void deleteSnapshot(@Nonnull DataStore dataStore, @Nonnull String rscDefName, @Nonnull String snapshotName)
    {
        StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePool.getHostAddress());

        try
        {
            ApiCallRcList answers = linstorApi.resourceSnapshotDelete(rscDefName, snapshotName, Collections.emptyList());
            if (answers.hasError())
            {
                for (ApiCallRc answer : answers)
                {
                    logger.error(answer.getMessage());
                }
                throw new CloudRuntimeException("Linstor: Unable to delete snapshot: " + rscDefName);
            }
            logger.info("Linstor: Deleted snapshot " + snapshotName + " for resource " + rscDefName);
        } catch (ApiException apiEx)
        {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
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
        logger.debug("deleteAsync: " + dataObject.getType() + ";" + dataObject.getUuid());
        String errMsg = null;

        final long storagePoolId = dataStore.getId();
        final StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        switch (dataObject.getType()) {
            case VOLUME:
            {
                final VolumeInfo volumeInfo = (VolumeInfo) dataObject;
                // if volume creation wasn't completely done .setPath wasn't called, so we fallback to vol.getUuid()
                final String volUuid = volumeInfo.getPath() != null ? volumeInfo.getPath() : volumeInfo.getUuid();
                final String rscName = LinstorUtil.RSC_PREFIX + volUuid;
                deleteResourceDefinition(storagePool, rscName);

                long usedBytes = storagePool.getUsedBytes();
                Long capacityIops = storagePool.getCapacityIops();

                if (capacityIops != null)
                {
                    if (volumeInfo.getMaxIops() != null)
                        capacityIops += volumeInfo.getMaxIops();
                    storagePool.setCapacityIops(Math.max(0, capacityIops));
                }

                usedBytes -= volumeInfo.getSize();
                storagePool.setUsedBytes(Math.max(0, usedBytes));

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
                logger.error(errMsg);
        }

        if (callback != null) {
            CommandResult result = new CommandResult();
            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void logLinstorAnswer(@Nonnull ApiCallRc answer) {
        if (answer.isError()) {
            logger.error(answer.getMessage());
        } else if (answer.isWarning()) {
            logger.warn(answer.getMessage());
        } else if (answer.isInfo()) {
            logger.info(answer.getMessage());
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

    private void applyQoSSettings(StoragePoolVO storagePool, DevelopersApi api, String rscName, Long maxIops)
        throws ApiException
    {
        Long currentQosIops = null;
        List<VolumeDefinition> vlmDfns = api.volumeDefinitionList(rscName, null, null);
        if (!vlmDfns.isEmpty())
        {
            Properties props = vlmDfns.get(0).getProps();
            long iops = Long.parseLong(props.getOrDefault("sys/fs/blkio_throttle_write_iops", "0"));
            currentQosIops = iops > 0 ? iops : null;
        }

        if (!Objects.equals(maxIops, currentQosIops))
        {
            VolumeDefinitionModify vdm = new VolumeDefinitionModify();
            if (maxIops != null)
            {
                Properties props = new Properties();
                props.put("sys/fs/blkio_throttle_read_iops", "" + maxIops);
                props.put("sys/fs/blkio_throttle_write_iops", "" + maxIops);
                vdm.overrideProps(props);
                logger.info("Apply qos setting: " + maxIops + " to " + rscName);
            }
            else
            {
                logger.info("Remove QoS setting for " + rscName);
                vdm.deleteProps(Arrays.asList("sys/fs/blkio_throttle_read_iops", "sys/fs/blkio_throttle_write_iops"));
            }
            ApiCallRcList answers = api.volumeDefinitionModify(rscName, 0, vdm);
            checkLinstorAnswersThrow(answers);

            Long capacityIops = storagePool.getCapacityIops();
            if (capacityIops != null)
            {
                long vcIops = currentQosIops != null ? currentQosIops * -1 : 0;
                long vMaxIops = maxIops != null ? maxIops : 0;
                long newIops = vcIops + vMaxIops;
                capacityIops -= newIops;
                logger.info("Current storagepool " + storagePool.getName() + " iops capacity:  " + capacityIops);
                storagePool.setCapacityIops(Math.max(0, capacityIops));
                _storagePoolDao.update(storagePool.getId(), storagePool);
            }
        }
    }

    private String getRscGrp(StoragePoolVO storagePoolVO) {
        return storagePoolVO.getUserInfo() != null && !storagePoolVO.getUserInfo().isEmpty() ?
            storagePoolVO.getUserInfo() : "DfltRscGrp";
    }

    private String createResourceBase(
        String rscName, long sizeInBytes, String volName, String vmName, DevelopersApi api, String rscGrp) {
        ResourceGroupSpawn rscGrpSpawn = new ResourceGroupSpawn();
        rscGrpSpawn.setResourceDefinitionName(rscName);
        rscGrpSpawn.addVolumeSizesItem(sizeInBytes / 1024);

        try
        {
            logger.info("Linstor: Spawn resource " + rscName);
            ApiCallRcList answers = api.resourceGroupSpawn(rscGrp, rscGrpSpawn);
            checkLinstorAnswersThrow(answers);

            answers = LinstorUtil.applyAuxProps(api, rscName, volName, vmName);
            checkLinstorAnswersThrow(answers);

            return LinstorUtil.getDevicePath(api, rscName);
        } catch (ApiException apiEx)
        {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private String createResource(VolumeInfo vol, StoragePoolVO storagePoolVO) {
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());
        final String rscGrp = getRscGrp(storagePoolVO);

        final String rscName = LinstorUtil.RSC_PREFIX + vol.getUuid();
        String deviceName = createResourceBase(
            rscName, vol.getSize(), vol.getName(), vol.getAttachedVmName(), linstorApi, rscGrp);

        try
        {
            applyQoSSettings(storagePoolVO, linstorApi, rscName, vol.getMaxIops());

            return LinstorUtil.getDevicePath(linstorApi, rscName);
        } catch (ApiException apiEx)
        {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private void resizeResource(DevelopersApi api, String resourceName, long sizeByte) throws ApiException {
        VolumeDefinitionModify dfm = new VolumeDefinitionModify();
        dfm.setSizeKib(sizeByte / 1024);

        ApiCallRcList answers = api.volumeDefinitionModify(resourceName, 0, dfm);
        if (answers.hasError()) {
            logger.error("Resize error: " + answers.get(0).getMessage());
            throw new CloudRuntimeException(answers.get(0).getMessage());
        } else {
            logger.info(String.format("Successfully resized %s to %d kib", resourceName, dfm.getSizeKib()));
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
                logger.info("Clone resource definition " + cloneRes + " to " + rscName);
                ResourceDefinitionCloneRequest cloneRequest = new ResourceDefinitionCloneRequest();
                cloneRequest.setName(rscName);
                ResourceDefinitionCloneStarted cloneStarted = linstorApi.resourceDefinitionClone(
                    cloneRes, cloneRequest);

                checkLinstorAnswersThrow(cloneStarted.getMessages());

                if (!CloneWaiter.waitFor(linstorApi, cloneStarted)) {
                    throw new CloudRuntimeException("Clone for resource " + rscName + " failed.");
                }

                logger.info("Clone resource definition " + cloneRes + " to " + rscName + " finished");

                if (volumeInfo.getSize() != null && volumeInfo.getSize() > 0) {
                    resizeResource(linstorApi, rscName, volumeInfo.getSize());
                }

                LinstorUtil.applyAuxProps(linstorApi, rscName, volumeInfo.getName(), volumeInfo.getAttachedVmName());
                applyQoSSettings(storagePoolVO, linstorApi, rscName, volumeInfo.getMaxIops());

                return LinstorUtil.getDevicePath(linstorApi, rscName);
            } catch (ApiException apiEx) {
                logger.error("Linstor: ApiEx - " + apiEx.getMessage());
                throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
            }
        } else {
            throw new CloudRuntimeException(
                "Unable to find Linstor resource for the following template data-object ID: " + csCloneId);
        }
    }

    private ResourceDefinitionCreate createResourceDefinitionCreate(String rscName, String rscGrpName)
            throws ApiException {
        ResourceDefinitionCreate rdCreate = new ResourceDefinitionCreate();
        ResourceDefinition rd = new ResourceDefinition();
        rd.setName(rscName);
        rd.setResourceGroupName(rscGrpName);
        rdCreate.setResourceDefinition(rd);
        return rdCreate;
    }

    private String createResourceFromSnapshot(long csSnapshotId, String rscName, StoragePoolVO storagePoolVO) {
        final String rscGrp = getRscGrp(storagePoolVO);
        final DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());

        SnapshotVO snapshotVO = _snapshotDao.findById(csSnapshotId);
        String snapName = LinstorUtil.RSC_PREFIX + snapshotVO.getUuid();
        VolumeVO volumeVO = _volumeDao.findById(snapshotVO.getVolumeId());
        String cloneRes = LinstorUtil.RSC_PREFIX + volumeVO.getPath();

        try
        {
            logger.debug("Create new resource definition: " + rscName);
            ResourceDefinitionCreate rdCreate = createResourceDefinitionCreate(rscName, rscGrp);
            ApiCallRcList answers = linstorApi.resourceDefinitionCreate(rdCreate);
            checkLinstorAnswersThrow(answers);

            SnapshotRestore snapshotRestore = new SnapshotRestore();
            snapshotRestore.toResource(rscName);

            logger.debug("Create new volume definition for snapshot: " + cloneRes + ":" + snapName);
            answers = linstorApi.resourceSnapshotsRestoreVolumeDefinition(cloneRes, snapName, snapshotRestore);
            checkLinstorAnswersThrow(answers);

            // restore snapshot to new resource
            logger.info("Restore resource from snapshot: " + cloneRes + ":" + snapName);
            answers = linstorApi.resourceSnapshotRestore(cloneRes, snapName, snapshotRestore);
            checkLinstorAnswersThrow(answers);

            LinstorUtil.applyAuxProps(linstorApi, rscName, volumeVO.getName(), null);
            applyQoSSettings(storagePoolVO, linstorApi, rscName, volumeVO.getMaxIops());

            return LinstorUtil.getDevicePath(linstorApi, rscName);
        } catch (ApiException apiEx) {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
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

            logger.debug("Temp resource created: " + tempRscName);
            addTempVolumeToDb(csSnapshotId, csName);
        }
        else if (snapshotDetails != null && snapshotDetails.getValue() != null &&
            snapshotDetails.getValue().equalsIgnoreCase("delete"))
        {
            snapshotDetails = _snapshotDetailsDao.findDetail(csSnapshotId, LinstorUtil.TEMP_VOLUME_ID);

            deleteResourceDefinition(storagePoolVO, snapshotDetails.getValue());

            logger.debug("Temp resource deleted: " + snapshotDetails.getValue());
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
                    logger.debug("createAsync - creating volume");
                    devPath = createVolume(volumeInfo, storagePool);
                    volume.setFolder("/dev/");
                    volume.setPoolId(storagePool.getId());
                    volume.setUuid(vol.getUuid());
                    volume.setPath(vol.getUuid());

                    _volumeDao.update(volume.getId(), volume);
                    break;
                case SNAPSHOT:
                    logger.debug("createAsync - SNAPSHOT");
                    createVolumeFromSnapshot((SnapshotInfo) vol, storagePool);
                    break;
                case TEMPLATE:
                    errMsg = "creating template - not supported";
                    logger.error("createAsync - " + errMsg);
                    break;
                default:
                    errMsg = "Invalid DataObjectType (" + vol.getType() + ") passed to createAsync";
                    logger.error(errMsg);
            }
        } catch (Exception ex)
        {
            errMsg = ex.getMessage();

            logger.error("createAsync: " + errMsg);
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

    private String revertSnapshotFromImageStore(
        final SnapshotInfo snapshot,
        final VolumeInfo volumeInfo,
        final DevelopersApi linstorApi,
        final String rscName)
    throws ApiException {
        String resultMsg = null;
        String value = _configDao.getValue(Config.BackupSnapshotWait.toString());
        int _backupsnapshotwait = NumbersUtil.parseInt(
            value, Integer.parseInt(Config.BackupSnapshotWait.getDefaultValue()));

        LinstorRevertBackupSnapshotCommand cmd = new LinstorRevertBackupSnapshotCommand(
            snapshot.getTO(),
            volumeInfo.getTO(),
            _backupsnapshotwait,
            VirtualMachineManager.ExecuteInSequence.value());

        Optional<RemoteHostEndPoint> optEP = getDiskfullEP(linstorApi, rscName);
        if (optEP.isEmpty()) {
            optEP = getLinstorEP(linstorApi, rscName);
        }

        if (optEP.isPresent()) {
            Answer answer = optEP.get().sendMessage(cmd);
            if (!answer.getResult()) {
                resultMsg = answer.getDetails();
            }
        } else {
            resultMsg = "Unable to get matching Linstor endpoint.";
        }
        return resultMsg;
    }

    private String doRevertSnapshot(final SnapshotInfo snapshot, final VolumeInfo volumeInfo) {
        final StoragePool pool = (StoragePool) volumeInfo.getDataStore();
        final DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        final String rscName = LinstorUtil.RSC_PREFIX + volumeInfo.getPath();
        String resultMsg;
        try {
            if (snapshot.getDataStore().getRole() == DataStoreRole.Primary) {
                final String snapName = LinstorUtil.RSC_PREFIX + snapshot.getUuid();

                ApiCallRcList answers = linstorApi.resourceSnapshotRollback(rscName, snapName);
                resultMsg = checkLinstorAnswers(answers);
            } else if (snapshot.getDataStore().getRole() == DataStoreRole.Image) {
                resultMsg = revertSnapshotFromImageStore(snapshot, volumeInfo, linstorApi, rscName);
            } else {
                resultMsg = "Linstor: Snapshot revert datastore not supported";
            }
        } catch (ApiException apiEx) {
            logger.error("Linstor: ApiEx - " + apiEx.getMessage());
            resultMsg = apiEx.getBestMessage();
        }

        return resultMsg;
    }

    @Override
    public void revertSnapshot(
        SnapshotInfo snapshot,
        SnapshotInfo snapshotOnPrimaryStore,
        AsyncCompletionCallback<CommandResult> callback)
    {
        logger.debug("Linstor: revertSnapshot");
        final VolumeInfo volumeInfo = snapshot.getBaseVolume();
        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());
        if (volumeVO == null || volumeVO.getRemoved() != null) {
            CommandResult commandResult = new CommandResult();
            commandResult.setResult("The volume that the snapshot belongs to no longer exists.");
            callback.complete(commandResult);
            return;
        }

        String resultMsg = doRevertSnapshot(snapshot, volumeInfo);

        if (callback != null)
        {
            CommandResult result = new CommandResult();
            result.setResult(resultMsg);
            callback.complete(result);
        }
    }

    private static boolean canCopySnapshotCond(DataObject srcData, DataObject dstData) {
        return srcData.getType() == DataObjectType.SNAPSHOT && dstData.getType() == DataObjectType.SNAPSHOT
            && (dstData.getDataStore().getRole() == DataStoreRole.Image
            || dstData.getDataStore().getRole() == DataStoreRole.ImageCache);
    }

    private static boolean canCopyTemplateCond(DataObject srcData, DataObject dstData) {
        return srcData.getType() == DataObjectType.TEMPLATE && dstData.getType() == DataObjectType.TEMPLATE
            && dstData.getDataStore().getRole() == DataStoreRole.Primary
            && (srcData.getDataStore().getRole() == DataStoreRole.Image
            || srcData.getDataStore().getRole() == DataStoreRole.ImageCache);
    }

    private static boolean canCopyVolumeCond(DataObject srcData, DataObject dstData) {
        // Volume download from Linstor primary storage
        return srcData.getType() == DataObjectType.VOLUME
                && (dstData.getType() == DataObjectType.VOLUME || dstData.getType() == DataObjectType.TEMPLATE)
                && srcData.getDataStore().getRole() == DataStoreRole.Primary
                && (dstData.getDataStore().getRole() == DataStoreRole.Image
                || dstData.getDataStore().getRole() == DataStoreRole.ImageCache);
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject dstData)
    {
        logger.debug("LinstorPrimaryDataStoreDriverImpl.canCopy: " + srcData.getType() + " -> " + dstData.getType());

        if (canCopySnapshotCond(srcData, dstData)) {
            SnapshotInfo sinfo = (SnapshotInfo) srcData;
            VolumeInfo volume = sinfo.getBaseVolume();
            StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());
            return storagePool.getStorageProviderName().equals(LinstorUtil.PROVIDER_NAME);
        } else if (canCopyTemplateCond(srcData, dstData)) {
            TemplateInfo tInfo = (TemplateInfo) dstData;
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(dstData.getDataStore().getId());
            return storagePoolVO != null
                && storagePoolVO.getPoolType() == Storage.StoragePoolType.Linstor
                && tInfo.getSize() != null;
        } else if (canCopyVolumeCond(srcData, dstData)) {
            VolumeInfo srcVolInfo = (VolumeInfo) srcData;
            StoragePoolVO storagePool = _storagePoolDao.findById(srcVolInfo.getPoolId());
            return storagePool.getStorageProviderName().equals(LinstorUtil.PROVIDER_NAME);
        }
        return false;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject dstData, AsyncCompletionCallback<CopyCommandResult> callback)
    {
        logger.debug("LinstorPrimaryDataStoreDriverImpl.copyAsync: "
            + srcData.getType() + " -> " + dstData.getType());

        final CopyCommandResult res;
        if (canCopySnapshotCond(srcData, dstData)) {
            String errMsg = null;
            Answer answer = copySnapshot(srcData, dstData);
            if (answer != null && !answer.getResult()) {
                errMsg = answer.getDetails();
            } else {
                // delete primary storage snapshot
                SnapshotInfo sinfo = (SnapshotInfo) srcData;
                VolumeInfo volume = sinfo.getBaseVolume();
                deleteSnapshot(
                    srcData.getDataStore(),
                    LinstorUtil.RSC_PREFIX + volume.getPath(),
                    LinstorUtil.RSC_PREFIX + sinfo.getUuid());
            }
            res = new CopyCommandResult(null, answer);
            res.setResult(errMsg);
        } else if (canCopyTemplateCond(srcData, dstData)) {
            Answer answer = copyTemplate(srcData, dstData);
            res = new CopyCommandResult(null, answer);
        } else if (canCopyVolumeCond(srcData, dstData)) {
            Answer answer = copyVolume(srcData, dstData);
            res = new CopyCommandResult(null, answer);
        } else {
            Answer answer = new Answer(null, false, "noimpl");
            res = new CopyCommandResult(null, answer);
            res.setResult("Not implemented yet");
        }
        callback.complete(res);
    }

    /**
     * Tries to get a Linstor cloudstack end point, that is at least diskless.
     *
     * @param api Linstor java api object
     * @param rscName resource name to make available on node
     * @return Optional RemoteHostEndPoint if one could get found.
     * @throws ApiException
     */
    private Optional<RemoteHostEndPoint> getLinstorEP(DevelopersApi api, String rscName) throws ApiException {
        List<String> linstorNodeNames = LinstorUtil.getLinstorNodeNames(api);
        Collections.shuffle(linstorNodeNames);  // do not always pick the first linstor node

        Host host = null;
        for (String nodeName : linstorNodeNames) {
            host = _hostDao.findByName(nodeName);
            if (host != null && host.getResourceState() == ResourceState.Enabled) {
                logger.info(String.format("Linstor: Make resource %s available on node %s ...", rscName, nodeName));
                ApiCallRcList answers = api.resourceMakeAvailableOnNode(rscName, nodeName, new ResourceMakeAvailable());
                if (!answers.hasError()) {
                    break; // found working host
                } else {
                    logger.error(
                        String.format("Linstor: Unable to make resource %s on node %s available: %s",
                            rscName,
                            nodeName,
                            LinstorUtil.getBestErrorMessage(answers)));
                }
            }
        }

        if (host == null)
        {
            logger.error("Linstor: Couldn't create a resource on any cloudstack host.");
            return Optional.empty();
        }
        else
        {
            return Optional.of(RemoteHostEndPoint.getHypervisorHostEndPoint(host));
        }
    }

    private Optional<RemoteHostEndPoint> getDiskfullEP(DevelopersApi api, String rscName) throws ApiException {
        List<com.linbit.linstor.api.model.StoragePool> linSPs = LinstorUtil.getDiskfulStoragePools(api, rscName);
        if (linSPs != null) {
            for (com.linbit.linstor.api.model.StoragePool sp : linSPs) {
                Host host = _hostDao.findByName(sp.getNodeName());
                if (host != null && host.getResourceState() == ResourceState.Enabled) {
                    return Optional.of(RemoteHostEndPoint.getHypervisorHostEndPoint(host));
                }
            }
        }
        logger.error("Linstor: No diskfull host found.");
        return Optional.empty();
    }

    private String restoreResourceFromSnapshot(
            DevelopersApi api,
            StoragePoolVO storagePoolVO,
            String rscName,
            String snapshotName,
            String restoredName) throws ApiException {
        final String rscGrp = getRscGrp(storagePoolVO);
        ResourceDefinitionCreate rdc = createResourceDefinitionCreate(restoredName, rscGrp);
        api.resourceDefinitionCreate(rdc);

        SnapshotRestore sr = new SnapshotRestore();
        sr.toResource(restoredName);
        api.resourceSnapshotsRestoreVolumeDefinition(rscName, snapshotName, sr);

        api.resourceSnapshotRestore(rscName, snapshotName, sr);

        return LinstorUtil.getDevicePath(api, restoredName);
    }

    private Answer copyTemplate(DataObject srcData, DataObject dstData) {
        TemplateInfo tInfo = (TemplateInfo) dstData;
        final StoragePoolVO pool = _storagePoolDao.findById(dstData.getDataStore().getId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        final String rscName = LinstorUtil.RSC_PREFIX + dstData.getUuid();
        createResourceBase(
            LinstorUtil.RSC_PREFIX + dstData.getUuid(),
            tInfo.getSize(),
            tInfo.getName(),
            "",
            api,
            getRscGrp(pool));

        int nMaxExecutionMinutes = NumbersUtil.parseInt(
            _configDao.getValue(Config.SecStorageCmdExecutionTimeMax.key()), 30);
        CopyCommand cmd = new CopyCommand(
            srcData.getTO(),
            dstData.getTO(),
            nMaxExecutionMinutes * 60 * 1000,
            VirtualMachineManager.ExecuteInSequence.value());
        Answer answer;

        try {
            Optional<RemoteHostEndPoint> optEP = getLinstorEP(api, rscName);
            if (optEP.isPresent()) {
                answer = optEP.get().sendMessage(cmd);
            }
            else {
                answer = new Answer(cmd, false, "Unable to get matching Linstor endpoint.");
                deleteResourceDefinition(pool, rscName);
            }
        } catch (ApiException exc) {
            logger.error("copy template failed: ", exc);
            deleteResourceDefinition(pool, rscName);
            throw new CloudRuntimeException(exc.getBestMessage());
        }
        return answer;
    }

    private Answer copyVolume(DataObject srcData, DataObject dstData) {
        VolumeInfo srcVolInfo = (VolumeInfo) srcData;
        final StoragePoolVO pool = _storagePoolDao.findById(srcVolInfo.getDataStore().getId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        final String rscName = LinstorUtil.RSC_PREFIX + srcVolInfo.getPath();

        VolumeObjectTO to = (VolumeObjectTO) srcVolInfo.getTO();
        // patch source format
        // Linstor volumes are stored as RAW, but we can't set the correct format as RAW (we use QCOW2)
        // otherwise create template from snapshot won't work, because this operation
        // uses the format of the base volume and we backup snapshots as QCOW2
        // https://github.com/apache/cloudstack/pull/8802#issuecomment-2024019927
        to.setFormat(Storage.ImageFormat.RAW);
        int nMaxExecutionSeconds = NumbersUtil.parseInt(
                _configDao.getValue(Config.CopyVolumeWait.key()), 10800);
        CopyCommand cmd = new CopyCommand(
                to,
                dstData.getTO(),
                nMaxExecutionSeconds,
                VirtualMachineManager.ExecuteInSequence.value());
        Answer answer;

        try {
            Optional<RemoteHostEndPoint> optEP = getLinstorEP(api, rscName);
            if (optEP.isPresent()) {
                answer = optEP.get().sendMessage(cmd);
            }
            else {
                answer = new Answer(cmd, false, "Unable to get matching Linstor endpoint.");
            }
        } catch (ApiException exc) {
            logger.error("copy volume failed: ", exc);
            throw new CloudRuntimeException(exc.getBestMessage());
        }
        return answer;
    }

    /**
     * Create a temporary resource from the snapshot to backup, so we can copy the data on a diskless agent
     * @param api Linstor Developer api object
     * @param pool StoragePool this resource resides on
     * @param rscName rscName of the snapshotted resource
     * @param snapshotName Name of the snapshot to copy from
     * @param snapshotObject snapshot object of the origCmd, so the path can be modified
     * @param origCmd original LinstorBackupSnapshotCommand that needs to have a patched path
     * @return answer from agent operation
     * @throws ApiException if any Linstor api operation fails
     */
    private Answer copyFromTemporaryResource(
            DevelopersApi api,
            StoragePoolVO pool,
            String rscName,
            String snapshotName,
            SnapshotObject snapshotObject,
            CopyCommand origCmd)
            throws ApiException {
        Answer answer;
        String restoreName = rscName + "-rst";
        String devName = restoreResourceFromSnapshot(api, pool, rscName, snapshotName, restoreName);

        Optional<RemoteHostEndPoint> optEPAny = getLinstorEP(api, restoreName);
        if (optEPAny.isPresent()) {
            // patch the src device path to the temporary linstor resource
            snapshotObject.setPath(devName);
            origCmd.setSrcTO(snapshotObject.getTO());
            answer = optEPAny.get().sendMessage(origCmd);
        } else{
            answer = new Answer(origCmd, false, "Unable to get matching Linstor endpoint.");
        }
        // delete the temporary resource, noop if already gone
        api.resourceDefinitionDelete(restoreName);
        return answer;
    }

    /**
     * vmsnapshots don't have our typical snapshot path set
     * instead the path is the internal snapshot name e.g.: {vm}_VS_{datestr}
     * we have to find out and modify the path here before
     * @return the original snapshotObject.getPath()
     */
    private String setCorrectSnapshotPath(DevelopersApi api, String rscName, SnapshotObject snapshotObject)
            throws ApiException {
        String originalPath = LinstorUtil.RSC_PREFIX + snapshotObject.getUuid();
        if (!(snapshotObject.getPath().startsWith("/dev/mapper/") ||
                snapshotObject.getPath().startsWith("zfs://"))) {
            originalPath = snapshotObject.getPath();
            com.linbit.linstor.api.model.StoragePool linStoragePool =
                    LinstorUtil.getDiskfulStoragePool(api, rscName);
            if (linStoragePool == null) {
                throw new CloudRuntimeException("Linstor: Unable to find storage pool for resource " + rscName);
            }
            final String path = LinstorUtil.getSnapshotPath(linStoragePool, rscName, snapshotObject.getPath());
            snapshotObject.setPath(path);
        }
        return originalPath;
    }

    protected Answer copySnapshot(DataObject srcData, DataObject destData) {
        String value = _configDao.getValue(Config.BackupSnapshotWait.toString());
        int _backupsnapshotwait = NumbersUtil.parseInt(
            value, Integer.parseInt(Config.BackupSnapshotWait.getDefaultValue()));

        SnapshotObject snapshotObject = (SnapshotObject)srcData;
        Boolean snapshotFullBackup = snapshotObject.getFullBackup();
        final StoragePoolVO pool = _storagePoolDao.findById(srcData.getDataStore().getId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        boolean fullSnapshot = true;
        if (snapshotFullBackup != null) {
            fullSnapshot = snapshotFullBackup;
        }
        Map<String, String> options = new HashMap<>();
        options.put("fullSnapshot", fullSnapshot + "");
        options.put(SnapshotInfo.BackupSnapshotAfterTakingSnapshot.key(),
            String.valueOf(SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value()));
        options.put("volumeSize", snapshotObject.getBaseVolume().getSize() + "");

        try {
            final String rscName = LinstorUtil.RSC_PREFIX + snapshotObject.getBaseVolume().getPath();
            String snapshotName = setCorrectSnapshotPath(api, rscName, snapshotObject);

            CopyCommand cmd = new LinstorBackupSnapshotCommand(
                snapshotObject.getTO(),
                destData.getTO(),
                _backupsnapshotwait,
                VirtualMachineManager.ExecuteInSequence.value());
            cmd.setOptions(options);

            Optional<RemoteHostEndPoint> optEP = getDiskfullEP(api, rscName);
            Answer answer;
            if (optEP.isPresent()) {
                answer = optEP.get().sendMessage(cmd);
            } else {
                logger.debug("No diskfull endpoint found to copy image, creating diskless endpoint");
                answer = copyFromTemporaryResource(api, pool, rscName, snapshotName, snapshotObject, cmd);
            }
            return answer;
        } catch (Exception e) {
            logger.debug("copy snapshot failed, please cleanup snapshot manually: ", e);
            throw new CloudRuntimeException(e.toString());
        }

    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback)
    {
        // as long as canCopy is false, this isn't called
        logger.debug("Linstor: copyAsync with host");
        copyAsync(srcData, destData, callback);
    }

    private CreateCmdResult notifyResize(
        VolumeObject vol,
        long oldSize,
        ResizeVolumePayload resizeParameter)
    {
        StoragePool pool = (StoragePool) vol.getDataStore();

        ResizeVolumeCommand resizeCmd =
            new ResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), oldSize, resizeParameter.newSize, resizeParameter.shrinkOk,
                resizeParameter.instanceName, null);
        CreateCmdResult result = new CreateCmdResult(null, null);
        try {
            ResizeVolumeAnswer answer = (ResizeVolumeAnswer) _storageMgr.sendToPool(pool, resizeParameter.hosts, resizeCmd);
            if (answer != null && answer.getResult()) {
                logger.debug("Resize: notified hosts");
            } else if (answer != null) {
                result.setResult(answer.getDetails());
            } else {
                logger.debug("return a null answer, mark it as failed for unknown reason");
                result.setResult("return a null answer, mark it as failed for unknown reason");
            }

        } catch (Exception e) {
            logger.debug("sending resize command failed", e);
            result.setResult(e.toString());
        }

        return result;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback)
    {
        final VolumeObject vol = (VolumeObject) data;
        final StoragePoolVO pool = _storagePoolDao.findById(data.getDataStore().getId());
        final DevelopersApi api = LinstorUtil.getLinstorAPI(pool.getHostAddress());
        final ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();

        final String rscName = LinstorUtil.RSC_PREFIX + vol.getPath();
        final long oldSize = vol.getSize();

        String errMsg = null;
        VolumeDefinitionModify dfm = new VolumeDefinitionModify();
        dfm.setSizeKib(resizeParameter.newSize / 1024);
        try
        {
            resizeResource(api, rscName, resizeParameter.newSize);

            applyQoSSettings(pool, api, rscName, resizeParameter.newMaxIops);
            {
                final VolumeVO volume = _volumeDao.findById(vol.getId());
                volume.setMinIops(resizeParameter.newMinIops);
                volume.setMaxIops(resizeParameter.newMaxIops);
                volume.setSize(resizeParameter.newSize);
                _volumeDao.update(volume.getId(), volume);
            }
        } catch (ApiException apiExc)
        {
            logger.error(apiExc);
            errMsg = apiExc.getBestMessage();
        }

        CreateCmdResult result;
        if (errMsg != null) {
            result = new CreateCmdResult(null, new Answer(null, false, errMsg));
            result.setResult(errMsg);
        } else {
            // notify guests
            result = notifyResize(vol, oldSize, resizeParameter);
        }

        callback.complete(result);
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(
        VolumeInfo volumeInfo,
        QualityOfServiceState qualityOfServiceState)
    {
        logger.debug("Linstor: handleQualityOfServiceForVolumeMigration");
    }

    private Answer createAnswerAndPerstistDetails(DevelopersApi api, SnapshotInfo snapshotInfo, String rscName)
        throws ApiException {
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)snapshotInfo.getTO();
        com.linbit.linstor.api.model.StoragePool linStoragePool = LinstorUtil.getDiskfulStoragePool(api, rscName);
        if (linStoragePool == null) {
            throw new CloudRuntimeException("Linstor: Unable to find storage pool for resource " + rscName);
        }

        final String path = LinstorUtil.getSnapshotPath(linStoragePool, rscName, LinstorUtil.RSC_PREFIX + snapshotInfo.getUuid());
        snapshotTO.setPath(path);
        SnapshotDetailsVO details = new SnapshotDetailsVO(
            snapshotInfo.getId(), snapshotInfo.getUuid(), path, false);
        _snapshotDetailsDao.persist(details);

        return new CreateObjectAnswer(snapshotTO);
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshotInfo, AsyncCompletionCallback<CreateCmdResult> callback)
    {
        logger.debug("Linstor: takeSnapshot with snapshot: " + snapshotInfo.getUuid());

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
                logger.error("Snapshot error: " + errMsg);
                result = new CreateCmdResult(null, new Answer(null, false, errMsg));
                result.setResult(errMsg);
            } else
            {
                logger.info(String.format("Successfully took snapshot %s from %s", snapshot.getName(), rscName));

                Answer answer = createAnswerAndPerstistDetails(api, snapshotInfo, rscName);

                result = new CreateCmdResult(null, answer);
                result.setResult(null);
            }
        } catch (ApiException apiExc)
        {
            logger.error(apiExc);
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

    @Override
    public boolean isStorageSupportHA(StoragePoolType type) {
        return true;
    }

    @Override
    public void detachVolumeFromAllStorageNodes(Volume volume) {
    }
}
