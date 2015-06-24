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

package org.apache.cloudstack.storage.datastore.driver;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.FileSystem;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * The implementation class for <code>ElastistorPrimaryDataStoreDriver</code>.
 * This directs the public interface methods to use CloudByte's Elastistor based
 * volumes.
 */
public class ElastistorPrimaryDataStoreDriver extends CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {

    private static final Logger s_logger = Logger.getLogger(ElastistorPrimaryDataStoreDriver.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    StorageManager storageMgr;
    @Inject
    VolumeDetailsDao _volumeDetailsDao;

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {

        String iqn = null;
        String errMsg = null;

        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        if (dataObject.getType() == DataObjectType.VOLUME) {

            VolumeInfo volumeInfo = (VolumeInfo) dataObject;

            long storagePoolId = dataStore.getId();
            String volumeName = volumeInfo.getName();
            Long Iops = volumeInfo.getMaxIops();
            // quota size of the cloudbyte volume will be increased with the given HypervisorSnapshotReserve
            Long quotaSize = getVolumeSizeIncludingHypervisorSnapshotReserve(volumeInfo, _storagePoolDao.findById(storagePoolId));

            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());
            VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

            // if the primary storage is not managed (thick provisioned)
            // then no need to create volume at elastistor,
            // calling super(default) that creates a vdi(disk) only.
            if (!(storagePool.isManaged())) {
                super.createAsync(dataStore, dataObject, callback);

                // update the volume property
                volume.setPoolType(storagePool.getPoolType());
                _volumeDao.update(volume.getId(), volume);

                return;
            }

            DiskOfferingVO diskOffering = _diskOfferingDao.findById(volumeInfo.getDiskOfferingId());

            long capacityIops = storagePool.getCapacityIops();
            capacityIops = capacityIops - Iops;

            if (capacityIops < 0) {
                throw new CloudRuntimeException("IOPS not available. [pool:" + storagePool.getName() + "] [availiops:" + capacityIops + "] [requirediops:" + Iops + "]");
            }

            String protocoltype = null;
            StoragePoolVO dataStoreVO = _storagePoolDao.findById(storagePoolId);
            String desc = diskOffering.getDisplayText();

            if (desc.toLowerCase().contains("iscsi")) {
                protocoltype = "iscsi";
            } else if (dataStoreVO.getPoolType().equals(StoragePoolType.NetworkFilesystem) || dataStoreVO.getPoolType().equals(StoragePoolType.Filesystem)) {
                protocoltype = "nfs";
            } else {
                protocoltype = "iscsi";
            }

            FileSystem esvolume = null;
            try {
                esvolume = ElastistorUtil.createElastistorVolume(volumeName, dataStoreVO.getUuid(), quotaSize, Iops, protocoltype, volumeName);
            } catch (Throwable e) {
                s_logger.error(e.toString(), e);
                result.setResult(e.toString());
                callback.complete(result);
                throw new CloudRuntimeException(e.getMessage());
            }

            if (esvolume.getNfsenabled().equalsIgnoreCase("true")) {
                volume.set_iScsiName(esvolume.getPath());
                volume.setPoolType(StoragePoolType.NetworkFilesystem);
            } else {
                iqn = esvolume.getIqn();
                String modifiediqn = "/" + iqn + "/0";
                volume.set_iScsiName(modifiediqn);
                volume.setPoolType(StoragePoolType.IscsiLUN);
            }

            volume.setFolder(String.valueOf(esvolume.getUuid()));
            volume.setPoolId(storagePoolId);
            volume.setUuid(esvolume.getUuid());
            volume.setPath(null);

            _volumeDao.update(volume.getId(), volume);

            // create new volume details for the volume
            //updateVolumeDetails(volume, esvolume);

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();

            Long inbytes = volume.getSize();

            usedBytes += inbytes;

            storagePool.setCapacityIops(capacityIops);
            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
            s_logger.info("Elastistor volume creation complete.");
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
            s_logger.error(errMsg);
        }

        result.setResult(errMsg);

        callback.complete(result);
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {

        String errMsg = null;

        StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

        // if the primary storage is not managed(thick provisioned) then no need
        // to delete volume at elastistor, just
        // call the super(default) to delete a vdi(disk) only.

        if (!(storagePool.isManaged())) {
            super.deleteAsync(dataStore, dataObject, callback);
            return;
        }

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo) dataObject;

            long storagePoolId = dataStore.getId();
            boolean result = false;
            try {
                result = ElastistorUtil.deleteElastistorVolume(volumeInfo.getUuid());
            } catch (Throwable e) {
                e.printStackTrace();
                CommandResult result2 = new CommandResult();
                result2.setResult(e.toString());
                callback.complete(result2);
            }

            if (result) {
                long usedBytes = storagePool.getUsedBytes();
                long capacityIops = storagePool.getCapacityIops();

                usedBytes -= volumeInfo.getSize();
                capacityIops += volumeInfo.getMaxIops();

                storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);
                storagePool.setCapacityIops(capacityIops < 0 ? 0 : capacityIops);

                _storagePoolDao.update(storagePoolId, storagePool);
            } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
            }

        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
        }

        CommandResult result = new CommandResult();

        result.setResult(errMsg);

        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {

        s_logger.debug("Resize elastistor volume started");
        Boolean status = false;
        VolumeObject vol = (VolumeObject) data;
        StoragePool pool = (StoragePool) data.getDataStore();

        ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();

        CreateCmdResult result = new CreateCmdResult(null, null);

        StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());

        if (!(poolVO.isManaged())) {
            super.resize(data, callback);
            return;
        }

        try {

            status = ElastistorUtil.updateElastistorVolumeSize(vol.getUuid(), resizeParameter.newSize);

        } catch (Throwable e) {
            s_logger.error("Resize elastistor volume failed, please contact elastistor admin.", e);
            result.setResult(e.toString());
            callback.complete(result);
        }

        if (status) {
            // now updating the cloudstack storagepool usedbytes and volume
            Long usedBytes = poolVO.getUsedBytes();
            Long currentVolumeSize = vol.getSize();
            Long newUsedBytes;

            if (currentVolumeSize < resizeParameter.newSize) {
                newUsedBytes = usedBytes + (resizeParameter.newSize - currentVolumeSize);
                poolVO.setUsedBytes(newUsedBytes);
            } else {
                newUsedBytes = usedBytes - (currentVolumeSize - resizeParameter.newSize);
                poolVO.setUsedBytes(newUsedBytes);
            }

            _storagePoolDao.update(pool.getId(), poolVO);

            vol.getVolume().setSize(resizeParameter.newSize);
            vol.update();
            callback.complete(result);
        } else {
            callback.complete(result);
        }

    }

    //this method will utilize the volume details table to add third party volume properties
    public void updateVolumeDetails(VolumeVO volume, FileSystem esvolume) {

        VolumeDetailVO compression = new VolumeDetailVO(volume.getId(), "compression", esvolume.getCompression(), false);
        _volumeDetailsDao.persist(compression);
        VolumeDetailVO deduplication = new VolumeDetailVO(volume.getId(), "deduplication", esvolume.getDeduplication(), false);
        _volumeDetailsDao.persist(deduplication);
        VolumeDetailVO sync = new VolumeDetailVO(volume.getId(), "sync", esvolume.getSync(), false);
        _volumeDetailsDao.persist(sync);
        VolumeDetailVO graceallowed = new VolumeDetailVO(volume.getId(), "graceallowed", esvolume.getGraceallowed(), false);
        _volumeDetailsDao.persist(graceallowed);

    }

    @Override
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        long volumeSize = volume.getSize();
        Integer hypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

        if (hypervisorSnapshotReserve != null) {
            if (hypervisorSnapshotReserve < 25) {
                hypervisorSnapshotReserve = 25;
            }

            volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
        }

        return volumeSize;
    }

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        try {
            s_logger.info("taking elastistor volume snapshot");
            SnapshotObjectTO snapshotTO = (SnapshotObjectTO)snapshot.getTO();

            String volumeid = snapshotTO.getVolume().getUuid();
            String snapshotname = snapshotTO.getName();

            Answer answer = ElastistorUtil.createElastistorVolumeSnapshot(volumeid, snapshotname);

            if(answer.getResult() == false){
                s_logger.info("elastistor volume snapshot failed");
                throw new CloudRuntimeException("elastistor volume snapshot failed");
            }else{
                s_logger.info("elastistor volume snapshot succesfull");

                snapshotTO.setPath(answer.getDetails());

                CreateObjectAnswer createObjectAnswer = new CreateObjectAnswer(snapshotTO);

                result = new CreateCmdResult(null, createObjectAnswer);

                result.setResult(null);
            }
        }
         catch (Throwable e) {
            s_logger.debug("Failed to take snapshot: " + e.getMessage());
            result = new CreateCmdResult(null, null);
            result.setResult(e.toString());
        }
        callback.complete(result);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> mapCapabilities = new HashMap<String, String>();

        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());

        return mapCapabilities;
    }

}
