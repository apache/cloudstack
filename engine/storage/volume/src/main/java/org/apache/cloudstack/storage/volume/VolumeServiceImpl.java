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
package org.apache.cloudstack.storage.volume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.storage.ListVolumeAnswer;
import com.cloud.agent.api.storage.ListVolumeCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageAccessException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementService;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.RegisterVolumePayload;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.State;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.template.TemplateProp;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.google.common.base.Strings;

import static com.cloud.storage.resource.StorageProcessor.REQUEST_TEMPLATE_RELOAD;

@Component
public class VolumeServiceImpl implements VolumeService {
    private static final Logger s_logger = Logger.getLogger(VolumeServiceImpl.class);
    @Inject
    protected AgentManager agentMgr;
    @Inject
    VolumeDao volDao;
    @Inject
    PrimaryDataStoreProviderManager dataStoreMgr;
    @Inject
    DataMotionService motionSrv;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    SnapshotManager snapshotMgr;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    ConfigurationDao configDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    VMTemplatePoolDao _tmpltPoolDao;
    @Inject
    SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    HostDao _hostDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    private HostDetailsDao hostDetailsDao;
    @Inject
    private ManagementService mgr;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private VolumeDetailsDao _volumeDetailsDao;
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private TemplateDataFactory tmplFactory;
    @Inject
    private VolumeOrchestrationService _volumeMgr;
    @Inject
    private StorageManager _storageMgr;

    private final static String SNAPSHOT_ID = "SNAPSHOT_ID";

    public VolumeServiceImpl() {
    }

    private class CreateVolumeContext<T> extends AsyncRpcContext<T> {

        private final DataObject volume;
        private final AsyncCallFuture<VolumeApiResult> future;

        public CreateVolumeContext(AsyncCompletionCallback<T> callback, DataObject volume, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.future = future;
        }

        public DataObject getVolume() {
            return this.volume;
        }

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }

    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject, DataStore dataStore) {
        DataStoreDriver dataStoreDriver = dataStore.getDriver();

        if (dataStoreDriver instanceof PrimaryDataStoreDriver) {
            return ((PrimaryDataStoreDriver)dataStoreDriver).getChapInfo(dataObject);
        }

        return null;
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        DataStoreDriver dataStoreDriver = dataStore != null ? dataStore.getDriver() : null;

        if (dataStoreDriver instanceof PrimaryDataStoreDriver) {
            return ((PrimaryDataStoreDriver)dataStoreDriver).grantAccess(dataObject, host, dataStore);
        }

        return false;
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        DataStoreDriver dataStoreDriver = dataStore != null ? dataStore.getDriver() : null;
        if (dataStoreDriver == null) {
            return;
        }

        if (dataStoreDriver instanceof PrimaryDataStoreDriver) {
            ((PrimaryDataStoreDriver)dataStoreDriver).revokeAccess(dataObject, host, dataStore);
        }
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, DataStore dataStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        DataObject volumeOnStore = dataStore.create(volume);
        volumeOnStore.processEvent(Event.CreateOnlyRequested);

        try {
            CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, volumeOnStore, future);
            AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createVolumeCallback(null, null)).setContext(context);

            dataStore.getDriver().createAsync(dataStore, volumeOnStore, caller);
        } catch (CloudRuntimeException ex) {
            // clean up already persisted volume_store_ref entry in case of createVolumeCallback is never called
            VolumeDataStoreVO volStoreVO = _volumeStoreDao.findByStoreVolume(dataStore.getId(), volume.getId());
            if (volStoreVO != null) {
                VolumeInfo volObj = volFactory.getVolume(volume, dataStore);
                volObj.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            VolumeApiResult volResult = new VolumeApiResult((VolumeObject)volumeOnStore);
            volResult.setResult(ex.getMessage());
            future.complete(volResult);
        }
        return future;
    }

    protected Void createVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        DataObject vo = context.getVolume();
        String errMsg = null;
        if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed, result.getAnswer());
        } else {
            vo.processEvent(Event.OperationFailed);
            errMsg = result.getResult();
        }
        VolumeApiResult volResult = new VolumeApiResult((VolumeObject)vo);
        if (errMsg != null) {
            volResult.setResult(errMsg);
        }
        context.getFuture().complete(volResult);
        return null;
    }

    private class DeleteVolumeContext<T> extends AsyncRpcContext<T> {
        private final VolumeObject volume;
        private final AsyncCallFuture<VolumeApiResult> future;

        public DeleteVolumeContext(AsyncCompletionCallback<T> callback, VolumeObject volume, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.future = future;
        }

        public VolumeObject getVolume() {
            return this.volume;
        }

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }

    // check if a volume is expunged on both primary and secondary
    private boolean canVolumeBeRemoved(long volumeId) {
        VolumeVO vol = volDao.findById(volumeId);
        if (vol == null) {
            // already removed from volumes table
            return false;
        }
        VolumeDataStoreVO volumeStore = _volumeStoreDao.findByVolume(volumeId);
        if ((vol.getState() == State.Expunged || (vol.getPodId() == null && vol.getState() == State.Destroy)) && volumeStore == null) {
            // volume is expunged from primary, as well as on secondary
            return true;
        } else {
            return false;
        }

    }

    @DB
    @Override
    public AsyncCallFuture<VolumeApiResult> expungeVolumeAsync(VolumeInfo volume) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        if (volume.getDataStore() == null) {
            s_logger.info("Expunge volume with no data store specified");
            if (canVolumeBeRemoved(volume.getId())) {
                s_logger.info("Volume " + volume.getId() + " is not referred anywhere, remove it from volumes table");
                volDao.remove(volume.getId());
            }
            future.complete(result);
            return future;
        }

        // Find out if the volume is at state of download_in_progress on secondary storage
        VolumeDataStoreVO volumeStore = _volumeStoreDao.findByVolume(volume.getId());
        if (volumeStore != null) {
            if (volumeStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                String msg = "Volume: " + volume.getName() + " is currently being uploaded; cant' delete it.";
                s_logger.debug(msg);
                result.setSuccess(false);
                result.setResult(msg);
                future.complete(result);
                return future;
            }
        }

        VolumeVO vol = volDao.findById(volume.getId());
        if (vol == null) {
            s_logger.debug("Volume " + volume.getId() + " is not found");
            future.complete(result);
            return future;
        }

        if (!volumeExistsOnPrimary(vol)) {
            // not created on primary store
            if (volumeStore == null) {
                // also not created on secondary store
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Marking volume that was never created as destroyed: " + vol);
                }
                VMTemplateVO template = templateDao.findById(vol.getTemplateId());
                if (template != null && !template.isDeployAsIs()) {
                    volDao.remove(vol.getId());
                    future.complete(result);
                    return future;
                }
            }
        }
        VolumeObject vo = (VolumeObject)volume;

        if (volume.getDataStore().getRole() == DataStoreRole.Image) {
            // no need to change state in volumes table
            volume.processEventOnly(Event.DestroyRequested);
        } else if (volume.getDataStore().getRole() == DataStoreRole.Primary) {
            volume.processEvent(Event.ExpungeRequested);
        }

        DeleteVolumeContext<VolumeApiResult> context = new DeleteVolumeContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteVolumeCallback(null, null)).setContext(context);

        volume.getDataStore().getDriver().deleteAsync(volume.getDataStore(), volume, caller);
        return future;
    }

    public void ensureVolumeIsExpungeReady(long volumeId) {
        VolumeVO volume = volDao.findById(volumeId);
        if (volume != null && volume.getPodId() != null) {
            volume.setPodId(null);
            volDao.update(volumeId, volume);
        }
    }

    private boolean volumeExistsOnPrimary(VolumeVO vol) {
        Long poolId = vol.getPoolId();

        if (poolId == null) {
            return false;
        }

        PrimaryDataStore primaryStore = dataStoreMgr.getPrimaryDataStore(poolId);

        if (primaryStore == null) {
            return false;
        }

        if (primaryStore.isManaged()) {
            return true;
        }

        String volumePath = vol.getPath();

        if (volumePath == null || volumePath.trim().isEmpty()) {
            return false;
        }

        return true;
    }

    public Void deleteVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, DeleteVolumeContext<VolumeApiResult> context) {
        CommandResult result = callback.getResult();
        VolumeObject vo = context.getVolume();
        VolumeApiResult apiResult = new VolumeApiResult(vo);
        try {
            if (result.isSuccess()) {
                vo.processEvent(Event.OperationSuccessed);
                if (canVolumeBeRemoved(vo.getId())) {
                    s_logger.info("Volume " + vo.getId() + " is not referred anywhere, remove it from volumes table");
                    volDao.remove(vo.getId());
                }

                SnapshotDataStoreVO snapStoreVo = _snapshotStoreDao.findByVolume(vo.getId(), DataStoreRole.Primary);

                if (snapStoreVo != null) {
                    long storagePoolId = snapStoreVo.getDataStoreId();
                    StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);

                    if (storagePoolVO.isManaged()) {
                        DataStore primaryDataStore = dataStoreMgr.getPrimaryDataStore(storagePoolId);
                        Map<String, String> mapCapabilities = primaryDataStore.getDriver().getCapabilities();

                        String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
                        Boolean supportsStorageSystemSnapshots = new Boolean(value);

                        if (!supportsStorageSystemSnapshots) {
                            _snapshotStoreDao.remove(snapStoreVo.getId());
                        }
                    } else {
                        _snapshotStoreDao.remove(snapStoreVo.getId());
                    }
                }
            } else {
                vo.processEvent(Event.OperationFailed);
                apiResult.setResult(result.getResult());
            }
        } catch (Exception e) {
            s_logger.debug("ignore delete volume status update failure, it will be picked up by storage clean up thread later", e);
        }
        context.getFuture().complete(apiResult);
        return null;
    }

    @Override
    public boolean cloneVolume(long volumeId, long baseVolId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public VolumeEntity getVolumeEntity(long volumeId) {
        return null;
    }

    private class ManagedCreateBaseImageContext<T> extends AsyncRpcContext<T> {
        private final VolumeInfo _volumeInfo;
        private final PrimaryDataStore _primaryDataStore;
        private final TemplateInfo _templateInfo;
        private final AsyncCallFuture<VolumeApiResult> _future;

        public ManagedCreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volumeInfo, PrimaryDataStore primaryDatastore, TemplateInfo templateInfo,
                                             AsyncCallFuture<VolumeApiResult> future) {
            super(callback);

            _volumeInfo = volumeInfo;
            _primaryDataStore = primaryDatastore;
            _templateInfo = templateInfo;
            _future = future;
        }

        public VolumeInfo getVolumeInfo() {
            return _volumeInfo;
        }

        public PrimaryDataStore getPrimaryDataStore() {
            return _primaryDataStore;
        }

        public TemplateInfo getTemplateInfo() {
            return _templateInfo;
        }

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return _future;
        }
    }

    class CreateBaseImageContext<T> extends AsyncRpcContext<T> {
        private final VolumeInfo volume;
        private final PrimaryDataStore dataStore;
        private final TemplateInfo srcTemplate;
        private final AsyncCallFuture<VolumeApiResult> future;
        final DataObject destObj;
        long templatePoolId;

        public CreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, PrimaryDataStore datastore, TemplateInfo srcTemplate, AsyncCallFuture<VolumeApiResult> future,
                                      DataObject destObj, long templatePoolId) {
            super(callback);
            this.volume = volume;
            this.dataStore = datastore;
            this.future = future;
            this.srcTemplate = srcTemplate;
            this.destObj = destObj;
            this.templatePoolId = templatePoolId;
        }

        public VolumeInfo getVolume() {
            return this.volume;
        }

        public PrimaryDataStore getDataStore() {
            return this.dataStore;
        }

        public TemplateInfo getSrcTemplate() {
            return this.srcTemplate;
        }

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }

        public long getTemplatePoolId() {
            return templatePoolId;
        }

    }

    private TemplateInfo waitForTemplateDownloaded(PrimaryDataStore store, TemplateInfo template) {
        int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        int sleepTime = 120;
        int tries = storagePoolMaxWaitSeconds / sleepTime;
        while (tries > 0) {
            TemplateInfo tmpl = store.getTemplate(template.getId(), null);
            if (tmpl != null) {
                return tmpl;
            }
            try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException e) {
                s_logger.debug("waiting for template download been interrupted: " + e.toString());
            }
            tries--;
        }
        return null;
    }

    @DB
    protected void createBaseImageAsync(VolumeInfo volume, PrimaryDataStore dataStore, TemplateInfo template, AsyncCallFuture<VolumeApiResult> future) {
        String deployAsIsConfiguration = volume.getDeployAsIsConfiguration();
        DataObject templateOnPrimaryStoreObj = dataStore.create(template, deployAsIsConfiguration);

        VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(dataStore.getId(), template.getId(), deployAsIsConfiguration);
        if (templatePoolRef == null) {
            throw new CloudRuntimeException("Failed to find template " + template.getUniqueName() + " in storage pool " + dataStore.getId());
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found template " + template.getUniqueName() + " in storage pool " + dataStore.getId() + " with VMTemplateStoragePool id: " + templatePoolRef.getId());
            }
        }
        long templatePoolRefId = templatePoolRef.getId();
        CreateBaseImageContext<CreateCmdResult> context = new CreateBaseImageContext<CreateCmdResult>(null, volume, dataStore, template, future, templateOnPrimaryStoreObj, templatePoolRefId);
        AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyBaseImageCallback(null, null)).setContext(context);

        int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Acquire lock on VMTemplateStoragePool " + templatePoolRefId + " with timeout " + storagePoolMaxWaitSeconds + " seconds");
        }
        templatePoolRef = _tmpltPoolDao.acquireInLockTable(templatePoolRefId, storagePoolMaxWaitSeconds);

        if (templatePoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.info("Unable to acquire lock on VMTemplateStoragePool " + templatePoolRefId);
            }
            templatePoolRef = _tmpltPoolDao.findByPoolTemplate(dataStore.getId(), template.getId(), deployAsIsConfiguration);
            if (templatePoolRef != null && templatePoolRef.getState() == ObjectInDataStoreStateMachine.State.Ready) {
                s_logger.info(
                        "Unable to acquire lock on VMTemplateStoragePool " + templatePoolRefId + ", But Template " + template.getUniqueName() + " is already copied to primary storage, skip copying");
                createVolumeFromBaseImageAsync(volume, templateOnPrimaryStoreObj, dataStore, future);
                return;
            }
            throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templatePoolRefId);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.info("lock is acquired for VMTemplateStoragePool " + templatePoolRefId);
        }
        try {
            if (templatePoolRef.getState() == ObjectInDataStoreStateMachine.State.Ready) {
                s_logger.info("Template " + template.getUniqueName() + " is already copied to primary storage, skip copying");
                createVolumeFromBaseImageAsync(volume, templateOnPrimaryStoreObj, dataStore, future);
                return;
            }
            templateOnPrimaryStoreObj.processEvent(Event.CreateOnlyRequested);
            motionSrv.copyAsync(template, templateOnPrimaryStoreObj, caller);
        } catch (Throwable e) {
            s_logger.debug("failed to create template on storage", e);
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
            dataStore.create(template, deployAsIsConfiguration);  // make sure that template_spool_ref entry is still present so that the second thread can acquire the lock
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            future.complete(result);
        } finally {
            if (s_logger.isDebugEnabled()) {
                s_logger.info("releasing lock for VMTemplateStoragePool " + templatePoolRefId);
            }
            _tmpltPoolDao.releaseFromLockTable(templatePoolRefId);
        }
        return;
    }

    protected Void managedCopyBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, ManagedCreateBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        VolumeInfo volumeInfo = context.getVolumeInfo();
        VolumeApiResult res = new VolumeApiResult(volumeInfo);

        if (result.isSuccess()) {
            // volumeInfo.processEvent(Event.OperationSuccessed, result.getAnswer());

            VolumeVO volume = volDao.findById(volumeInfo.getId());
            CopyCmdAnswer answer = (CopyCmdAnswer)result.getAnswer();
            TemplateObjectTO templateObjectTo = (TemplateObjectTO)answer.getNewData();

            volume.setPath(templateObjectTo.getPath());

            if (templateObjectTo.getFormat() != null) {
                volume.setFormat(templateObjectTo.getFormat());
            }

            volDao.update(volume.getId(), volume);
        } else {
            volumeInfo.processEvent(Event.DestroyRequested);

            res.setResult(result.getResult());
        }

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();

        future.complete(res);

        return null;
    }

    protected Void createManagedTemplateImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<CreateCmdResult> context) {
        CreateCmdResult result = callback.getResult();
        VolumeApiResult res = new VolumeApiResult(null);

        res.setResult(result.getResult());

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        DataObject templateOnPrimaryStoreObj = context.getVolume();

        if (result.isSuccess()) {
            ((TemplateObject)templateOnPrimaryStoreObj).setInstallPath(result.getPath());
            templateOnPrimaryStoreObj.processEvent(Event.OperationSuccessed, result.getAnswer());
        } else {
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
        }

        future.complete(res);

        return null;
    }

    protected Void copyManagedTemplateCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        VolumeApiResult res = new VolumeApiResult(context.getVolume());

        res.setResult(result.getResult());

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        DataObject templateOnPrimaryStoreObj = context.destObj;

        if (result.isSuccess()) {
            templateOnPrimaryStoreObj.processEvent(Event.OperationSuccessed, result.getAnswer());
        } else {
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
        }

        future.complete(res);

        return null;
    }

    @DB
    protected Void copyBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        VolumeApiResult res = new VolumeApiResult(context.getVolume());

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        DataObject templateOnPrimaryStoreObj = context.destObj;
        if (!result.isSuccess()) {
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
            res.setResult(result.getResult());
            future.complete(res);
            return null;
        }

        templateOnPrimaryStoreObj.processEvent(Event.OperationSuccessed, result.getAnswer());
        createVolumeFromBaseImageAsync(context.volume, templateOnPrimaryStoreObj, context.dataStore, future);
        return null;
    }

    private class CreateVolumeFromBaseImageContext<T> extends AsyncRpcContext<T> {
        private final DataObject vo;
        private final AsyncCallFuture<VolumeApiResult> future;
        private final DataObject templateOnStore;
        private final SnapshotInfo snapshot;
        private final String deployAsIsConfiguration;

        public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T> callback, DataObject vo, DataStore primaryStore, DataObject templateOnStore, AsyncCallFuture<VolumeApiResult> future,
                                                SnapshotInfo snapshot, String configuration) {
            super(callback);
            this.vo = vo;
            this.future = future;
            this.templateOnStore = templateOnStore;
            this.snapshot = snapshot;
            this.deployAsIsConfiguration = configuration;
        }

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }

    @DB
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, DataObject templateOnPrimaryStore, PrimaryDataStore pd, AsyncCallFuture<VolumeApiResult> future) {
        DataObject volumeOnPrimaryStorage = pd.create(volume, volume.getDeployAsIsConfiguration());
        volumeOnPrimaryStorage.processEvent(Event.CreateOnlyRequested);

        CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, volumeOnPrimaryStorage, pd, templateOnPrimaryStore, future, null, volume.getDeployAsIsConfiguration());
        AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeFromBaseImageCallBack(null, null));
        caller.setContext(context);

        motionSrv.copyAsync(context.templateOnStore, volumeOnPrimaryStorage, caller);

        return;
    }

    @DB
    protected Void createVolumeFromBaseImageCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        DataObject vo = context.vo;
        DataObject tmplOnPrimary = context.templateOnStore;
        CopyCommandResult result = callback.getResult();
        VolumeApiResult volResult = new VolumeApiResult((VolumeObject)vo);
        String deployAsIsConfiguration = context.deployAsIsConfiguration;

        if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed, result.getAnswer());
        } else {

            vo.processEvent(Event.OperationFailed);
            volResult.setResult(result.getResult());
            // hack for Vmware: host is down, previously download template to the host needs to be re-downloaded, so we need to reset
            // template_spool_ref entry here to NOT_DOWNLOADED and Allocated state
            Answer ans = result.getAnswer();
            if (ans != null && ans instanceof CopyCmdAnswer && ans.getDetails().contains(REQUEST_TEMPLATE_RELOAD)) {
                if (tmplOnPrimary != null) {
                    s_logger.info("Reset template_spool_ref entry so that vmware template can be reloaded in next try");
                    VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(tmplOnPrimary.getDataStore().getId(), tmplOnPrimary.getId(), deployAsIsConfiguration);
                    if (templatePoolRef != null) {
                        long templatePoolRefId = templatePoolRef.getId();
                        templatePoolRef = _tmpltPoolDao.acquireInLockTable(templatePoolRefId, 1200);
                        try {
                            if (templatePoolRef == null) {
                                s_logger.warn("Reset Template State On Pool failed - unable to lock TemplatePoolRef " + templatePoolRefId);
                            } else {
                                templatePoolRef.setTemplateSize(0);
                                templatePoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED);
                                templatePoolRef.setState(ObjectInDataStoreStateMachine.State.Allocated);

                                _tmpltPoolDao.update(templatePoolRefId, templatePoolRef);
                            }
                        } finally {
                            _tmpltPoolDao.releaseFromLockTable(templatePoolRefId);
                        }
                    }
                }
            }
        }

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        future.complete(volResult);
        return null;
    }

    @DB
    protected Void createVolumeFromBaseManagedImageCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        DataObject vo = context.vo;
        DataObject tmplOnPrimary = context.templateOnStore;
        VolumeApiResult volResult = new VolumeApiResult((VolumeObject)vo);

        if (result.isSuccess()) {
            VolumeVO volume = volDao.findById(vo.getId());
            CopyCmdAnswer answer = (CopyCmdAnswer)result.getAnswer();
            VolumeObjectTO volumeObjectTo = (VolumeObjectTO)answer.getNewData();
            volume.setPath(volumeObjectTo.getPath());
            if (volumeObjectTo.getFormat() != null) {
                volume.setFormat(volumeObjectTo.getFormat());
            }

            volDao.update(volume.getId(), volume);
            vo.processEvent(Event.OperationSuccessed);
        } else {
            volResult.setResult(result.getResult());

            try {
                destroyAndReallocateManagedVolume((VolumeInfo) vo);
            } catch (CloudRuntimeException ex) {
                s_logger.warn("Couldn't destroy managed volume: " + vo.getId());
            }
        }

        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        future.complete(volResult);
        return null;
    }

    /**
     * Creates a template volume on managed storage, which will be used for creating ROOT volumes by cloning.
     *
     * @param srcTemplateInfo Source template on secondary storage
     * @param destPrimaryDataStore Managed storage on which we need to create the volume
     */
    private TemplateInfo createManagedTemplateVolume(TemplateInfo srcTemplateInfo, PrimaryDataStore destPrimaryDataStore) {
        // create a template volume on primary storage
        AsyncCallFuture<VolumeApiResult> createTemplateFuture = new AsyncCallFuture<>();
        TemplateInfo templateOnPrimary = (TemplateInfo)destPrimaryDataStore.create(srcTemplateInfo, srcTemplateInfo.getDeployAsIsConfiguration());

        VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destPrimaryDataStore.getId(), templateOnPrimary.getId(), srcTemplateInfo.getDeployAsIsConfiguration());

        if (templatePoolRef == null) {
            throw new CloudRuntimeException("Failed to find template " + srcTemplateInfo.getUniqueName() + " in storage pool " + destPrimaryDataStore.getId());
        } else if (templatePoolRef.getState() == ObjectInDataStoreStateMachine.State.Ready) {
            // Template already exists
            return templateOnPrimary;
        }

        // At this point, we have an entry in the DB that points to our cached template.
        // We need to lock it as there may be other VMs that may get started using the same template.
        // We want to avoid having to create multiple cache copies of the same template.

        int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        long templatePoolRefId = templatePoolRef.getId();

        templatePoolRef = _tmpltPoolDao.acquireInLockTable(templatePoolRefId, storagePoolMaxWaitSeconds);

        if (templatePoolRef == null) {
            throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templatePoolRefId);
        }

        try {
            // create a cache volume on the back-end

            templateOnPrimary.processEvent(Event.CreateOnlyRequested);

            CreateVolumeContext<CreateCmdResult> createContext = new CreateVolumeContext<>(null, templateOnPrimary, createTemplateFuture);
            AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> createCaller = AsyncCallbackDispatcher.create(this);

            createCaller.setCallback(createCaller.getTarget().createManagedTemplateImageCallback(null, null)).setContext(createContext);

            destPrimaryDataStore.getDriver().createAsync(destPrimaryDataStore, templateOnPrimary, createCaller);

            VolumeApiResult result = createTemplateFuture.get();

            if (result.isFailed()) {
                String errMesg = result.getResult();

                throw new CloudRuntimeException("Unable to create template " + templateOnPrimary.getId() + " on primary storage " + destPrimaryDataStore.getId() + ":" + errMesg);
            }
        } catch (Throwable e) {
            s_logger.debug("Failed to create template volume on storage", e);

            templateOnPrimary.processEvent(Event.OperationFailed);

            throw new CloudRuntimeException(e.getMessage());
        } finally {
            _tmpltPoolDao.releaseFromLockTable(templatePoolRefId);
        }

        return templateOnPrimary;
    }

    /**
     * This function copies a template from secondary storage to a template volume
     * created on managed storage. This template volume will be used as a cache.
     * Instead of copying the template to a ROOT volume every time, a clone is performed instead.
     *
     * @param srcTemplateInfo Source from which to copy the template
     * @param templateOnPrimary Dest to copy to
     * @param templatePoolRef Template reference on primary storage (entry in the template_spool_ref)
     * @param destPrimaryDataStore The managed primary storage
     * @param destHost The host that we will use for the copy
     */
    private void copyTemplateToManagedTemplateVolume(TemplateInfo srcTemplateInfo, TemplateInfo templateOnPrimary, VMTemplateStoragePoolVO templatePoolRef, PrimaryDataStore destPrimaryDataStore,
            Host destHost) throws StorageAccessException {
        AsyncCallFuture<VolumeApiResult> copyTemplateFuture = new AsyncCallFuture<>();
        int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        long templatePoolRefId = templatePoolRef.getId();

        try {
            templatePoolRef = _tmpltPoolDao.acquireInLockTable(templatePoolRefId, storagePoolMaxWaitSeconds);

            if (templatePoolRef == null) {
                throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templatePoolRefId);
            }

            if (templatePoolRef.getDownloadState() == Status.DOWNLOADED) {
                // There can be cases where we acquired the lock, but the template
                // was already copied by a previous thread. Just return in that case.
                s_logger.debug("Template already downloaded, nothing to do");
                return;
            }

            // copy the template from sec storage to the created volume
            CreateBaseImageContext<CreateCmdResult> copyContext = new CreateBaseImageContext<>(null, null, destPrimaryDataStore, srcTemplateInfo, copyTemplateFuture, templateOnPrimary,
                    templatePoolRefId);

            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> copyCaller = AsyncCallbackDispatcher.create(this);
            copyCaller.setCallback(copyCaller.getTarget().copyManagedTemplateCallback(null, null)).setContext(copyContext);

            // Populate details which will be later read by the storage subsystem.
            Map<String, String> details = new HashMap<>();

            details.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
            details.put(PrimaryDataStore.STORAGE_HOST, destPrimaryDataStore.getHostAddress());
            details.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(destPrimaryDataStore.getPort()));
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET, templateOnPrimary.getInstallPath());
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, srcTemplateInfo.getUniqueName());
            details.put(PrimaryDataStore.REMOVE_AFTER_COPY, Boolean.TRUE.toString());
            details.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(templateOnPrimary.getSize()));
            details.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(destPrimaryDataStore.getId())));

            ChapInfo chapInfo = getChapInfo(templateOnPrimary, destPrimaryDataStore);

            if (chapInfo != null) {
                details.put(PrimaryDataStore.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
                details.put(PrimaryDataStore.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
                details.put(PrimaryDataStore.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
                details.put(PrimaryDataStore.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
            }

            destPrimaryDataStore.setDetails(details);

            try {
                grantAccess(templateOnPrimary, destHost, destPrimaryDataStore);
            } catch (Exception e) {
                throw new StorageAccessException("Unable to grant access to template: " + templateOnPrimary.getId() + " on host: " + destHost.getId());
            }

            templateOnPrimary.processEvent(Event.CopyingRequested);

            VolumeApiResult result;

            try {
                motionSrv.copyAsync(srcTemplateInfo, templateOnPrimary, destHost, copyCaller);

                result = copyTemplateFuture.get();
            } finally {
                revokeAccess(templateOnPrimary, destHost, destPrimaryDataStore);

                if (HypervisorType.VMware.equals(destHost.getHypervisorType())) {
                    details.put(ModifyTargetsCommand.IQN, templateOnPrimary.getInstallPath());

                    List<Map<String, String>> targets = new ArrayList<>();

                    targets.add(details);

                    removeDynamicTargets(destHost.getId(), targets);
                }
            }

            if (result.isFailed()) {
                throw new CloudRuntimeException("Failed to copy template " + templateOnPrimary.getId() + " to primary storage " + destPrimaryDataStore.getId() + ": " + result.getResult());
                // XXX: I find it is useful to destroy the volume on primary storage instead of another thread trying the copy again because I've seen
                // something weird happens to the volume (XenServer creates an SR, but the VDI copy can fail).
                // For now, I just retry the copy.
            }
        } catch (StorageAccessException e) {
            throw e;
        } catch (Throwable e) {
            s_logger.debug("Failed to create a template on primary storage", e);

            templateOnPrimary.processEvent(Event.OperationFailed);

            throw new CloudRuntimeException(e.getMessage());
        } finally {
            _tmpltPoolDao.releaseFromLockTable(templatePoolRefId);
        }
    }

    private void removeDynamicTargets(long hostId, List<Map<String, String>> targets) {
        ModifyTargetsCommand cmd = new ModifyTargetsCommand();

        cmd.setTargets(targets);
        cmd.setApplyToAllHostsInCluster(true);
        cmd.setAdd(false);
        cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);

        sendModifyTargetsCommand(cmd, hostId);
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            String msg = "Unable to get an answer to the modify targets command";

            s_logger.warn(msg);
        } else if (!answer.getResult()) {
            String msg = "Unable to modify target on the following host: " + hostId;

            s_logger.warn(msg);
        }
    }

    /**
     * Clones the template volume on managed storage to the ROOT volume
     *
     * @param volumeInfo ROOT volume to create
     * @param templateOnPrimary Template from which to clone the ROOT volume
     * @param destPrimaryDataStore Primary storage of the volume
     * @param future For async
     */
    private void createManagedVolumeCloneTemplateAsync(VolumeInfo volumeInfo, TemplateInfo templateOnPrimary, PrimaryDataStore destPrimaryDataStore, AsyncCallFuture<VolumeApiResult> future) {
        VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destPrimaryDataStore.getId(), templateOnPrimary.getId(), volumeInfo.getDeployAsIsConfiguration());

        if (templatePoolRef == null) {
            throw new CloudRuntimeException("Failed to find template " + templateOnPrimary.getUniqueName() + " in storage pool " + destPrimaryDataStore.getId());
        }

        //XXX: not sure if this the right thing to do here. We can always fallback to the "copy from sec storage"
        if (templatePoolRef.getDownloadState() == Status.NOT_DOWNLOADED) {
            throw new CloudRuntimeException("Template " + templateOnPrimary.getUniqueName() + " has not been downloaded to primary storage.");
        }

        try {
            volumeInfo.processEvent(Event.CreateOnlyRequested);

            CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<>(null, volumeInfo, destPrimaryDataStore, templateOnPrimary, future, null, volumeInfo.getDeployAsIsConfiguration());

            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);

            caller.setCallback(caller.getTarget().createVolumeFromBaseImageCallBack(null, null));
            caller.setContext(context);

            motionSrv.copyAsync(templateOnPrimary, volumeInfo, caller);
        } catch (Throwable e) {
            s_logger.debug("Failed to clone template on primary storage", e);

            volumeInfo.processEvent(Event.OperationFailed);

            throw new CloudRuntimeException(e.getMessage());
        }
    }

    private void createManagedVolumeCopyManagedTemplateAsync(VolumeInfo volumeInfo, PrimaryDataStore destPrimaryDataStore, TemplateInfo srcTemplateOnPrimary, Host destHost, AsyncCallFuture<VolumeApiResult> future) throws StorageAccessException {
        VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destPrimaryDataStore.getId(), srcTemplateOnPrimary.getId(), null);

        if (templatePoolRef == null) {
            throw new CloudRuntimeException("Failed to find template " + srcTemplateOnPrimary.getUniqueName() + " in storage pool " + srcTemplateOnPrimary.getId());
        }

        if (templatePoolRef.getDownloadState() == Status.NOT_DOWNLOADED) {
            throw new CloudRuntimeException("Template " + srcTemplateOnPrimary.getUniqueName() + " has not been downloaded to primary storage.");
        }

        String volumeDetailKey = "POOL_TEMPLATE_ID_COPY_ON_HOST_" + destHost.getId();

        try {
            try {
                grantAccess(srcTemplateOnPrimary, destHost, destPrimaryDataStore);
            } catch (Exception e) {
                throw new StorageAccessException("Unable to grant access to src template: " + srcTemplateOnPrimary.getId() + " on host: " + destHost.getId());
            }

            _volumeDetailsDao.addDetail(volumeInfo.getId(), volumeDetailKey, String.valueOf(templatePoolRef.getId()), false);

            // Create a volume on managed storage.
            AsyncCallFuture<VolumeApiResult> createVolumeFuture = createVolumeAsync(volumeInfo, destPrimaryDataStore);
            VolumeApiResult createVolumeResult = createVolumeFuture.get();

            if (createVolumeResult.isFailed()) {
                throw new CloudRuntimeException("Creation of a volume failed: " + createVolumeResult.getResult());
            }

            // Refresh the volume info from the DB.
            volumeInfo = volFactory.getVolume(volumeInfo.getId(), destPrimaryDataStore);

            Map<String, String> details = new HashMap<String, String>();
            details.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
            details.put(PrimaryDataStore.STORAGE_HOST, destPrimaryDataStore.getHostAddress());
            details.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(destPrimaryDataStore.getPort()));
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET, volumeInfo.get_iScsiName());
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, volumeInfo.getName());
            details.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(volumeInfo.getSize()));
            details.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(destPrimaryDataStore.getId())));
            destPrimaryDataStore.setDetails(details);

            grantAccess(volumeInfo, destHost, destPrimaryDataStore);

            volumeInfo.processEvent(Event.CreateRequested);

            CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<>(null, volumeInfo, destPrimaryDataStore, srcTemplateOnPrimary, future, null, null);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createVolumeFromBaseManagedImageCallBack(null, null));
            caller.setContext(context);

            try {
                motionSrv.copyAsync(srcTemplateOnPrimary, volumeInfo, destHost, caller);
            } finally {
                revokeAccess(volumeInfo, destHost, destPrimaryDataStore);
            }
        } catch (StorageAccessException e) {
            throw e;
        } catch (Throwable e) {
            s_logger.debug("Failed to copy managed template on primary storage", e);
            String errMsg = "Failed due to " + e.toString();

            try {
                destroyAndReallocateManagedVolume(volumeInfo);
            } catch (CloudRuntimeException ex) {
                s_logger.warn("Failed to destroy managed volume: " + volumeInfo.getId());
                errMsg += " : " + ex.getMessage();
            }

            VolumeApiResult result = new VolumeApiResult(volumeInfo);
            result.setResult(errMsg);
            future.complete(result);
        } finally {
            _volumeDetailsDao.removeDetail(volumeInfo.getId(), volumeDetailKey);

            List<VolumeDetailVO> volumeDetails = _volumeDetailsDao.findDetails(volumeDetailKey, String.valueOf(templatePoolRef.getId()), false);
            if (volumeDetails == null || volumeDetails.isEmpty()) {
                revokeAccess(srcTemplateOnPrimary, destHost, destPrimaryDataStore);
            }
        }
    }

    private void destroyAndReallocateManagedVolume(VolumeInfo volumeInfo) {
        if (volumeInfo == null) {
            return;
        }

        VolumeVO volume = volDao.findById(volumeInfo.getId());
        if (volume == null) {
            return;
        }

        if (volume.getState() == State.Allocated) { // Possible states here: Allocated, Ready & Creating
            return;
        }

        volumeInfo.processEvent(Event.DestroyRequested);

        Volume newVol = _volumeMgr.allocateDuplicateVolume(volume, null);
        VolumeVO newVolume = (VolumeVO) newVol;
        newVolume.set_iScsiName(null);
        volDao.update(newVolume.getId(), newVolume);
        s_logger.debug("Allocated new volume: " + newVolume.getId() + " for the VM: " + volume.getInstanceId());

        try {
            AsyncCallFuture<VolumeApiResult> expungeVolumeFuture = expungeVolumeAsync(volumeInfo);
            VolumeApiResult expungeVolumeResult = expungeVolumeFuture.get();
            if (expungeVolumeResult.isFailed()) {
                s_logger.warn("Failed to expunge volume: " + volumeInfo.getId() + " that was created");
                throw new CloudRuntimeException("Failed to expunge volume: " + volumeInfo.getId() + " that was created");
            }
        } catch (Exception ex) {
            if (canVolumeBeRemoved(volumeInfo.getId())) {
                volDao.remove(volumeInfo.getId());
            }
            s_logger.warn("Unable to expunge volume: " + volumeInfo.getId() + " due to: " + ex.getMessage());
            throw new CloudRuntimeException("Unable to expunge volume: " + volumeInfo.getId() + " due to: " + ex.getMessage());
        }
    }

    private void createManagedVolumeCopyTemplateAsync(VolumeInfo volumeInfo, PrimaryDataStore primaryDataStore, TemplateInfo srcTemplateInfo, Host destHost, AsyncCallFuture<VolumeApiResult> future) {
        try {
            // Create a volume on managed storage.

            TemplateInfo destTemplateInfo = (TemplateInfo)primaryDataStore.create(srcTemplateInfo, false, volumeInfo.getDeployAsIsConfiguration());

            AsyncCallFuture<VolumeApiResult> createVolumeFuture = createVolumeAsync(volumeInfo, primaryDataStore);
            VolumeApiResult createVolumeResult = createVolumeFuture.get();

            if (createVolumeResult.isFailed()) {
                throw new CloudRuntimeException("Creation of a volume failed: " + createVolumeResult.getResult());
            }

            // Refresh the volume info from the DB.
            volumeInfo = volFactory.getVolume(volumeInfo.getId(), primaryDataStore);

            ManagedCreateBaseImageContext<CreateCmdResult> context = new ManagedCreateBaseImageContext<CreateCmdResult>(null, volumeInfo, primaryDataStore, srcTemplateInfo, future);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);

            caller.setCallback(caller.getTarget().managedCopyBaseImageCallback(null, null)).setContext(context);

            Map<String, String> details = new HashMap<String, String>();

            details.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
            details.put(PrimaryDataStore.STORAGE_HOST, primaryDataStore.getHostAddress());
            details.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(primaryDataStore.getPort()));
            // for managed storage, the storage repository (XenServer) or datastore (ESX) name is based off of the iScsiName property of a volume
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET, volumeInfo.get_iScsiName());
            details.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, volumeInfo.getName());
            details.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(volumeInfo.getSize()));
            details.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(primaryDataStore.getId())));

            ChapInfo chapInfo = getChapInfo(volumeInfo, primaryDataStore);

            if (chapInfo != null) {
                details.put(PrimaryDataStore.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
                details.put(PrimaryDataStore.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
                details.put(PrimaryDataStore.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
                details.put(PrimaryDataStore.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
            }

            primaryDataStore.setDetails(details);

            grantAccess(volumeInfo, destHost, primaryDataStore);

            try {
                motionSrv.copyAsync(srcTemplateInfo, destTemplateInfo, destHost, caller);
            } finally {
                revokeAccess(volumeInfo, destHost, primaryDataStore);
            }
        } catch (Throwable t) {
            String errMsg = t.toString();

            volumeInfo.processEvent(Event.DestroyRequested);

            try {
                AsyncCallFuture<VolumeApiResult> expungeVolumeFuture = expungeVolumeAsync(volumeInfo);

                VolumeApiResult expungeVolumeResult = expungeVolumeFuture.get();

                if (expungeVolumeResult.isFailed()) {
                    errMsg += " : Failed to expunge a volume that was created";
                }
            } catch (Exception ex) {
                errMsg += " : " + ex.getMessage();
            }

            VolumeApiResult result = new VolumeApiResult(volumeInfo);

            result.setResult(errMsg);

            future.complete(result);
        }
    }

    @Override
    public TemplateInfo createManagedStorageTemplate(long srcTemplateId, long destDataStoreId, long destHostId) throws StorageAccessException {
        Host destHost = _hostDao.findById(destHostId);
        if (destHost == null) {
            throw new CloudRuntimeException("Destination host should not be null.");
        }

        TemplateInfo srcTemplateInfo = tmplFactory.getTemplate(srcTemplateId);
        if (srcTemplateInfo == null) {
            throw new CloudRuntimeException("Failed to get info of template: " + srcTemplateId);
        }

        if (Storage.ImageFormat.ISO.equals(srcTemplateInfo.getFormat())) {
            throw new CloudRuntimeException("Unsupported format: " + Storage.ImageFormat.ISO.toString() + " for managed storage template");
        }

        GlobalLock lock = null;
        TemplateInfo templateOnPrimary = null;
        try {
            String templateIdManagedPoolIdLockString = "templateId:" + srcTemplateId + "managedPoolId:" + destDataStoreId;
            lock = GlobalLock.getInternLock(templateIdManagedPoolIdLockString);
            if (lock == null) {
                throw new CloudRuntimeException("Unable to create managed storage template, couldn't get global lock on " + templateIdManagedPoolIdLockString);
            }

            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
            if (!lock.lock(storagePoolMaxWaitSeconds)) {
                s_logger.debug("Unable to create managed storage template, couldn't lock on " + templateIdManagedPoolIdLockString);
                throw new CloudRuntimeException("Unable to create managed storage template, couldn't lock on " + templateIdManagedPoolIdLockString);
            }

            PrimaryDataStore destPrimaryDataStore = dataStoreMgr.getPrimaryDataStore(destDataStoreId);

            // Check if template exists on the storage pool. If not, downland and copy to managed storage pool
            VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destDataStoreId, srcTemplateId, null);
            if (templatePoolRef != null && templatePoolRef.getDownloadState() == Status.DOWNLOADED) {
                return tmplFactory.getTemplate(srcTemplateId, destPrimaryDataStore);
            }

            templateOnPrimary = createManagedTemplateVolume(srcTemplateInfo, destPrimaryDataStore);
            if (templateOnPrimary == null) {
                throw new CloudRuntimeException("Failed to create template " + srcTemplateInfo.getUniqueName() + " on primary storage: " + destDataStoreId);
            }

            templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destPrimaryDataStore.getId(), templateOnPrimary.getId(), null);
            if (templatePoolRef == null) {
                throw new CloudRuntimeException("Failed to find template " + srcTemplateInfo.getUniqueName() + " in storage pool " + destPrimaryDataStore.getId());
            }

            if (templatePoolRef.getDownloadState() == Status.NOT_DOWNLOADED) {
                // Populate details which will be later read by the storage subsystem.
                Map<String, String> details = new HashMap<>();

                details.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
                details.put(PrimaryDataStore.STORAGE_HOST, destPrimaryDataStore.getHostAddress());
                details.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(destPrimaryDataStore.getPort()));
                details.put(PrimaryDataStore.MANAGED_STORE_TARGET, templateOnPrimary.getInstallPath());
                details.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, srcTemplateInfo.getUniqueName());
                details.put(PrimaryDataStore.REMOVE_AFTER_COPY, Boolean.TRUE.toString());
                details.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(templateOnPrimary.getSize()));
                details.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(destPrimaryDataStore.getId())));
                destPrimaryDataStore.setDetails(details);

                try {
                    grantAccess(templateOnPrimary, destHost, destPrimaryDataStore);
                } catch (Exception e) {
                    throw new StorageAccessException("Unable to grant access to template: " + templateOnPrimary.getId() + " on host: " + destHost.getId());
                }

                templateOnPrimary.processEvent(Event.CopyingRequested);

                try {
                    //Download and copy template to the managed volume
                    TemplateInfo templateOnPrimaryNow =  tmplFactory.getReadyBypassedTemplateOnManagedStorage(srcTemplateId, templateOnPrimary, destDataStoreId, destHostId);
                    if (templateOnPrimaryNow == null) {
                        s_logger.debug("Failed to prepare ready bypassed template: " + srcTemplateId + " on primary storage: " + templateOnPrimary.getId());
                        throw new CloudRuntimeException("Failed to prepare ready bypassed template: " + srcTemplateId + " on primary storage: " + templateOnPrimary.getId());
                    }
                    templateOnPrimary.processEvent(Event.OperationSuccessed);
                    return templateOnPrimaryNow;
                } finally {
                    revokeAccess(templateOnPrimary, destHost, destPrimaryDataStore);
                }
            }
            return null;
        } catch (StorageAccessException e) {
            throw e;
        } catch (Throwable e) {
            s_logger.debug("Failed to create template on managed primary storage", e);
            if (templateOnPrimary != null) {
                templateOnPrimary.processEvent(Event.OperationFailed);
            }

            throw new CloudRuntimeException(e.getMessage());
        } finally {
            if (lock != null) {
                lock.unlock();
                lock.releaseRef();
            }
        }
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> createManagedStorageVolumeFromTemplateAsync(VolumeInfo volumeInfo, long destDataStoreId, TemplateInfo srcTemplateInfo, long destHostId) throws StorageAccessException {
        PrimaryDataStore destPrimaryDataStore = dataStoreMgr.getPrimaryDataStore(destDataStoreId);
        Host destHost = _hostDao.findById(destHostId);

        if (destHost == null) {
            throw new CloudRuntimeException("Destination host should not be null.");
        }

        Boolean storageCanCloneVolume = new Boolean(destPrimaryDataStore.getDriver().getCapabilities().get(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString()));

        boolean computeSupportsVolumeClone = computeSupportsVolumeClone(destHost.getDataCenterId(), destHost.getHypervisorType());

        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<>();

        if (storageCanCloneVolume && computeSupportsVolumeClone) {
            s_logger.debug("Storage " + destDataStoreId + " can support cloning using a cached template and compute side is OK with volume cloning.");

            GlobalLock lock = null;
            TemplateInfo templateOnPrimary = null;

            try {
                String tmplIdManagedPoolIdLockString = "tmplId:" + srcTemplateInfo.getId() + "managedPoolId:" + destDataStoreId;
                lock = GlobalLock.getInternLock(tmplIdManagedPoolIdLockString);
                if (lock == null) {
                    throw new CloudRuntimeException("Unable to create managed storage template/volume, couldn't get global lock on " + tmplIdManagedPoolIdLockString);
                }

                int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
                if (!lock.lock(storagePoolMaxWaitSeconds)) {
                    s_logger.debug("Unable to create managed storage template/volume, couldn't lock on " + tmplIdManagedPoolIdLockString);
                    throw new CloudRuntimeException("Unable to create managed storage template/volume, couldn't lock on " + tmplIdManagedPoolIdLockString);
                }

                templateOnPrimary = destPrimaryDataStore.getTemplate(srcTemplateInfo.getId(), null);

                if (templateOnPrimary == null) {
                    templateOnPrimary = createManagedTemplateVolume(srcTemplateInfo, destPrimaryDataStore);

                    if (templateOnPrimary == null) {
                        throw new CloudRuntimeException("Failed to create template " + srcTemplateInfo.getUniqueName() + " on primary storage: " + destDataStoreId);
                    }
                }

                // Copy the template to the template volume.
                VMTemplateStoragePoolVO templatePoolRef = _tmpltPoolDao.findByPoolTemplate(destPrimaryDataStore.getId(), templateOnPrimary.getId(), null);

                if (templatePoolRef == null) {
                    throw new CloudRuntimeException("Failed to find template " + srcTemplateInfo.getUniqueName() + " in storage pool " + destPrimaryDataStore.getId());
                }

                if (templatePoolRef.getDownloadState() == Status.NOT_DOWNLOADED) {
                    copyTemplateToManagedTemplateVolume(srcTemplateInfo, templateOnPrimary, templatePoolRef, destPrimaryDataStore, destHost);
                }
            } finally {
                if (lock != null) {
                    lock.unlock();
                    lock.releaseRef();
                }
            }

            if (destPrimaryDataStore.getPoolType() != StoragePoolType.PowerFlex) {
                // We have a template on primary storage. Clone it to new volume.
                s_logger.debug("Creating a clone from template on primary storage " + destDataStoreId);

                createManagedVolumeCloneTemplateAsync(volumeInfo, templateOnPrimary, destPrimaryDataStore, future);
            } else {
                // We have a template on PowerFlex primary storage. Create new volume and copy to it.
                s_logger.debug("Copying the template to the volume on primary storage");
                createManagedVolumeCopyManagedTemplateAsync(volumeInfo, destPrimaryDataStore, templateOnPrimary, destHost, future);
            }
        } else {
            s_logger.debug("Primary storage does not support cloning or no support for UUID resigning on the host side; copying the template normally");

            createManagedVolumeCopyTemplateAsync(volumeInfo, destPrimaryDataStore, srcTemplateInfo, destHost, future);
        }

        return future;
    }

    private boolean computeSupportsVolumeClone(long zoneId, HypervisorType hypervisorType) {
        if (HypervisorType.VMware.equals(hypervisorType) || HypervisorType.KVM.equals(hypervisorType)) {
            return true;
        }

        return getHost(zoneId, hypervisorType, true) != null;
    }

    private HostVO getHost(Long zoneId, HypervisorType hypervisorType, boolean computeClusterMustSupportResign) {
        if (zoneId == null) {
            throw new CloudRuntimeException("Zone ID cannot be null.");
        }

        List<? extends Cluster> clusters = mgr.searchForClusters(zoneId, new Long(0), Long.MAX_VALUE, hypervisorType.toString());

        if (clusters == null) {
            clusters = new ArrayList<>();
        }

        Collections.shuffle(clusters, new Random(System.nanoTime()));

        clusters: for (Cluster cluster : clusters) {
            if (cluster.getAllocationState() == AllocationState.Enabled) {
                List<HostVO> hosts = _hostDao.findByClusterId(cluster.getId());

                if (hosts != null) {
                    Collections.shuffle(hosts, new Random(System.nanoTime()));

                    for (HostVO host : hosts) {
                        if (host.getResourceState() == ResourceState.Enabled) {
                            if (computeClusterMustSupportResign) {
                                if (clusterDao.getSupportsResigning(cluster.getId())) {
                                    return host;
                                } else {
                                    // no other host in the cluster in question should be able to satisfy our requirements here, so move on to the next cluster
                                    continue clusters;
                                }
                            } else {
                                return host;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @DB
    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, TemplateInfo template) {
        PrimaryDataStore pd = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        TemplateInfo templateOnPrimaryStore = pd.getTemplate(template.getId(), volume.getDeployAsIsConfiguration());
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<>();

        if (templateOnPrimaryStore == null) {
            createBaseImageAsync(volume, pd, template, future);
            return future;
        }

        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStore, pd, future);
        return future;
    }

    @DB
    @Override
    public void destroyVolume(long volumeId) {
        // mark volume entry in volumes table as destroy state
        VolumeInfo vol = volFactory.getVolume(volumeId);
        vol.stateTransit(Volume.Event.DestroyRequested);
        snapshotMgr.deletePoliciesForVolume(volumeId);

        vol.stateTransit(Volume.Event.OperationSucceeded);

        if (vol.getAttachedVM() == null || vol.getAttachedVM().getType() == VirtualMachine.Type.User) {
            // Decrement the resource count for volumes and primary storage belonging user VM's only
            _resourceLimitMgr.decrementResourceCount(vol.getAccountId(), ResourceType.volume, vol.isDisplay());
            _resourceLimitMgr.decrementResourceCount(vol.getAccountId(), ResourceType.primary_storage, vol.isDisplay(), new Long(vol.getSize()));
        }
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeFromSnapshot(VolumeInfo volume, DataStore store, SnapshotInfo snapshot) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();

        try {
            DataObject volumeOnStore = store.create(volume);
            volumeOnStore.processEvent(Event.CreateOnlyRequested);
            _volumeDetailsDao.addDetail(volume.getId(), SNAPSHOT_ID, Long.toString(snapshot.getId()), false);

            CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, volume, store, volumeOnStore, future, snapshot, null);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createVolumeFromSnapshotCallback(null, null)).setContext(context);
            motionSrv.copyAsync(snapshot, volumeOnStore, caller);
        } catch (Exception e) {
            s_logger.debug("create volume from snapshot failed", e);
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            future.complete(result);
        }

        return future;
    }

    protected Void createVolumeFromSnapshotCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        VolumeInfo volume = (VolumeInfo)context.templateOnStore;
        SnapshotInfo snapshot = context.snapshot;
        VolumeApiResult apiResult = new VolumeApiResult(volume);
        Event event = null;
        if (result.isFailed()) {
            apiResult.setResult(result.getResult());
            event = Event.OperationFailed;
        } else {
            event = Event.OperationSuccessed;
        }

        try {
            if (result.isSuccess()) {
                volume.processEvent(event, result.getAnswer());
            } else {
                volume.processEvent(event);
            }
            _volumeDetailsDao.removeDetail(volume.getId(), SNAPSHOT_ID);

        } catch (Exception e) {
            s_logger.debug("create volume from snapshot failed", e);
            apiResult.setResult(e.toString());
        }

        AsyncCallFuture<VolumeApiResult> future = context.future;
        future.complete(apiResult);
        return null;
    }

    protected VolumeVO duplicateVolumeOnAnotherStorage(Volume volume, StoragePool pool) {
        Long lastPoolId = volume.getPoolId();
        String folder = pool.getPath();
        // For SMB, pool credentials are also stored in the uri query string.  We trim the query string
        // part  here to make sure the credentials do not get stored in the db unencrypted.
        if (pool.getPoolType() == StoragePoolType.SMB && folder != null && folder.contains("?")) {
            folder = folder.substring(0, folder.indexOf("?"));
        } else  if (pool.getPoolType() == StoragePoolType.PowerFlex) {
            folder = volume.getFolder();
        }

        VolumeVO newVol = new VolumeVO(volume);
        newVol.setInstanceId(null);
        newVol.setChainInfo(null);
        newVol.setPath(null);
        newVol.setFolder(folder);
        newVol.setPodId(pool.getPodId());
        newVol.setPoolId(pool.getId());
        newVol.setPoolType(pool.getPoolType());
        newVol.setLastPoolId(lastPoolId);
        newVol.setPodId(pool.getPodId());
        return volDao.persist(newVol);
    }

    private class CopyVolumeContext<T> extends AsyncRpcContext<T> {
        final VolumeInfo srcVolume;
        final VolumeInfo destVolume;
        final AsyncCallFuture<VolumeApiResult> future;

        public CopyVolumeContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<VolumeApiResult> future, VolumeInfo srcVolume, VolumeInfo destVolume, DataStore destStore) {
            super(callback);
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.future = future;
        }
    }

    protected AsyncCallFuture<VolumeApiResult> copyVolumeFromImageToPrimary(VolumeInfo srcVolume, DataStore destStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        VolumeInfo destVolume = null;
        try {
            destVolume = (VolumeInfo)destStore.create(srcVolume);
            destVolume.processEvent(Event.CopyingRequested);
            srcVolume.processEvent(Event.CopyingRequested);

            CopyVolumeContext<VolumeApiResult> context = new CopyVolumeContext<VolumeApiResult>(null, future, srcVolume, destVolume, destStore);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyVolumeFromImageToPrimaryCallback(null, null)).setContext(context);

            motionSrv.copyAsync(srcVolume, destVolume, caller);
            return future;
        } catch (Exception e) {
            s_logger.error("failed to copy volume from image store", e);
            if (destVolume != null) {
                destVolume.processEvent(Event.OperationFailed);
            }

            srcVolume.processEvent(Event.OperationFailed);
            res.setResult(e.toString());
            future.complete(res);
            return future;
        }
    }

    protected Void copyVolumeFromImageToPrimaryCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CopyVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        VolumeInfo destVolume = context.destVolume;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(destVolume);
        try {
            if (result.isFailed()) {
                destVolume.processEvent(Event.OperationFailed);
                srcVolume.processEvent(Event.OperationFailed);
                res.setResult(result.getResult());
                future.complete(res);
                return null;
            }

            srcVolume.processEvent(Event.OperationSuccessed);
            destVolume.processEvent(Event.OperationSuccessed, result.getAnswer());
            srcVolume.getDataStore().delete(srcVolume);
            future.complete(res);
        } catch (Exception e) {
            res.setResult(e.toString());
            future.complete(res);
        }
        return null;
    }

    protected AsyncCallFuture<VolumeApiResult> copyVolumeFromPrimaryToImage(VolumeInfo srcVolume, DataStore destStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        VolumeInfo destVolume = null;
        try {
            destVolume = (VolumeInfo)destStore.create(srcVolume);
            srcVolume.processEvent(Event.MigrationRequested);    // this is just used for locking that src volume record in DB to avoid using lock
            destVolume.processEventOnly(Event.CreateOnlyRequested);

            CopyVolumeContext<VolumeApiResult> context = new CopyVolumeContext<VolumeApiResult>(null, future, srcVolume, destVolume, destStore);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyVolumeFromPrimaryToImageCallback(null, null)).setContext(context);

            motionSrv.copyAsync(srcVolume, destVolume, caller);
            return future;
        } catch (Exception e) {
            s_logger.error("failed to copy volume to image store", e);
            if (destVolume != null) {
                destVolume.getDataStore().delete(destVolume);
            }
            srcVolume.processEvent(Event.OperationFailed); // unlock source volume record
            res.setResult(e.toString());
            future.complete(res);
            return future;
        }
    }

    protected Void copyVolumeFromPrimaryToImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CopyVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        VolumeInfo destVolume = context.destVolume;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(destVolume);
        try {
            if (result.isFailed()) {
                srcVolume.processEvent(Event.OperationFailed); // back to Ready state in Volume table
                destVolume.processEventOnly(Event.OperationFailed);
                res.setResult(result.getResult());
                future.complete(res);
            } else {
                srcVolume.processEvent(Event.OperationSuccessed); // back to Ready state in Volume table
                destVolume.processEventOnly(Event.OperationSuccessed, result.getAnswer());
                future.complete(res);
            }
        } catch (Exception e) {
            res.setResult(e.toString());
            future.complete(res);
        }
        return null;
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> copyVolume(VolumeInfo srcVolume, DataStore destStore) {
        DataStore srcStore = srcVolume.getDataStore();
        if (s_logger.isDebugEnabled()) {
            String srcRole = (srcStore != null && srcStore.getRole() != null ? srcVolume.getDataStore().getRole().toString() : "<unknown role>");

            String msg = String.format("copying %s(id=%d, role=%s) to %s (id=%d, role=%s)"
                    , srcVolume.getName()
                    , srcVolume.getId()
                    , srcRole
                    , destStore.getName()
                    , destStore.getId()
                    , destStore.getRole());
            s_logger.debug(msg);
        }

        if (srcVolume.getState() == Volume.State.Uploaded) {
            return copyVolumeFromImageToPrimary(srcVolume, destStore);
        }

        if (destStore.getRole() == DataStoreRole.Image) {
            return copyVolumeFromPrimaryToImage(srcVolume, destStore);
        }

        if (srcStore.getRole() == DataStoreRole.Primary && destStore.getRole() == DataStoreRole.Primary && ((PrimaryDataStore) destStore).isManaged() &&
                requiresNewManagedVolumeInDestStore((PrimaryDataStore) srcStore, (PrimaryDataStore) destStore)) {
            return copyManagedVolume(srcVolume, destStore);
        }

        // OfflineVmwareMigration: aren't we missing secondary to secondary in this logic?

        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        try {
            if (!snapshotMgr.canOperateOnVolume(srcVolume)) {
                s_logger.debug("There are snapshots creating on this volume, can not move this volume");

                res.setResult("There are snapshots creating on this volume, can not move this volume");
                future.complete(res);
                return future;
            }

            VolumeVO destVol = duplicateVolumeOnAnotherStorage(srcVolume, (StoragePool)destStore);
            VolumeInfo destVolume = volFactory.getVolume(destVol.getId(), destStore);
            destVolume.processEvent(Event.MigrationCopyRequested);
            srcVolume.processEvent(Event.MigrationRequested);

            CopyVolumeContext<VolumeApiResult> context = new CopyVolumeContext<VolumeApiResult>(null, future, srcVolume, destVolume, destStore);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyVolumeCallBack(null, null)).setContext(context);
            motionSrv.copyAsync(srcVolume, destVolume, caller);
        } catch (Exception e) {
            s_logger.error("Failed to copy volume:" + e);
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to copy volume.", e);
            }
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }

    protected Void copyVolumeCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CopyVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        VolumeInfo destVolume = context.destVolume;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(destVolume);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destVolume.processEvent(Event.MigrationCopyFailed);
                srcVolume.processEvent(Event.OperationFailed);
                destroyVolume(destVolume.getId());
                if (destVolume.getStoragePoolType() == StoragePoolType.PowerFlex) {
                    s_logger.info("Dest volume " + destVolume.getId() + " can be removed");
                    destVolume.processEvent(Event.ExpungeRequested);
                    destVolume.processEvent(Event.OperationSuccessed);
                    volDao.remove(destVolume.getId());
                    future.complete(res);
                    return null;
                }
                destVolume = volFactory.getVolume(destVolume.getId());
                AsyncCallFuture<VolumeApiResult> destroyFuture = expungeVolumeAsync(destVolume);
                destroyFuture.get();
                future.complete(res);
            } else {
                srcVolume.processEvent(Event.OperationSuccessed);
                destVolume.processEvent(Event.MigrationCopySucceeded, result.getAnswer());
                volDao.updateUuid(srcVolume.getId(), destVolume.getId());
                try {
                    destroyVolume(srcVolume.getId());
                    if (srcVolume.getStoragePoolType() == StoragePoolType.PowerFlex) {
                        s_logger.info("Src volume " + srcVolume.getId() + " can be removed");
                        srcVolume.processEvent(Event.ExpungeRequested);
                        srcVolume.processEvent(Event.OperationSuccessed);
                        volDao.remove(srcVolume.getId());
                        future.complete(res);
                        return null;
                    }
                    srcVolume = volFactory.getVolume(srcVolume.getId());
                    AsyncCallFuture<VolumeApiResult> destroyFuture = expungeVolumeAsync(srcVolume);
                    // If volume destroy fails, this could be because of vdi is still in use state, so wait and retry.
                    if (destroyFuture.get().isFailed()) {
                        Thread.sleep(5 * 1000);
                        destroyFuture = expungeVolumeAsync(srcVolume);
                        destroyFuture.get();
                    }
                    future.complete(res);
                } catch (Exception e) {
                    s_logger.debug("failed to clean up volume on storage", e);
                }
            }
        } catch (Exception e) {
            s_logger.debug("Failed to process copy volume callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    private class CopyManagedVolumeContext<T> extends AsyncRpcContext<T> {
        final VolumeInfo srcVolume;
        final VolumeInfo destVolume;
        final Host host;
        final AsyncCallFuture<VolumeApiResult> future;

        public CopyManagedVolumeContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<VolumeApiResult> future, VolumeInfo srcVolume, VolumeInfo destVolume, Host host) {
            super(callback);
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.host = host;
            this.future = future;
        }
    }

    private AsyncCallFuture<VolumeApiResult> copyManagedVolume(VolumeInfo srcVolume, DataStore destStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        try {
            if (!snapshotMgr.canOperateOnVolume(srcVolume)) {
                s_logger.debug("There are snapshots creating for this volume, can not move this volume");
                res.setResult("There are snapshots creating for this volume, can not move this volume");
                future.complete(res);
                return future;
            }

            if (snapshotMgr.backedUpSnapshotsExistsForVolume(srcVolume)) {
                s_logger.debug("There are backed up snapshots for this volume, can not move.");
                res.setResult("[UNSUPPORTED] There are backed up snapshots for this volume, can not move. Please try again after removing them.");
                future.complete(res);
                return future;
            }

            List<Long> poolIds = new ArrayList<Long>();
            poolIds.add(srcVolume.getPoolId());
            poolIds.add(destStore.getId());

            Host hostWithPoolsAccess = _storageMgr.findUpAndEnabledHostWithAccessToStoragePools(poolIds);
            if (hostWithPoolsAccess == null) {
                s_logger.debug("No host(s) available with pool access, can not move this volume");
                res.setResult("No host(s) available with pool access, can not move this volume");
                future.complete(res);
                return future;
            }

            VolumeVO destVol = duplicateVolumeOnAnotherStorage(srcVolume, (StoragePool)destStore);
            VolumeInfo destVolume = volFactory.getVolume(destVol.getId(), destStore);

            // Create a volume on managed storage.
            AsyncCallFuture<VolumeApiResult> createVolumeFuture = createVolumeAsync(destVolume, destStore);
            VolumeApiResult createVolumeResult = createVolumeFuture.get();
            if (createVolumeResult.isFailed()) {
                s_logger.debug("Failed to create dest volume " + destVolume.getId() + ", volume can be removed");
                destroyVolume(destVolume.getId());
                destVolume.processEvent(Event.ExpungeRequested);
                destVolume.processEvent(Event.OperationSuccessed);
                volDao.remove(destVolume.getId());
                throw new CloudRuntimeException("Creation of a dest volume failed: " + createVolumeResult.getResult());
            }

            // Refresh the volume info from the DB.
            destVolume = volFactory.getVolume(destVolume.getId(), destStore);

            PrimaryDataStore srcPrimaryDataStore = (PrimaryDataStore) srcVolume.getDataStore();
            if (srcPrimaryDataStore.isManaged()) {
                Map<String, String> srcPrimaryDataStoreDetails = new HashMap<String, String>();
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.STORAGE_HOST, srcPrimaryDataStore.getHostAddress());
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(srcPrimaryDataStore.getPort()));
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED_STORE_TARGET, srcVolume.get_iScsiName());
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, srcVolume.getName());
                srcPrimaryDataStoreDetails.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(srcVolume.getSize()));
                srcPrimaryDataStoreDetails.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(srcPrimaryDataStore.getId())));
                srcPrimaryDataStore.setDetails(srcPrimaryDataStoreDetails);
                grantAccess(srcVolume, hostWithPoolsAccess, srcVolume.getDataStore());
            }

            PrimaryDataStore destPrimaryDataStore = (PrimaryDataStore) destStore;
            Map<String, String> destPrimaryDataStoreDetails = new HashMap<String, String>();
            destPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED, Boolean.TRUE.toString());
            destPrimaryDataStoreDetails.put(PrimaryDataStore.STORAGE_HOST, destPrimaryDataStore.getHostAddress());
            destPrimaryDataStoreDetails.put(PrimaryDataStore.STORAGE_PORT, String.valueOf(destPrimaryDataStore.getPort()));
            destPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED_STORE_TARGET, destVolume.get_iScsiName());
            destPrimaryDataStoreDetails.put(PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME, destVolume.getName());
            destPrimaryDataStoreDetails.put(PrimaryDataStore.VOLUME_SIZE, String.valueOf(destVolume.getSize()));
            destPrimaryDataStoreDetails.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(destPrimaryDataStore.getId())));
            destPrimaryDataStore.setDetails(destPrimaryDataStoreDetails);

            grantAccess(destVolume, hostWithPoolsAccess, destStore);

            destVolume.processEvent(Event.CreateRequested);
            srcVolume.processEvent(Event.MigrationRequested);

            CopyManagedVolumeContext<VolumeApiResult> context = new CopyManagedVolumeContext<VolumeApiResult>(null, future, srcVolume, destVolume, hostWithPoolsAccess);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyManagedVolumeCallBack(null, null)).setContext(context);

            motionSrv.copyAsync(srcVolume, destVolume, hostWithPoolsAccess, caller);
        } catch (Exception e) {
            s_logger.error("Copy to managed volume failed due to: " + e);
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Copy to managed volume failed.", e);
            }
            res.setResult(e.toString());
            future.complete(res);
        }

        return future;
    }

    protected Void copyManagedVolumeCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CopyManagedVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        VolumeInfo destVolume = context.destVolume;
        Host host = context.host;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(destVolume);

        try {
            if (srcVolume.getDataStore() != null && ((PrimaryDataStore) srcVolume.getDataStore()).isManaged()) {
                revokeAccess(srcVolume, host, srcVolume.getDataStore());
            }
            revokeAccess(destVolume, host, destVolume.getDataStore());

            if (result.isFailed()) {
                res.setResult(result.getResult());
                destVolume.processEvent(Event.MigrationCopyFailed);
                srcVolume.processEvent(Event.OperationFailed);
                try {
                    destroyVolume(destVolume.getId());
                    destVolume = volFactory.getVolume(destVolume.getId());
                    AsyncCallFuture<VolumeApiResult> destVolumeDestroyFuture = expungeVolumeAsync(destVolume);
                    destVolumeDestroyFuture.get();
                    // If dest managed volume destroy fails, wait and retry.
                    if (destVolumeDestroyFuture.get().isFailed()) {
                        Thread.sleep(5 * 1000);
                        destVolumeDestroyFuture = expungeVolumeAsync(destVolume);
                        destVolumeDestroyFuture.get();
                    }
                    future.complete(res);
                } catch (Exception e) {
                    s_logger.debug("failed to clean up managed volume on storage", e);
                }
            } else {
                srcVolume.processEvent(Event.OperationSuccessed);
                destVolume.processEvent(Event.MigrationCopySucceeded, result.getAnswer());
                volDao.updateUuid(srcVolume.getId(), destVolume.getId());
                try {
                    destroyVolume(srcVolume.getId());
                    srcVolume = volFactory.getVolume(srcVolume.getId());
                    AsyncCallFuture<VolumeApiResult> srcVolumeDestroyFuture = expungeVolumeAsync(srcVolume);
                    // If src volume destroy fails, wait and retry.
                    if (srcVolumeDestroyFuture.get().isFailed()) {
                        Thread.sleep(5 * 1000);
                        srcVolumeDestroyFuture = expungeVolumeAsync(srcVolume);
                        srcVolumeDestroyFuture.get();
                    }
                    future.complete(res);
                } catch (Exception e) {
                    s_logger.debug("failed to clean up volume on storage", e);
                }
            }
        } catch (Exception e) {
            s_logger.debug("Failed to process copy managed volume callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    private boolean requiresNewManagedVolumeInDestStore(PrimaryDataStore srcDataStore, PrimaryDataStore destDataStore) {
        if (srcDataStore == null || destDataStore == null) {
            s_logger.warn("Unable to check for new volume, either src or dest pool is null");
            return false;
        }

        if (srcDataStore.getPoolType() == StoragePoolType.PowerFlex && destDataStore.getPoolType() == StoragePoolType.PowerFlex) {
            if (srcDataStore.getId() == destDataStore.getId()) {
                return false;
            }

            final String STORAGE_POOL_SYSTEM_ID = "powerflex.storagepool.system.id";
            String srcPoolSystemId = null;
            StoragePoolDetailVO srcPoolSystemIdDetail = _storagePoolDetailsDao.findDetail(srcDataStore.getId(), STORAGE_POOL_SYSTEM_ID);
            if (srcPoolSystemIdDetail != null) {
                srcPoolSystemId = srcPoolSystemIdDetail.getValue();
            }

            String destPoolSystemId = null;
            StoragePoolDetailVO destPoolSystemIdDetail = _storagePoolDetailsDao.findDetail(destDataStore.getId(), STORAGE_POOL_SYSTEM_ID);
            if (destPoolSystemIdDetail != null) {
                destPoolSystemId = destPoolSystemIdDetail.getValue();
            }

            if (Strings.isNullOrEmpty(srcPoolSystemId) || Strings.isNullOrEmpty(destPoolSystemId)) {
                s_logger.warn("PowerFlex src pool: " + srcDataStore.getId() + " or dest pool: " + destDataStore.getId() +
                        " storage instance details are not available");
                return false;
            }

            if (!srcPoolSystemId.equals(destPoolSystemId)) {
                s_logger.debug("PowerFlex src pool: " + srcDataStore.getId() + " and dest pool: "  + destDataStore.getId() +
                        " belongs to different storage instances, create new managed volume");
                return true;
            }
        }

        // New volume not required for all other cases (address any cases required in future)
        return false;
    }

    private class MigrateVolumeContext<T> extends AsyncRpcContext<T> {
        final VolumeInfo srcVolume;
        final VolumeInfo destVolume;
        final AsyncCallFuture<VolumeApiResult> future;

        /**
         * @param callback
         */
        public MigrateVolumeContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<VolumeApiResult> future, VolumeInfo srcVolume, VolumeInfo destVolume, DataStore destStore) {
            super(callback);
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.future = future;
        }
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> migrateVolume(VolumeInfo srcVolume, DataStore destStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        try {
            if (!snapshotMgr.canOperateOnVolume(srcVolume)) {
                s_logger.debug("Snapshots are being created on this volume. This volume cannot be migrated now.");
                res.setResult("Snapshots are being created on this volume. This volume cannot be migrated now.");
                future.complete(res);
                return future;
            }

            VolumeInfo destVolume = volFactory.getVolume(srcVolume.getId(), destStore);
            srcVolume.processEvent(Event.MigrationRequested);
            MigrateVolumeContext<VolumeApiResult> context = new MigrateVolumeContext<VolumeApiResult>(null, future, srcVolume, destVolume, destStore);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().migrateVolumeCallBack(null, null)).setContext(context);
            motionSrv.copyAsync(srcVolume, destVolume, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to migrate volume", e);
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }

    protected Void migrateVolumeCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, MigrateVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                srcVolume.processEvent(Event.OperationFailed);
                future.complete(res);
            } else {
                srcVolume.processEvent(Event.OperationSuccessed);
                if (srcVolume.getStoragePoolType() == StoragePoolType.PowerFlex) {
                    future.complete(res);
                    return null;
                }
                snapshotMgr.cleanupSnapshotsByVolume(srcVolume.getId());
                future.complete(res);
            }
        } catch (Exception e) {
            s_logger.error("Failed to process migrate volume callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    private class MigrateVmWithVolumesContext<T> extends AsyncRpcContext<T> {
        final Map<VolumeInfo, DataStore> volumeToPool;
        final AsyncCallFuture<CommandResult> future;

        public MigrateVmWithVolumesContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<CommandResult> future, Map<VolumeInfo, DataStore> volumeToPool) {
            super(callback);
            this.volumeToPool = volumeToPool;
            this.future = future;
        }
    }

    @Override
    public AsyncCallFuture<CommandResult> migrateVolumes(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost) {
        AsyncCallFuture<CommandResult> future = new AsyncCallFuture<CommandResult>();
        CommandResult res = new CommandResult();
        try {
            // Check to make sure there are no snapshot operations on a volume
            // and
            // put it in the migrating state.
            List<VolumeInfo> volumesMigrating = new ArrayList<VolumeInfo>();
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeMap.entrySet()) {
                VolumeInfo volume = entry.getKey();
                if (!snapshotMgr.canOperateOnVolume(volume)) {
                    s_logger.debug("Snapshots are being created on a volume. Volumes cannot be migrated now.");
                    res.setResult("Snapshots are being created on a volume. Volumes cannot be migrated now.");
                    future.complete(res);

                    // All the volumes that are already in migrating state need
                    // to be put back in ready state.
                    for (VolumeInfo volumeMigrating : volumesMigrating) {
                        volumeMigrating.processEvent(Event.OperationFailed);
                    }
                    return future;
                } else {
                    volume.processEvent(Event.MigrationRequested);
                    volumesMigrating.add(volume);
                }
            }

            MigrateVmWithVolumesContext<CommandResult> context = new MigrateVmWithVolumesContext<CommandResult>(null, future, volumeMap);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().migrateVmWithVolumesCallBack(null, null)).setContext(context);
            motionSrv.copyAsync(volumeMap, vmTo, srcHost, destHost, caller);

        } catch (Exception e) {
            s_logger.debug("Failed to copy volume", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return future;
    }

    protected Void migrateVmWithVolumesCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, MigrateVmWithVolumesContext<CommandResult> context) {
        Map<VolumeInfo, DataStore> volumeToPool = context.volumeToPool;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<CommandResult> future = context.future;
        CommandResult res = new CommandResult();
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                    VolumeInfo volume = entry.getKey();
                    volume.processEvent(Event.OperationFailed);
                }
                future.complete(res);
            } else {
                for (Map.Entry<VolumeInfo, DataStore> entry : volumeToPool.entrySet()) {
                    VolumeInfo volume = entry.getKey();
                    snapshotMgr.cleanupSnapshotsByVolume(volume.getId());
                    volume.processEvent(Event.OperationSuccessed);
                }
                future.complete(res);
            }
        } catch (Exception e) {
            s_logger.error("Failed to process copy volume callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> registerVolume(VolumeInfo volume, DataStore store) {

        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        DataObject volumeOnStore = store.create(volume);

        volumeOnStore.processEvent(Event.CreateOnlyRequested);

        try {
            CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, volumeOnStore, future);
            AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().registerVolumeCallback(null, null));
            caller.setContext(context);

            store.getDriver().createAsync(store, volumeOnStore, caller);
        } catch (CloudRuntimeException ex) {
            // clean up already persisted volume_store_ref entry in case of createVolumeCallback is never called
            VolumeDataStoreVO volStoreVO = _volumeStoreDao.findByStoreVolume(store.getId(), volume.getId());
            if (volStoreVO != null) {
                VolumeInfo volObj = volFactory.getVolume(volume, store);
                volObj.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            VolumeApiResult res = new VolumeApiResult((VolumeObject)volumeOnStore);
            res.setResult(ex.getMessage());
            future.complete(res);
        }
        return future;
    }

    @Override
    public Pair<EndPoint, DataObject> registerVolumeForPostUpload(VolumeInfo volume, DataStore store) {

        EndPoint ep = _epSelector.select(store);
        if (ep == null) {
            String errorMessage = "There is no secondary storage VM for image store " + store.getName();
            s_logger.warn(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }
        DataObject volumeOnStore = store.create(volume);
        return new Pair<>(ep, volumeOnStore);
    }

    protected Void registerVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        VolumeObject vo = (VolumeObject)context.volume;
        try {
            if (result.isFailed()) {
                vo.processEvent(Event.OperationFailed);
                // delete the volume entry from volumes table in case of failure
                VolumeVO vol = volDao.findById(vo.getId());
                if (vol != null) {
                    volDao.remove(vo.getId());
                }

            } else {
                vo.processEvent(Event.OperationSuccessed, result.getAnswer());

                if (vo.getSize() != null) {
                    // publish usage events
                    // get physical size from volume_store_ref table
                    long physicalSize = 0;
                    DataStore ds = vo.getDataStore();
                    VolumeDataStoreVO volStore = _volumeStoreDao.findByStoreVolume(ds.getId(), vo.getId());
                    if (volStore != null) {
                        physicalSize = volStore.getPhysicalSize();
                    } else {
                        s_logger.warn("No entry found in volume_store_ref for volume id: " + vo.getId() + " and image store id: " + ds.getId() + " at the end of uploading volume!");
                    }
                    Scope dsScope = ds.getScope();
                    if (dsScope.getScopeType() == ScopeType.ZONE) {
                        if (dsScope.getScopeId() != null) {
                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_UPLOAD, vo.getAccountId(), dsScope.getScopeId(), vo.getId(), vo.getName(), null, null, physicalSize, vo.getSize(),
                                    Volume.class.getName(), vo.getUuid());
                        } else {
                            s_logger.warn("Zone scope image store " + ds.getId() + " has a null scope id");
                        }
                    } else if (dsScope.getScopeType() == ScopeType.REGION) {
                        // publish usage event for region-wide image store using a -1 zoneId for 4.2, need to revisit post-4.2
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_UPLOAD, vo.getAccountId(), -1, vo.getId(), vo.getName(), null, null, physicalSize, vo.getSize(),
                                Volume.class.getName(), vo.getUuid());

                        _resourceLimitMgr.incrementResourceCount(vo.getAccountId(), ResourceType.secondary_storage, vo.getSize());
                    }
                }
            }
            VolumeApiResult res = new VolumeApiResult(vo);
            context.future.complete(res);
            return null;

        } catch (Exception e) {
            s_logger.error("register volume failed: ", e);
            // delete the volume entry from volumes table in case of failure
            VolumeVO vol = volDao.findById(vo.getId());
            if (vol != null) {
                volDao.remove(vo.getId());
            }
            VolumeApiResult res = new VolumeApiResult(null);
            context.future.complete(res);
            return null;
        }
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> resize(VolumeInfo volume) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        try {
            volume.processEvent(Event.ResizeRequested);
        } catch (Exception e) {
            s_logger.debug("Failed to change state to resize", e);
            result.setResult(e.toString());
            future.complete(result);
            return future;
        }
        CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, volume, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().resizeVolumeCallback(caller, context)).setContext(context);

        try {
            volume.getDataStore().getDriver().resize(volume, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to change state to resize", e);

            result.setResult(e.toString());

            future.complete(result);
        }

        return future;
    }

    @Override
    public void resizeVolumeOnHypervisor(long volumeId, long newSize, long destHostId, String instanceName) {
        final String errMsg = "Resize command failed";

        try {
            Answer answer = null;
            Host destHost = _hostDao.findById(destHostId);
            EndPoint ep = RemoteHostEndPoint.getHypervisorHostEndPoint(destHost);

            if (ep != null) {
                VolumeVO volume = volDao.findById(volumeId);
                PrimaryDataStore primaryDataStore = this.dataStoreMgr.getPrimaryDataStore(volume.getPoolId());
                ResizeVolumeCommand resizeCmd = new ResizeVolumeCommand(volume.getPath(), new StorageFilerTO(primaryDataStore), volume.getSize(), newSize, true, instanceName,
                        primaryDataStore.isManaged(), volume.get_iScsiName());

                answer = ep.sendMessage(resizeCmd);
            } else {
                throw new CloudRuntimeException("Could not find a remote endpoint to send command to. Check if host or SSVM is down.");
            }

            if (answer == null || !answer.getResult()) {
                throw new CloudRuntimeException(answer != null ? answer.getDetails() : errMsg);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    protected Void resizeVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeInfo volume = (VolumeInfo)context.volume;

        if (result.isFailed()) {
            try {
                volume.processEvent(Event.OperationFailed);
            } catch (Exception e) {
                s_logger.debug("Failed to change state", e);
            }
            VolumeApiResult res = new VolumeApiResult(volume);
            res.setResult(result.getResult());
            future.complete(res);
            return null;
        }

        try {
            volume.processEvent(Event.OperationSuccessed);
        } catch (Exception e) {
            s_logger.debug("Failed to change state", e);
            VolumeApiResult res = new VolumeApiResult(volume);
            res.setResult(result.getResult());
            future.complete(res);
            return null;
        }

        VolumeApiResult res = new VolumeApiResult(volume);
        future.complete(res);

        return null;
    }

    @Override
    public void handleVolumeSync(DataStore store) {
        if (store == null) {
            s_logger.warn("Huh? image store is null");
            return;
        }
        long storeId = store.getId();

        // add lock to make template sync for a data store only be done once
        String lockString = "volumesync.storeId:" + storeId;
        GlobalLock syncLock = GlobalLock.getInternLock(lockString);
        try {
            if (syncLock.lock(3)) {
                try {
                    Map<Long, TemplateProp> volumeInfos = listVolume(store);
                    if (volumeInfos == null) {
                        return;
                    }

                    // find all the db volumes including those with NULL url column to avoid accidentally deleting volumes on image store later.
                    List<VolumeDataStoreVO> dbVolumes = _volumeStoreDao.listByStoreId(storeId);
                    List<VolumeDataStoreVO> toBeDownloaded = new ArrayList<VolumeDataStoreVO>(dbVolumes);
                    for (VolumeDataStoreVO volumeStore : dbVolumes) {
                        VolumeVO volume = volDao.findById(volumeStore.getVolumeId());
                        if (volume == null) {
                            s_logger.warn("Volume_store_ref table shows that volume " + volumeStore.getVolumeId() + " is on image store " + storeId
                                    + ", but the volume is not found in volumes table, potentially some bugs in deleteVolume, so we just treat this volume to be deleted and mark it as destroyed");
                            volumeStore.setDestroyed(true);
                            _volumeStoreDao.update(volumeStore.getId(), volumeStore);
                            continue;
                        }
                        // Exists then don't download
                        if (volumeInfos.containsKey(volume.getId())) {
                            TemplateProp volInfo = volumeInfos.remove(volume.getId());
                            toBeDownloaded.remove(volumeStore);
                            s_logger.info("Volume Sync found " + volume.getUuid() + " already in the volume image store table");
                            if (volumeStore.getDownloadState() != Status.DOWNLOADED) {
                                volumeStore.setErrorString("");
                            }
                            if (volInfo.isCorrupted()) {
                                volumeStore.setDownloadState(Status.DOWNLOAD_ERROR);
                                String msg = "Volume " + volume.getUuid() + " is corrupted on image store";
                                volumeStore.setErrorString(msg);
                                s_logger.info(msg);
                                if (volume.getState() == State.NotUploaded || volume.getState() == State.UploadInProgress) {
                                    s_logger.info("Volume Sync found " + volume.getUuid() + " uploaded using SSVM on image store " + storeId + " as corrupted, marking it as failed");
                                    _volumeStoreDao.update(volumeStore.getId(), volumeStore);
                                    // mark volume as failed, so that storage GC will clean it up
                                    VolumeObject volObj = (VolumeObject)volFactory.getVolume(volume.getId());
                                    volObj.processEvent(Event.OperationFailed);
                                } else if (volumeStore.getDownloadUrl() == null) {
                                    msg = "Volume (" + volume.getUuid() + ") with install path " + volInfo.getInstallPath() + " is corrupted, please check in image store: "
                                            + volumeStore.getDataStoreId();
                                    s_logger.warn(msg);
                                } else {
                                    s_logger.info("Removing volume_store_ref entry for corrupted volume " + volume.getName());
                                    _volumeStoreDao.remove(volumeStore.getId());
                                    toBeDownloaded.add(volumeStore);
                                }
                            } else { // Put them in right status
                                volumeStore.setDownloadPercent(100);
                                volumeStore.setDownloadState(Status.DOWNLOADED);
                                volumeStore.setState(ObjectInDataStoreStateMachine.State.Ready);
                                volumeStore.setInstallPath(volInfo.getInstallPath());
                                volumeStore.setSize(volInfo.getSize());
                                volumeStore.setPhysicalSize(volInfo.getPhysicalSize());
                                volumeStore.setLastUpdated(new Date());
                                _volumeStoreDao.update(volumeStore.getId(), volumeStore);

                                if (volume.getSize() == 0) {
                                    // Set volume size in volumes table
                                    volume.setSize(volInfo.getSize());
                                    volDao.update(volumeStore.getVolumeId(), volume);
                                }

                                if (volume.getState() == State.NotUploaded || volume.getState() == State.UploadInProgress) {
                                    VolumeObject volObj = (VolumeObject)volFactory.getVolume(volume.getId());
                                    volObj.processEvent(Event.OperationSuccessed);
                                }

                                if (volInfo.getSize() > 0) {
                                    try {
                                        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(volume.getAccountId()), com.cloud.configuration.Resource.ResourceType.secondary_storage,
                                                volInfo.getSize() - volInfo.getPhysicalSize());
                                    } catch (ResourceAllocationException e) {
                                        s_logger.warn(e.getMessage());
                                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, volume.getDataCenterId(), volume.getPodId(), e.getMessage(), e.getMessage());
                                    } finally {
                                        _resourceLimitMgr.recalculateResourceCount(volume.getAccountId(), volume.getDomainId(),
                                                com.cloud.configuration.Resource.ResourceType.secondary_storage.getOrdinal());
                                    }
                                }
                            }
                            continue;
                        } else if (volume.getState() == State.NotUploaded || volume.getState() == State.UploadInProgress) { // failed uploads through SSVM
                            s_logger.info("Volume Sync did not find " + volume.getUuid() + " uploaded using SSVM on image store " + storeId + ", marking it as failed");
                            toBeDownloaded.remove(volumeStore);
                            volumeStore.setDownloadState(Status.DOWNLOAD_ERROR);
                            String msg = "Volume " + volume.getUuid() + " is corrupted on image store";
                            volumeStore.setErrorString(msg);
                            _volumeStoreDao.update(volumeStore.getId(), volumeStore);
                            // mark volume as failed, so that storage GC will clean it up
                            VolumeObject volObj = (VolumeObject)volFactory.getVolume(volume.getId());
                            volObj.processEvent(Event.OperationFailed);
                            continue;
                        }
                        // Volume is not on secondary but we should download.
                        if (volumeStore.getDownloadState() != Status.DOWNLOADED) {
                            s_logger.info("Volume Sync did not find " + volume.getName() + " ready on image store " + storeId + ", will request download to start/resume shortly");
                        }
                    }

                    // Download volumes which haven't been downloaded yet.
                    if (toBeDownloaded.size() > 0) {
                        for (VolumeDataStoreVO volumeHost : toBeDownloaded) {
                            if (volumeHost.getDownloadUrl() == null) { // If url is null, skip downloading
                                s_logger.info("Skip downloading volume " + volumeHost.getVolumeId() + " since no download url is specified.");
                                continue;
                            }

                            // if this is a region store, and there is already an DOWNLOADED entry there without install_path information, which
                            // means that this is a duplicate entry from migration of previous NFS to staging.
                            if (store.getScope().getScopeType() == ScopeType.REGION) {
                                if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED && volumeHost.getInstallPath() == null) {
                                    s_logger.info("Skip sync volume for migration of previous NFS to object store");
                                    continue;
                                }
                            }

                            s_logger.debug("Volume " + volumeHost.getVolumeId() + " needs to be downloaded to " + store.getName());
                            // reset volume status back to Allocated
                            VolumeObject vol = (VolumeObject)volFactory.getVolume(volumeHost.getVolumeId());
                            vol.processEvent(Event.OperationFailed); // reset back volume status
                            // remove leftover volume_store_ref entry since re-download will create it again
                            _volumeStoreDao.remove(volumeHost.getId());
                            // get an updated volumeVO
                            vol = (VolumeObject)volFactory.getVolume(volumeHost.getVolumeId());
                            RegisterVolumePayload payload = new RegisterVolumePayload(volumeHost.getDownloadUrl(), volumeHost.getChecksum(), vol.getFormat().toString());
                            vol.addPayload(payload);
                            createVolumeAsync(vol, store);
                        }
                    }

                    // Delete volumes which are not present on DB.
                    for (Map.Entry<Long, TemplateProp> entry : volumeInfos.entrySet()) {
                        Long uniqueName = entry.getKey();
                        TemplateProp tInfo = entry.getValue();

                        // we cannot directly call expungeVolumeAsync here to reuse delete logic since in this case db does not have this volume at all.
                        VolumeObjectTO tmplTO = new VolumeObjectTO();
                        tmplTO.setDataStore(store.getTO());
                        tmplTO.setPath(tInfo.getInstallPath());
                        tmplTO.setId(tInfo.getId());
                        DeleteCommand dtCommand = new DeleteCommand(tmplTO);
                        EndPoint ep = _epSelector.select(store);
                        Answer answer = null;
                        if (ep == null) {
                            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                            s_logger.error(errMsg);
                            answer = new Answer(dtCommand, false, errMsg);
                        } else {
                            answer = ep.sendMessage(dtCommand);
                        }
                        if (answer == null || !answer.getResult()) {
                            s_logger.info("Failed to deleted volume at store: " + store.getName());

                        } else {
                            String description = "Deleted volume " + tInfo.getTemplateName() + " on secondary storage " + storeId;
                            s_logger.info(description);
                        }
                    }
                } finally {
                    syncLock.unlock();
                }
            } else {
                s_logger.info("Couldn't get global lock on " + lockString + ", another thread may be doing volume sync on data store " + storeId + " now.");
            }
        } finally {
            syncLock.releaseRef();
        }
    }

    private Map<Long, TemplateProp> listVolume(DataStore store) {
        ListVolumeCommand cmd = new ListVolumeCommand(store.getTO(), store.getUri());
        EndPoint ep = _epSelector.select(store);
        Answer answer = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        if (answer != null && answer.getResult()) {
            ListVolumeAnswer tanswer = (ListVolumeAnswer)answer;
            return tanswer.getTemplateInfo();
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Can not list volumes for image store " + store.getId());
            }
        }

        return null;
    }

    @Override
    public SnapshotInfo takeSnapshot(VolumeInfo volume) {
        SnapshotInfo snapshot = null;
        try {
            snapshot = snapshotMgr.takeSnapshot(volume);
        } catch (CloudRuntimeException cre) {
            s_logger.error("Take snapshot: " + volume.getId() + " failed", cre);
            throw cre;
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("unknown exception while taking snapshot for volume " + volume.getId() + " was caught", e);
            }
            throw new CloudRuntimeException("Failed to take snapshot", e);
        }

        return snapshot;
    }

    // For managed storage on Xen and VMware, we need to potentially make space for hypervisor snapshots.
    // The disk offering can collect this information and pass it on to the volume that's about to be created.
    // Ex. if you want a 10 GB CloudStack volume to reside on managed storage on Xen, this leads to an SR
    // that is a total size of (10 GB * (hypervisorSnapshotReserveSpace / 100) + 10 GB).
    @Override
    public VolumeInfo updateHypervisorSnapshotReserveForVolume(DiskOffering diskOffering, long volumeId, HypervisorType hyperType) {
        if (diskOffering != null && hyperType != null) {
            Integer hypervisorSnapshotReserve = diskOffering.getHypervisorSnapshotReserve();

            if (hyperType == HypervisorType.KVM) {
                hypervisorSnapshotReserve = null;
            } else if (hypervisorSnapshotReserve == null || hypervisorSnapshotReserve < 0) {
                hypervisorSnapshotReserve = 0;
            }

            VolumeVO volume = volDao.findById(volumeId);

            volume.setHypervisorSnapshotReserve(hypervisorSnapshotReserve);

            volDao.update(volume.getId(), volume);
        }

        return volFactory.getVolume(volumeId);
    }

    @DB
    @Override
    /**
     * The volume must be marked as expunged on DB to exclude it from the storage cleanup task
     */
    public void unmanageVolume(long volumeId) {
        VolumeInfo vol = volFactory.getVolume(volumeId);
        if (vol != null) {
            vol.stateTransit(Volume.Event.DestroyRequested);
            snapshotMgr.deletePoliciesForVolume(volumeId);
            vol.stateTransit(Volume.Event.OperationSucceeded);
            vol.stateTransit(Volume.Event.ExpungingRequested);
            vol.stateTransit(Volume.Event.OperationSucceeded);
            volDao.remove(vol.getId());
        }
    }
}
