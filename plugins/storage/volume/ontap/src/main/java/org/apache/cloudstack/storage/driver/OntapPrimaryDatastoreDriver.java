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
package org.apache.cloudstack.storage.driver;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
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
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.SANStrategy;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntapPrimaryDatastoreDriver implements PrimaryDataStoreDriver {

    private static final Logger logger = (Logger)LogManager.getLogger(OntapPrimaryDatastoreDriver.class);

    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;

    @Override
    public Map<String, String> getCapabilities() {
        logger.trace("OntapPrimaryDatastoreDriver: getCapabilities: Called");
        Map<String, String> mapCapabilities = new HashMap<>();

        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());

        return mapCapabilities;
    }

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
        CreateCmdResult createCmdResult = null;
        String path = null;
        String errMsg = null;
        if (dataStore == null) {
            throw new InvalidParameterValueException("createAsync: dataStore should not be null");
        }
        if (dataObject == null) {
            throw new InvalidParameterValueException("createAsync: dataObject should not be null");
        }
        if (callback == null) {
            throw new InvalidParameterValueException("createAsync: callback should not be null");
        }

       try {
           logger.info("createAsync: Volume creation starting for data store [{}] and data object [{}] of type [{}]",
                    dataStore, dataObject, dataObject.getType());
           if (dataObject.getType() == DataObjectType.VOLUME) {
                path = createCloudStackVolume(dataStore.getId(), dataObject);
               createCmdResult = new CreateCmdResult(path, new Answer(null, true, null));
           } else {
                errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
           }
       } catch (Exception e) {
            errMsg = e.getMessage();
            logger.error("createAsync: Volume creation failed for dataObject [{}]: {}", dataObject, errMsg);
            createCmdResult = new CreateCmdResult(null, new Answer(null, false, errMsg));
            createCmdResult.setResult(e.toString());
       } finally {
            callback.complete(createCmdResult);
       }
    }

    private String createCloudStackVolume(long storagePoolId, DataObject dataObject) {
        String path = null;
        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);
        if(storagePool == null) {
            throw new CloudRuntimeException("createCloudStackVolume : Storage Pool not found for id: " + storagePoolId);
        }
        List<String> keyList = Arrays.asList(Constants.USERNAME, Constants.PROTOCOL, Constants.IS_DISAGGREGATED, Constants.PASSWORD, Constants.MANAGEMENT_LIF,
                Constants.SVM_NAME, Constants.NAME);
        Map<String, String> storagePoolDetailMap = storagePoolDetailsDao.listDetailsKeyPairs(storagePoolId, keyList);
        OntapStorage ontapStorage = new OntapStorage(storagePoolDetailMap.get(Constants.USERNAME), storagePoolDetailMap.get(Constants.PASSWORD),
                storagePoolDetailMap.get(Constants.MANAGEMENT_LIF), storagePoolDetailMap.get(Constants.SVM_NAME), Constants.ProtocolType.valueOf(storagePoolDetailMap.get(Constants.PROTOCOL)),
                Boolean.parseBoolean(storagePoolDetailMap.get(Constants.IS_DISAGGREGATED)));
        StorageStrategy storageStrategy = StorageProviderFactory.createStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect();
        if (isValid) {
            String svmName = ontapStorage.getSvmName();
            String lunOrFileName = dataObject.getName();
            Long size = dataObject.getSize();
            String volName = storagePool.getName();

            // Create LUN based on protocol
            if (ontapStorage.getProtocol().equals(Constants.ISCSI)) {
                SANStrategy sanStrategy = StorageProviderFactory.getSANStrategy(ontapStorage);
                Lun lun = sanStrategy.createLUN(svmName, volName, lunOrFileName, size , Lun.OsTypeEnum.LINUX.getValue());
                if(lun.getName() == null || lun.getName().isEmpty()) {
                    throw new CloudRuntimeException("createCloudStackVolume : LUN Name is invalid");
                }
                path = lun.getName();
            }
        } else {
            throw new CloudRuntimeException("createCloudStackVolume : ONTAP details validation failed, cannot connect to ONTAP cluster");
        }
        return path;
    }

    @Override
    public void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback) {

    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {

    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {

    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        if (dataStore == null) {
            throw new CloudRuntimeException("grantAccess: dataStore should not be null");
        }
        if (dataObject == null) {
            throw new CloudRuntimeException("grantAccess: dataObject should not be null");
        }
        if (host == null) {
            throw new CloudRuntimeException("grantAccess: host should not be null");
        }
        return true;
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {

    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
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
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {

    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {

    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {

    }

    @Override
    public boolean canProvideStorageStats() {
        return true;
    }

    @Override
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        return null;
    }

    @Override
    public boolean canProvideVolumeStats() {
        return true;
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
        return true;
    }

    @Override
    public void provideVmInfo(long vmId, long volumeId) {

    }

    @Override
    public boolean isVmTagsNeeded(String tagKey) {
        return true;
    }

    @Override
    public void provideVmTags(long vmId, long volumeId, String tagValue) {

    }

    @Override
    public boolean isStorageSupportHA(Storage.StoragePoolType type) {
        return true;
    }

    @Override
    public void detachVolumeFromAllStorageNodes(Volume volume) {

    }
}
