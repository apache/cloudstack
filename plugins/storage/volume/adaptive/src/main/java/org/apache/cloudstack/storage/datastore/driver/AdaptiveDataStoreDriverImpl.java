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

import java.util.Map;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterConstants;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterContext;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDataObject;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDiskOffering;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterFactory;
import org.apache.cloudstack.storage.datastore.adapter.ProviderSnapshot;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStats;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStorageStats;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.provider.AdaptivePrimaryDatastoreAdapterFactoryMap;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.snapshot.SnapshotObject;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdaptiveDataStoreDriverImpl extends CloudStackPrimaryDataStoreDriverImpl {

    protected Logger logger = LogManager.getLogger(getClass());

    private String providerName = null;

    @Inject
    AccountManager _accountMgr;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    ProjectDao _projectDao;
    @Inject
    SnapshotDataStoreDao _snapshotDataStoreDao;
    @Inject
    SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    VolumeDetailsDao _volumeDetailsDao;
    @Inject
    VMTemplatePoolDao _vmTemplatePoolDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    VMTemplateDao _vmTemplateDao;
    @Inject
    DataCenterDao _datacenterDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    VolumeService _volumeService;
    @Inject
    VolumeDataFactory volumeDataFactory;

    private AdaptivePrimaryDatastoreAdapterFactoryMap _adapterFactoryMap = null;

    public AdaptiveDataStoreDriverImpl(AdaptivePrimaryDatastoreAdapterFactoryMap factoryMap) {
        this._adapterFactoryMap = factoryMap;
    }

    @Override
    public DataTO getTO(DataObject data) {
        // we need to get connectionId and and the VLUN ID for currently attached hosts to add to the DataTO object
        DataTO to = null;
        if (data.getType() == DataObjectType.VOLUME) {
            VolumeObjectTO vto = new VolumeObjectTO((VolumeObject)data);
            vto.setPath(getPath(data));
            to = vto;
        } else if (data.getType() == DataObjectType.TEMPLATE) {
            TemplateObjectTO tto =  new TemplateObjectTO((TemplateObject)data);
            tto.setPath(getPath(data));
            to = tto;
        } else if (data.getType() == DataObjectType.SNAPSHOT) {
            SnapshotObjectTO sto = new SnapshotObjectTO((SnapshotObject)data);
            sto.setPath(getPath(data));
            to = sto;
        } else {
            to = super.getTO(data);
        }
        return to;
    }

    /*
     * For the given data object, return the path with current connection info.  If a snapshot
     * object is passed, we will determine if a temporary volume is avialable for that
     * snapshot object and return that conneciton info instead.
     */
    String getPath(DataObject data) {
        StoragePoolVO storagePool = _storagePoolDao.findById(data.getDataStore().getId());
        Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
        ProviderAdapter api = getAPI(storagePool, details);

        ProviderAdapterDataObject dataIn = newManagedDataObject(data, storagePool);

        /** This means the object is not yet associated with the external provider so path is null */
        if (dataIn.getExternalName() == null) {
            return null;
        }

        ProviderAdapterContext context = newManagedVolumeContext(data);
        Map<String,String> connIdMap = api.getConnectionIdMap(dataIn);
        ProviderVolume volume = api.getVolume(context, dataIn);
        // if this is an existing object, generate the path for it.
        String finalPath = null;
        if (volume != null) {
            finalPath = generatePathInfo(volume, connIdMap);
        }
        return finalPath;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    public ProviderAdapter getAPI(StoragePool pool, Map<String, String> details) {
        return _adapterFactoryMap.getAPI(pool.getUuid(), pool.getStorageProviderName(), details);
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        try {
            logger.info("Volume creation starting for data store [" + dataStore.getName() +
                    "] and data object [" + dataObject.getUuid() + "] of type [" + dataObject.getType() + "]");

            // quota size of the cloudbyte volume will be increased with the given
            // HypervisorSnapshotReserve
            Long volumeSizeBytes = dataObject.getSize();
            // cloudstack talks bytes, primera talks MiB
            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());
            Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());

            ProviderAdapter api = getAPI(storagePool, details);
            ProviderAdapterContext context = newManagedVolumeContext(dataObject);
            ProviderAdapterDataObject dataIn = newManagedDataObject(dataObject, storagePool);
            ProviderAdapterDiskOffering inDiskOffering = null;
            // only get the offering if its a volume type.  If its a template type we skip this.
            if (DataObjectType.VOLUME.equals(dataObject.getType())) {
                // get the disk offering as provider may need to see details of this to
                // provision the correct type of volume
                VolumeVO volumeVO = _volumeDao.findById(dataObject.getId());
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(volumeVO.getDiskOfferingId());
                if (diskOffering.isUseLocalStorage()) {
                    throw new CloudRuntimeException(
                            "Disk offering requires local storage but this storage provider does not suppport local storage.  Please contact the cloud adminstrator to have the disk offering configuration updated to avoid this conflict.");
                }
                inDiskOffering = new ProviderAdapterDiskOffering(diskOffering);
            }

            // if its a template and it already exist, just return the info -- may mean a previous attempt to
            // copy this template failed after volume creation and its state has not advanced yet.
            ProviderVolume volume = null;
            if (DataObjectType.TEMPLATE.equals(dataObject.getType())) {
                volume = api.getVolume(context, dataIn);
                if (volume != null) {
                    logger.info("Template volume already exists [" + dataObject.getUuid() + "]");
                }
            }

            // create the volume if it didn't already exist
            if (volume == null) {
                // klunky - if this fails AND this detail property is set, it means upstream may have already created it
                // in VolumeService and DataMotionStrategy tries to do it again before copying...
                try {
                    volume = api.create(context, dataIn, inDiskOffering, volumeSizeBytes);
                } catch (Exception e) {
                    VolumeDetailVO csId = _volumeDetailsDao.findDetail(dataObject.getId(), "cloneOfTemplate");
                    if (csId != null && csId.getId() > 0) {
                        volume = api.getVolume(context, dataIn);
                    } else {
                        throw e;
                    }
                }
                logger.info("New volume created on remote storage for [" + dataObject.getUuid() + "]");
            }

            // set these from the discovered or created volume before proceeding
            dataIn.setExternalName(volume.getExternalName());
            dataIn.setExternalUuid(volume.getExternalUuid());

            // update the cloudstack metadata about the volume
            persistVolumeOrTemplateData(storagePool, details, dataObject, volume, null);

            result = new CreateCmdResult(dataObject.getUuid(), new Answer(null));
            result.setSuccess(true);
            logger.info("Volume creation complete for [" + dataObject.getUuid() + "]");
        } catch (Throwable e) {
            logger.error("Volume creation  failed for dataObject [" + dataObject.getUuid() + "]: " + e.toString(), e);
            result = new CreateCmdResult(null, new Answer(null));
            result.setResult(e.toString());
            result.setSuccess(false);
            throw new CloudRuntimeException(e.getMessage());
        } finally {
            if (callback != null)
                callback.complete(result);
        }
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CommandResult> callback) {
        logger.debug("Delete volume started");
        CommandResult result = new CommandResult();
        try {
            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());
            Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
            ProviderAdapter api = getAPI(storagePool, details);
            ProviderAdapterContext context = newManagedVolumeContext(dataObject);
            ProviderAdapterDataObject inData = newManagedDataObject(dataObject, storagePool);
            // skip adapter delete if neither external identifier is set.  Probably means the volume
            // create failed before this chould be set
            if (!(inData.getExternalName() == null && inData.getExternalUuid() == null)) {
                api.delete(context, inData);
            }
            result.setResult("Successfully deleted volume");
            result.setSuccess(true);
        } catch (Throwable e) {
            logger.error("Result to volume delete failed with exception", e);
            result.setResult(e.toString());
        } finally {
            if (callback != null)
                callback.complete(result);
        }
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destdata,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result = null;
        try {
            logger.info("Copying volume " + srcdata.getUuid() + " to " + destdata.getUuid() + "]");

            if (!canCopy(srcdata, destdata)) {
                throw new CloudRuntimeException(
                        "The data store provider is unable to perform copy operations because the source or destination object is not the correct type of volume");
            }

            try {
                StoragePoolVO storagePool = _storagePoolDao.findById(srcdata.getDataStore().getId());
                Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
                ProviderAdapter api = getAPI(storagePool, details);

                logger.info("Copy volume " + srcdata.getUuid() + " to " + destdata.getUuid());

                ProviderVolume outVolume;
                ProviderAdapterContext context = newManagedVolumeContext(destdata);
                ProviderAdapterDataObject sourceIn = newManagedDataObject(srcdata, storagePool);
                ProviderAdapterDataObject destIn = newManagedDataObject(destdata, storagePool);

                outVolume = api.copy(context, sourceIn, destIn);

                // populate this data - it may be needed later
                destIn.setExternalName(outVolume.getExternalName());
                destIn.setExternalConnectionId(outVolume.getExternalConnectionId());
                destIn.setExternalUuid(outVolume.getExternalUuid());

                // if we copied from one volume to another, the target volume's disk offering or user input may be of a larger size
                // we won't, however, shrink a volume if its smaller.
                if (outVolume.getAllocatedSizeInBytes() < destdata.getSize()) {
                    logger.info("Resizing volume " + destdata.getUuid() + " to requested target volume size of " + destdata.getSize());
                    api.resize(context, destIn, destdata.getSize());
                }

                // initial volume info does not have connection map yet.  That is added when grantAccess is called later.
                String finalPath = generatePathInfo(outVolume, null);
                persistVolumeData(storagePool, details, destdata, outVolume, null);
                logger.info("Copy completed from [" + srcdata.getUuid() + "] to [" + destdata.getUuid() + "]");

                VolumeObjectTO voto = new VolumeObjectTO();
                voto.setPath(finalPath);

                result = new CopyCommandResult(finalPath, new CopyCmdAnswer(voto));
                result.setSuccess(true);
            } catch (Throwable e) {
                logger.error("Result to volume copy failed with exception", e);
                result = new CopyCommandResult(null, null);
                result.setSuccess(false);
                result.setResult(e.toString());
            }
        } finally {
            if (callback != null)
                callback.complete(result);
        }
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, callback);
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        logger.debug("canCopy: Checking srcData [" + srcData.getUuid() + ":" + srcData.getType() + ":"
                + srcData.getDataStore().getId() + " AND destData ["
                + destData.getUuid() + ":" + destData.getType() + ":" + destData.getDataStore().getId() + "]");
        try {
            if (!isSameProvider(srcData)) {
                logger.debug("canCopy: No we can't -- the source provider is NOT the correct type for this driver!");
                return false;
            }

            if (!isSameProvider(destData)) {
                logger.debug("canCopy: No we can't -- the destination provider is NOT the correct type for this driver!");
                return false;
            }
            logger.debug(
                    "canCopy: Source and destination are the same so we can copy via storage endpoint, checking that the source actually exists");
            StoragePoolVO poolVO = _storagePoolDao.findById(srcData.getDataStore().getId());
            Map<String, String> details = _storagePoolDao.getDetails(srcData.getDataStore().getId());
            ProviderAdapter api = getAPI(poolVO, details);

            /**
             * The storage provider generates its own names for snapshots which we store and
             * retrieve when needed
             */
            ProviderAdapterContext context = newManagedVolumeContext(srcData);
            ProviderAdapterDataObject srcDataObject = newManagedDataObject(srcData, poolVO);
            if (srcData instanceof SnapshotObject) {
                ProviderSnapshot snapshot = api.getSnapshot(context, srcDataObject);
                if (snapshot == null) {
                    return false;
                } else {
                    return true;
                }
            } else {
                ProviderVolume vol = api.getVolume(context, srcDataObject);
                if (vol == null) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (Throwable e) {
            logger.warn("Problem checking if we canCopy", e);
            return false;
        }
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        logger.debug("Resize volume started");
        CreateCmdResult result = null;
        try {

            // Boolean status = false;
            VolumeObject vol = (VolumeObject) data;
            StoragePool pool = (StoragePool) data.getDataStore();

            ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();

            StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());

            if (!(poolVO.isManaged())) {
                super.resize(data, callback);
                return;
            }

            try {
                Map<String, String> details = _storagePoolDao.getDetails(pool.getId());
                ProviderAdapter api = getAPI(pool, details);

                // doesn't support shrink (maybe can truncate but separate API calls to
                // investigate)
                if (vol.getSize() > resizeParameter.newSize) {
                    throw new CloudRuntimeException("Storage provider does not support shrinking an existing volume");
                }

                ProviderAdapterContext context = newManagedVolumeContext(data);
                ProviderAdapterDataObject dataIn = newManagedDataObject(data, poolVO);
                if (logger.isDebugEnabled()) logger.debug("Calling provider API to resize volume " + data.getUuid() + " to " + resizeParameter.newSize);
                api.resize(context, dataIn, resizeParameter.newSize);

                if (vol.isAttachedVM()) {
                    if (VirtualMachine.State.Running.equals(vol.getAttachedVM().getState())) {
                        if (logger.isDebugEnabled()) logger.debug("Notify currently attached VM of volume resize for " + data.getUuid() + " to " + resizeParameter.newSize);
                        _volumeService.resizeVolumeOnHypervisor(vol.getId(), resizeParameter.newSize, vol.getAttachedVM().getHostId(), vol.getAttachedVM().getInstanceName());
                    }
                }

                result = new CreateCmdResult(data.getUuid(), new Answer(null));
                result.setSuccess(true);
            } catch (Throwable e) {
                logger.error("Resize volume failed, please contact cloud support.", e);
                result = new CreateCmdResult(null, new Answer(null));
                result.setResult(e.toString());
                result.setSuccess(false);
            }
        } finally {
            if (callback != null)
                callback.complete(result);
        }

    }

    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        logger.debug("Granting host " + host.getName() + " access to volume " + dataObject.getUuid());

        try {
            StoragePoolVO storagePool = _storagePoolDao.findById(dataObject.getDataStore().getId());
            Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
            ProviderAdapter api = getAPI(storagePool, details);

            ProviderAdapterContext context = newManagedVolumeContext(dataObject);
            ProviderAdapterDataObject sourceIn = newManagedDataObject(dataObject, storagePool);
            api.attach(context, sourceIn, host.getName());

            // rewrite the volume data, especially the connection string for informational purposes - unless it was turned off above
            ProviderVolume vol = api.getVolume(context, sourceIn);
            ProviderAdapterDataObject dataIn = newManagedDataObject(dataObject, storagePool);
            Map<String,String> connIdMap = api.getConnectionIdMap(dataIn);
            persistVolumeOrTemplateData(storagePool, details, dataObject, vol, connIdMap);


            logger.info("Granted host " + host.getName() + " access to volume " + dataObject.getUuid());
            return true;
        } catch (Throwable e) {
            String msg = "Error granting host " + host.getName() + " access to volume " + dataObject.getUuid() + ":" + e.getMessage();
            logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        // nothing to do if the host is null
        if (dataObject == null || host == null || dataStore == null) {
            return;
        }

        logger.debug("Revoking access for host " + host.getName() + " to volume " + dataObject.getUuid());

        try {
            StoragePoolVO storagePool = _storagePoolDao.findById(dataObject.getDataStore().getId());
            Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
            ProviderAdapter api = getAPI(storagePool, details);

            ProviderAdapterContext context = newManagedVolumeContext(dataObject);
            ProviderAdapterDataObject sourceIn = newManagedDataObject(dataObject, storagePool);

            api.detach(context, sourceIn, host.getName());

            // rewrite the volume data, especially the connection string for informational purposes
            ProviderVolume vol = api.getVolume(context, sourceIn);
            ProviderAdapterDataObject dataIn = newManagedDataObject(dataObject, storagePool);
            Map<String,String> connIdMap = api.getConnectionIdMap(dataIn);
            persistVolumeOrTemplateData(storagePool, details, dataObject, vol, connIdMap);

            logger.info("Revoked access for host " + host.getName() + " to volume " + dataObject.getUuid());
        } catch (Throwable e) {
            String msg = "Error revoking access for host " + host.getName() + " to volume " + dataObject.getUuid() + ":" + e.getMessage();
            logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo,
            QualityOfServiceState qualityOfServiceState) {
        logger.info("handleQualityOfServiceVolumeMigration: " + volumeInfo.getUuid() + " " +
                volumeInfo.getPath() + ": " + qualityOfServiceState.toString());
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {
        VolumeInfo volume = (VolumeInfo) dataObject;
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
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        try {
            logger.debug("taking volume snapshot");
            SnapshotObjectTO snapshotTO = (SnapshotObjectTO) snapshot.getTO();

            VolumeInfo baseVolume = snapshot.getBaseVolume();
            DataStore ds = baseVolume.getDataStore();
            StoragePoolVO storagePool = _storagePoolDao.findById(ds.getId());

            Map<String, String> details = _storagePoolDao.getDetails(ds.getId());
            ProviderAdapter api = getAPI(storagePool, details);

            ProviderAdapterContext context = newManagedVolumeContext(snapshot);
            ProviderAdapterDataObject inVolumeDO = newManagedDataObject(baseVolume, storagePool);
            ProviderAdapterDataObject inSnapshotDO = newManagedDataObject(snapshot, storagePool);
            ProviderSnapshot outSnapshot = api.snapshot(context, inVolumeDO, inSnapshotDO);

            // add the snapshot to the host group (needed for copying to non-provider storage
            // to create templates, etc)
            String finalAddress = outSnapshot.getAddress();
            snapshotTO.setPath(finalAddress);
            snapshotTO.setName(outSnapshot.getName());
            snapshotTO.setHypervisorType(HypervisorType.KVM);

            // unclear why this is needed vs snapshotTO.setPath, but without it the path on
            // the target snapshot object isn't set
            // so a volume created from it also is not set and can't be attached to a VM
            SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshot.getId(),
                    DiskTO.PATH, finalAddress, true);
            _snapshotDetailsDao.persist(snapshotDetail);

            // save the name (reuse on revert)
            snapshotDetail = new SnapshotDetailsVO(snapshot.getId(),
                    ProviderAdapterConstants.EXTERNAL_NAME, outSnapshot.getExternalName(), true);
            _snapshotDetailsDao.persist(snapshotDetail);

            // save the uuid (reuse on revert)
            snapshotDetail = new SnapshotDetailsVO(snapshot.getId(),
                    ProviderAdapterConstants.EXTERNAL_UUID, outSnapshot.getExternalUuid(), true);
            _snapshotDetailsDao.persist(snapshotDetail);

            result = new CreateCmdResult(finalAddress, new CreateObjectAnswer(snapshotTO));
            result.setResult("Snapshot completed with new WWN " + finalAddress);
            result.setSuccess(true);
        } catch (Throwable e) {
            logger.debug("Failed to take snapshot: " + e.getMessage());
            result = new CreateCmdResult(null, null);
            result.setResult(e.toString());
        } finally {
            if (callback != null)
                callback.complete(result);
        }
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, SnapshotInfo snapshotOnPrimaryStore,
            AsyncCompletionCallback<CommandResult> callback) {

        CommandResult result = new CommandResult();
        ProviderAdapter api = null;
        try {
            DataStore ds = snapshotOnPrimaryStore.getDataStore();
            StoragePoolVO storagePool = _storagePoolDao.findById(ds.getId());
            Map<String, String> details = _storagePoolDao.getDetails(ds.getId());
            api = getAPI(storagePool, details);

            String externalName = null;
            String externalUuid = null;
            List<SnapshotDetailsVO> list = _snapshotDetailsDao.findDetails(snapshot.getId(),
                    ProviderAdapterConstants.EXTERNAL_NAME);
            if (list != null && list.size() > 0) {
                externalName = list.get(0).getValue();
            }

            list = _snapshotDetailsDao.findDetails(snapshot.getId(), ProviderAdapterConstants.EXTERNAL_UUID);
            if (list != null && list.size() > 0) {
                externalUuid = list.get(0).getValue();
            }

            ProviderAdapterContext context = newManagedVolumeContext(snapshot);
            ProviderAdapterDataObject inSnapshotDO = newManagedDataObject(snapshot, storagePool);
            inSnapshotDO.setExternalName(externalName);
            inSnapshotDO.setExternalUuid(externalUuid);

            // perform promote (async, wait for job to finish)
            api.revert(context, inSnapshotDO);

            // set command as success
            result.setSuccess(true);
        } catch (Throwable e) {
            logger.warn("revertSnapshot failed", e);
            result.setResult(e.toString());
            result.setSuccess(false);
        } finally {
            if (callback != null)
                callback.complete(result);
        }
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        long usedSpaceBytes = 0;
        // Volumes
        List<VolumeVO> volumes = _volumeDao.findByPoolIdAndState(storagePool.getId(), Volume.State.Ready);
        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                usedSpaceBytes += volume.getSize();

                long vmSnapshotChainSize = volume.getVmSnapshotChainSize() == null ? 0
                        : volume.getVmSnapshotChainSize();
                usedSpaceBytes += vmSnapshotChainSize;
            }
        }

        // Snapshots
        List<SnapshotDataStoreVO> snapshots = _snapshotDataStoreDao.listByStoreIdAndState(storagePool.getId(),
                ObjectInDataStoreStateMachine.State.Ready);
        if (snapshots != null) {
            for (SnapshotDataStoreVO snapshot : snapshots) {
                usedSpaceBytes += snapshot.getSize();
            }
        }

        // Templates
        List<VMTemplateStoragePoolVO> templates = _vmTemplatePoolDao.listByPoolIdAndState(storagePool.getId(),
                ObjectInDataStoreStateMachine.State.Ready);
        if (templates != null) {
            for (VMTemplateStoragePoolVO template : templates) {
                usedSpaceBytes += template.getTemplateSize();
            }
        }

        logger.debug("Used/Allocated storage space (in bytes): " + String.valueOf(usedSpaceBytes));

        return usedSpaceBytes;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return super.getUsedIops(storagePool);
    }

    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> mapCapabilities = new HashMap<String, String>();

        mapCapabilities.put(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString(), Boolean.TRUE.toString());
        mapCapabilities.put(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString(), Boolean.TRUE.toString()); // set to false because it causes weird behavior when copying templates to root volumes
        mapCapabilities.put(DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString(), Boolean.TRUE.toString());
        ProviderAdapterFactory factory = _adapterFactoryMap.getFactory(this.getProviderName());
        if (factory != null) {
            mapCapabilities.put("CAN_DIRECT_ATTACH_SNAPSHOT", factory.canDirectAttachSnapshot().toString());
        } else {
            mapCapabilities.put("CAN_DIRECT_ATTACH_SNAPSHOT", Boolean.FALSE.toString());
        }
        return mapCapabilities;
    }

    @Override
    public boolean canProvideStorageStats() {
        return true;
    }

    @Override
    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
        String capacityBytesStr = details.get("capacityBytes");
        Long capacityBytes = null;
        if (capacityBytesStr == null) {
            ProviderAdapter api = getAPI(storagePool, details);
            ProviderVolumeStorageStats stats = api.getManagedStorageStats();
            if (stats == null) {
                return null;
            }
            capacityBytes = stats.getCapacityInBytes();
        } else {
            capacityBytes = Long.parseLong(capacityBytesStr);
        }
        Long usedBytes = this.getUsedBytes(storagePool);
        return new Pair<Long, Long>(capacityBytes, usedBytes);
    }

    @Override
    public boolean canProvideVolumeStats() {
        return true;
    }

    @Override
    public boolean requiresAccessForMigration(DataObject dataObject) {
        return true;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumePath) {
        Map<String, String> details = _storagePoolDao.getDetails(storagePool.getId());
        ProviderAdapter api = getAPI(storagePool, details);
        ProviderVolume.AddressType addressType = null;
        if (volumePath.indexOf(";") > 1) {
            String[] fields = volumePath.split(";");
            if (fields.length > 0) {
                for (String field: fields) {
                    if (field.trim().startsWith("address=")) {
                        String[] toks = field.split("=");
                        if (toks.length > 1) {
                            volumePath = toks[1];
                        }
                    } else if (field.trim().startsWith("type=")) {
                        String[] toks = field.split("=");
                        if (toks.length > 1) {
                            addressType = ProviderVolume.AddressType.valueOf(toks[1]);
                        }
                    }
                }
            }
        } else {
            addressType = ProviderVolume.AddressType.FIBERWWN;
        }
        // limited context since this is not at an account level
        ProviderAdapterContext context = new ProviderAdapterContext();
        context.setZoneId(storagePool.getDataCenterId());
        ProviderVolume volume = api.getVolumeByAddress(context, addressType, volumePath);

        if (volume == null) {
            return null;
        }

        ProviderAdapterDataObject object = new ProviderAdapterDataObject();
        object.setExternalUuid(volume.getExternalUuid());
        object.setExternalName(volume.getExternalName());
        object.setType(ProviderAdapterDataObject.Type.VOLUME);
        ProviderVolumeStats stats = api.getVolumeStats(context, object);

        Long provisionedSizeInBytes = null;
        Long allocatedSizeInBytes = null;
        if (stats != null) {
            provisionedSizeInBytes = stats.getActualUsedInBytes();
            allocatedSizeInBytes = stats.getAllocatedInBytes();
        }

        if (provisionedSizeInBytes == null || allocatedSizeInBytes == null) {
            return null;
        }
        return new Pair<Long, Long>(provisionedSizeInBytes, allocatedSizeInBytes);
    }

    @Override
    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        Map<String, String> details = _storagePoolDao.getDetails(pool.getId());
        ProviderAdapter api = getAPI(pool, details);

        ProviderAdapterContext context = new ProviderAdapterContext();
        context.setZoneId(host.getDataCenterId());
        return api.canAccessHost(context, host.getName());
    }

    void persistVolumeOrTemplateData(StoragePoolVO storagePool, Map<String, String> storagePoolDetails,
            DataObject dataObject, ProviderVolume volume, Map<String,String> connIdMap) {
        if (dataObject.getType() == DataObjectType.VOLUME) {
            persistVolumeData(storagePool, storagePoolDetails, dataObject, volume, connIdMap);
        } else if (dataObject.getType() == DataObjectType.TEMPLATE) {
            persistTemplateData(storagePool, storagePoolDetails, dataObject, volume, connIdMap);
        }
    }

    void persistVolumeData(StoragePoolVO storagePool, Map<String, String> details, DataObject dataObject,
            ProviderVolume managedVolume, Map<String,String> connIdMap) {
        VolumeVO volumeVO = _volumeDao.findById(dataObject.getId());

        String finalPath = generatePathInfo(managedVolume, connIdMap);
        volumeVO.setPath(finalPath);
        volumeVO.setFormat(ImageFormat.RAW);
        volumeVO.setPoolId(storagePool.getId());
        volumeVO.setExternalUuid(managedVolume.getExternalUuid());
        volumeVO.setDisplay(true);
        volumeVO.setDisplayVolume(true);
        _volumeDao.update(volumeVO.getId(), volumeVO);

        volumeVO = _volumeDao.findById(volumeVO.getId());

        VolumeDetailVO volumeDetailVO = new VolumeDetailVO(volumeVO.getId(),
                DiskTO.PATH, finalPath, true);
        _volumeDetailsDao.persist(volumeDetailVO);

        volumeDetailVO = new VolumeDetailVO(volumeVO.getId(),
                ProviderAdapterConstants.EXTERNAL_NAME, managedVolume.getExternalName(), true);
        _volumeDetailsDao.persist(volumeDetailVO);

        volumeDetailVO = new VolumeDetailVO(volumeVO.getId(),
                ProviderAdapterConstants.EXTERNAL_UUID, managedVolume.getExternalUuid(), true);
        _volumeDetailsDao.persist(volumeDetailVO);
    }

    void persistTemplateData(StoragePoolVO storagePool, Map<String, String> details, DataObject dataObject,
            ProviderVolume volume, Map<String,String> connIdMap) {
        TemplateInfo templateInfo = (TemplateInfo) dataObject;
        VMTemplateStoragePoolVO templatePoolRef = _vmTemplatePoolDao.findByPoolTemplate(storagePool.getId(),
                templateInfo.getId(), null);

        templatePoolRef.setInstallPath(generatePathInfo(volume, connIdMap));
        templatePoolRef.setLocalDownloadPath(volume.getExternalName());
        templatePoolRef.setTemplateSize(volume.getAllocatedSizeInBytes());
        _vmTemplatePoolDao.update(templatePoolRef.getId(), templatePoolRef);
    }

    String generatePathInfo(ProviderVolume volume, Map<String,String> connIdMap) {
        String finalPath = String.format("type=%s; address=%s; providerName=%s; providerID=%s;",
            volume.getAddressType().toString(), volume.getAddress().toLowerCase(), volume.getExternalName(), volume.getExternalUuid());

        // if a map was provided, add the connection IDs to the path info.  the map is all the possible vlun id's used
        // across each host or the hostset (represented with host name key as "*");
        if (connIdMap != null && connIdMap.size() > 0) {
            for (String key: connIdMap.keySet()) {
               finalPath += String.format(" connid.%s=%s;", key, connIdMap.get(key));
            }
        }
        return finalPath;
    }

    ProviderAdapterContext newManagedVolumeContext(DataObject obj) {
        ProviderAdapterContext ctx = new ProviderAdapterContext();
        if (obj instanceof VolumeInfo) {
            VolumeVO vol = _volumeDao.findById(obj.getId());
            ctx.setAccountId(vol.getAccountId());
            ctx.setDomainId(vol.getDomainId());
        } else if (obj instanceof SnapshotInfo) {
            SnapshotVO snap = _snapshotDao.findById(obj.getId());
            ctx.setAccountId(snap.getAccountId());
            ctx.setDomainId(snap.getDomainId());
        } else if (obj instanceof TemplateInfo) {
            VMTemplateVO template = _vmTemplateDao.findById(obj.getId());
            ctx.setAccountId(template.getAccountId());
            // templates don't have a domain ID so always set to 0
            ctx.setDomainId(0L);
        }

        if (ctx.getAccountId() != null) {
            AccountVO acct = _accountDao.findById(ctx.getAccountId());
            if (acct != null) {
                ctx.setAccountUuid(acct.getUuid());
                ctx.setAccountName(acct.getName());
            }
        }

        if (ctx.getDomainId() != null) {
            DomainVO domain  = _domainDao.findById(ctx.getDomainId());
            if (domain != null) {
                ctx.setDomainUuid(domain.getUuid());
                ctx.setDomainName(domain.getName());
            }
        }

        return ctx;
    }

    boolean isSameProvider(DataObject obj) {
        StoragePoolVO storagePool = this._storagePoolDao.findById(obj.getDataStore().getId());
        if (storagePool != null && storagePool.getStorageProviderName().equals(this.getProviderName())) {
            return true;
        } else {
            return false;
        }
    }

    ProviderAdapterDataObject newManagedDataObject(DataObject data, StoragePool storagePool) {
        ProviderAdapterDataObject dataIn = new ProviderAdapterDataObject();
        if (data instanceof VolumeInfo) {
            List<VolumeDetailVO> list = _volumeDetailsDao.findDetails(data.getId(),
                    ProviderAdapterConstants.EXTERNAL_NAME);
            String externalName = null;
            if (list != null && list.size() > 0) {
                externalName = list.get(0).getValue();
            }

            list = _volumeDetailsDao.findDetails(data.getId(), ProviderAdapterConstants.EXTERNAL_UUID);
            String externalUuid = null;
            if (list != null && list.size() > 0) {
                externalUuid = list.get(0).getValue();
            }

            dataIn.setName(((VolumeInfo) data).getName());
            dataIn.setExternalName(externalName);
            dataIn.setExternalUuid(externalUuid);
        } else if (data instanceof SnapshotInfo) {
            List<SnapshotDetailsVO> list = _snapshotDetailsDao.findDetails(data.getId(),
                    ProviderAdapterConstants.EXTERNAL_NAME);
            String externalName = null;
            if (list != null && list.size() > 0) {
                externalName = list.get(0).getValue();
            }

            list = _snapshotDetailsDao.findDetails(data.getId(), ProviderAdapterConstants.EXTERNAL_UUID);
            String externalUuid = null;
            if (list != null && list.size() > 0) {
                externalUuid = list.get(0).getValue();
            }

            dataIn = new ProviderAdapterDataObject();
            dataIn.setName(((SnapshotInfo) data).getName());
            dataIn.setExternalName(externalName);
            dataIn.setExternalUuid(externalUuid);
        } else if (data instanceof TemplateInfo) {
            TemplateInfo ti = (TemplateInfo)data;
            dataIn.setName(ti.getName());
            VMTemplateStoragePoolVO templatePoolRef = _vmTemplatePoolDao.findByPoolTemplate(storagePool.getId(), ti.getId(), null);
            dataIn.setExternalName(templatePoolRef.getLocalDownloadPath());
        }
        dataIn.setId(data.getId());
        dataIn.setDataStoreId(data.getDataStore().getId());
        dataIn.setDataStoreUuid(data.getDataStore().getUuid());
        dataIn.setDataStoreName(data.getDataStore().getName());
        dataIn.setUuid(data.getUuid());
        dataIn.setType(ProviderAdapterDataObject.Type.valueOf(data.getType().toString()));
        return dataIn;
    }

    public boolean volumesRequireGrantAccessWhenUsed() {
        return true;
    }
}
