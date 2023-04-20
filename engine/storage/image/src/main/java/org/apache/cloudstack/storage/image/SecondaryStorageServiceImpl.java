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

package org.apache.cloudstack.storage.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SecondaryStorageService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;

import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.Pair;

public class SecondaryStorageServiceImpl implements SecondaryStorageService {

    private static final Logger s_logger = Logger.getLogger(SecondaryStorageServiceImpl.class);

    @Inject
    DataMotionService motionSrv;
    @Inject
    CommandExecLogDao _cmdExecLogDao;
    @Inject
    TemplateDataStoreDao templateStoreDao;
    @Inject
    SnapshotDataStoreDao snapshotStoreDao;
    @Inject
    VolumeDataStoreDao volumeDataStoreDao;

    private class MigrateDataContext<T> extends AsyncRpcContext<T> {
        final DataObject srcData;
        final DataObject destData;
        final AsyncCallFuture<DataObjectResult> future;

        /**
         * @param callback
         */
        public MigrateDataContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<DataObjectResult> future, DataObject srcData, DataObject destData, DataStore destStore) {
            super(callback);
            this.srcData = srcData;
            this.destData = destData;
            this.future = future;
        }
    }

    @Override
    public AsyncCallFuture<DataObjectResult> migrateData(DataObject srcDataObject, DataStore srcDatastore, DataStore destDatastore,
                                                         Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain, Map<DataObject, Pair<List<TemplateInfo>, Long>> templateChain) {
        AsyncCallFuture<DataObjectResult> future = new AsyncCallFuture<DataObjectResult>();
        DataObjectResult res = new DataObjectResult(srcDataObject);
        DataObject destDataObject = null;
        try {
            if (srcDataObject instanceof SnapshotInfo && snapshotChain != null && snapshotChain.containsKey(srcDataObject)) {
                List<String> parentSnapshotPaths = new ArrayList<>();
                for (SnapshotInfo snapshotInfo : snapshotChain.get(srcDataObject).first()) {
                    destDataObject = null;
                    if (!parentSnapshotPaths.isEmpty() && parentSnapshotPaths.contains(snapshotInfo.getPath())) {
                        parentSnapshotPaths.add(snapshotInfo.getPath());
                        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, srcDatastore.getId(), snapshotInfo.getSnapshotId());
                        if (snapshotStore == null) {
                            res.setResult("Failed to find snapshot " + snapshotInfo.getUuid() + " on store: " + srcDatastore.getName());
                            res.setSuccess(false);
                            future.complete(res);
                            break;
                        }
                        snapshotStore.setDataStoreId(destDatastore.getId());
                        snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);
                        continue;
                    }
                    parentSnapshotPaths.add(snapshotInfo.getPath());
                    destDataObject = destDatastore.create(snapshotInfo);
                    snapshotInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                    destDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                    migrateJob(future, snapshotInfo, destDataObject, destDatastore);
                    if (future.get() != null && future.get().isFailed()) {
                        break;
                    }
                }
            } else if (srcDataObject instanceof TemplateInfo && templateChain != null && templateChain.containsKey(srcDataObject)) {
                for (TemplateInfo templateInfo : templateChain.get(srcDataObject).first()) {
                    destDataObject = destDatastore.create(templateInfo);
                    templateInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                    destDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                    migrateJob(future, templateInfo, destDataObject, destDatastore);
                }
            }
            else {
                // Check if template in destination store, if yes, do not proceed
                if (srcDataObject instanceof TemplateInfo) {
                    s_logger.debug("Checking if template present at destination");
                    TemplateDataStoreVO templateStoreVO = templateStoreDao.findByStoreTemplate(destDatastore.getId(), srcDataObject.getId());
                    if (templateStoreVO != null) {
                        String msg = "Template already exists in destination store";
                        s_logger.debug(msg);
                        res.setResult(msg);
                        res.setSuccess(true);
                        future.complete(res);
                        return future;
                    }
                }
                destDataObject = destDatastore.create(srcDataObject);
                srcDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                destDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrateDataRequested);
                migrateJob(future, srcDataObject, destDataObject, destDatastore);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to copy Data", e);
            if (destDataObject != null) {
                s_logger.info("Deleting data on destination store: " + destDataObject.getDataStore().getName());
                destDataObject.getDataStore().delete(destDataObject);
            }
            if (!(srcDataObject instanceof VolumeInfo)) {
                srcDataObject.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            } else {
                ((VolumeInfo) srcDataObject).processEventOnly(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }

    protected void migrateJob(AsyncCallFuture<DataObjectResult> future, DataObject srcDataObject, DataObject destDataObject, DataStore destDatastore) throws ExecutionException, InterruptedException {
        MigrateDataContext<DataObjectResult> context = new MigrateDataContext<DataObjectResult>(null, future, srcDataObject, destDataObject, destDatastore);
        AsyncCallbackDispatcher<SecondaryStorageServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().migrateDataCallBack(null, null)).setContext(context);
        motionSrv.copyAsync(srcDataObject, destDataObject, caller);
    }

    /**
     * Callback function to handle state change of source and destination data objects based on the success or failure of the migrate task
     */
    protected Void migrateDataCallBack(AsyncCallbackDispatcher<SecondaryStorageServiceImpl, CopyCommandResult> callback, MigrateDataContext<DataObjectResult> context) throws ExecutionException, InterruptedException {
        DataObject srcData = context.srcData;
        DataObject destData = context.destData;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<DataObjectResult> future = context.future;
        DataObjectResult res = new DataObjectResult(srcData);
        Answer answer = result.getAnswer();
        try {
            if (!answer.getResult()) {
                s_logger.warn("Migration failed for "+srcData.getUuid());
                res.setResult(result.getResult());
                if (!(srcData instanceof VolumeInfo) ) {
                    srcData.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                    destData.processEvent(ObjectInDataStoreStateMachine.Event.MigrationFailed);
                    destData.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);
                } else {
                    ((VolumeInfo)srcData).processEventOnly(ObjectInDataStoreStateMachine.Event.OperationFailed);
                    ((VolumeInfo)destData).processEventOnly(ObjectInDataStoreStateMachine.Event.MigrationFailed);
                    ((VolumeInfo)destData).processEventOnly(ObjectInDataStoreStateMachine.Event.DestroyRequested);
                }

                if (destData != null) {
                    destData.getDataStore().delete(destData);
                }
            } else {
                if (destData instanceof  VolumeInfo) {
                    ((VolumeInfo) destData).processEventOnly(ObjectInDataStoreStateMachine.Event.OperationSuccessed, answer);
                } else {
                    destData.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed, answer);
                }
                updateDataObject(srcData, destData);
                s_logger.debug("Deleting source data");
                srcData.getDataStore().delete(srcData);
                s_logger.debug("Successfully migrated "+srcData.getUuid());
            }
            _cmdExecLogDao.expunge(Long.parseLong(answer.getContextParam("cmd")));
            future.complete(res);
        } catch (Exception e) {
            s_logger.error("Failed to process migrate data callback", e);
            res.setResult(e.toString());
            _cmdExecLogDao.expunge(Long.parseLong(answer.getContextParam("cmd")));
            future.complete(res);
        }
        return null;
    }

    private void updateDataObject(DataObject srcData, DataObject destData) {
        if (destData instanceof SnapshotInfo) {
            SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySourceSnapshot(srcData.getId(), DataStoreRole.Image);
            SnapshotDataStoreVO destSnapshotStore = snapshotStoreDao.findBySnapshot(srcData.getId(), DataStoreRole.Image);
            if (snapshotStore != null && destSnapshotStore != null) {
                destSnapshotStore.setPhysicalSize(snapshotStore.getPhysicalSize());
                destSnapshotStore.setCreated(snapshotStore.getCreated());
                if (snapshotStore.getParentSnapshotId() != destSnapshotStore.getParentSnapshotId()) {
                    destSnapshotStore.setParentSnapshotId(snapshotStore.getParentSnapshotId());
                }
                snapshotStoreDao.update(destSnapshotStore.getId(), destSnapshotStore);
            }
        } else if (destData instanceof VolumeInfo) {
            VolumeDataStoreVO srcVolume = volumeDataStoreDao.findByStoreVolume(srcData.getDataStore().getId(), srcData.getId());
            VolumeDataStoreVO destVolume = volumeDataStoreDao.findByStoreVolume(destData.getDataStore().getId(), destData.getId());
            if (srcVolume != null && destVolume != null) {
                destVolume.setPhysicalSize(srcVolume.getPhysicalSize());
                destVolume.setCreated(srcVolume.getCreated());
                volumeDataStoreDao.update(destVolume.getId(), destVolume);
            }
        } else if (destData instanceof TemplateInfo) {
            TemplateDataStoreVO srcTemplate = templateStoreDao.findByStoreTemplate(srcData.getDataStore().getId(), srcData.getId());
            TemplateDataStoreVO destTemplate = templateStoreDao.findByStoreTemplate(destData.getDataStore().getId(), destData.getId());
            if (srcTemplate != null && destTemplate != null) {
                destTemplate.setCreated(srcTemplate.getCreated());
                templateStoreDao.update(destTemplate.getId(), destTemplate);
            }
        } else {
            s_logger.debug("Unsupported data object type");
        }
    }
}
