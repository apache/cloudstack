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

import static org.apache.cloudstack.storage.datastore.util.NexentaUtil.NexentaPluginParameters;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance;
import org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.NexentaStorZvol;
import org.apache.cloudstack.storage.datastore.util.NexentaUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.host.Host;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;

public class NexentaPrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    private static final Logger logger = Logger.getLogger(NexentaPrimaryDataStoreDriver.class);

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        //To change body of implemented methods use File | Settings | File Templates.
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
        return 0;
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
    }

    @Inject
    private VolumeDao _volumeDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    private AccountDao _accountDao;

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    private NexentaStorAppliance getNexentaStorAppliance(long storagePoolId) {
        NexentaPluginParameters parameters = new NexentaPluginParameters();

        parameters.setNmsUrl(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.NMS_URL).getValue());
        parameters.setVolume(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.VOLUME).getValue());
        parameters.setStorageType(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.STORAGE_TYPE).getValue());
        parameters.setStorageHost(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.STORAGE_HOST).getValue());
        parameters.setStoragePort(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.STORAGE_PORT).getValue());
        parameters.setStoragePath(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.STORAGE_PATH).getValue());
        parameters.setSparseVolumes(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.SPARSE_VOLUMES).getValue());
        parameters.setVolumeBlockSize(_storagePoolDetailsDao.findDetail(storagePoolId, NexentaUtil.VOLUME_BLOCK_SIZE).getValue());

        return new NexentaStorAppliance(parameters);
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {}

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {}

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errorMessage = null;

        if (dataObject.getType() != DataObjectType.VOLUME) {
            errorMessage = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
        } else {
            VolumeInfo volumeInfo = (VolumeInfo) dataObject;

            long storagePoolId = dataStore.getId();
            NexentaStorAppliance appliance = getNexentaStorAppliance(storagePoolId);

            // TODO: maybe we should use md5(volume name) as volume name
            NexentaStorZvol zvol = (NexentaStorZvol) appliance.createVolume(volumeInfo.getName(), volumeInfo.getSize());
            iqn = zvol.getIqn();

            VolumeVO volume = this._volumeDao.findById(volumeInfo.getId());
            volume.set_iScsiName(iqn);
            volume.setFolder(zvol.getName());
            volume.setPoolType(Storage.StoragePoolType.IscsiLUN);
            volume.setPoolId(storagePoolId);
            _volumeDao.update(volume.getId(), volume);

            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);
            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();
            usedBytes += volumeInfo.getSize();
            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);
            _storagePoolDao.update(storagePoolId, storagePool);
        }

        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errorMessage == null, errorMessage));
        result.setResult(errorMessage);
        callback.complete(result);
    }

    @Override
    public void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        String errorMessage = null;
        if (data.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo) data;
            long storagePoolId = store.getId();
            NexentaStorAppliance appliance = getNexentaStorAppliance(storagePoolId);
            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);



//            _storagePoolDao.update(stoagePoolId);
        } else {
            errorMessage = String.format(
                    "Invalid DataObjectType(%s) passed to deleteAsync",
                    data.getType());
        }
        CommandResult result = new CommandResult();
        result.setResult(errorMessage);
        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {}

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {}

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {}

    @Override
    public boolean canProvideStorageStats() {
        return false;
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
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        return null;
    }

    @Override
    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        return true;
    }
}
