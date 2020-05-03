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
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.log4j.Logger;

import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.utils.Pair;

public class StorageServiceImpl implements SecondaryStorageService {

    private static final Logger s_logger = Logger.getLogger(StorageServiceImpl.class);

    @Inject
    DataMotionService motionSrv;
    @Inject
    CommandExecLogDao _cmdExecLogDao;

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
    public AsyncCallFuture<DataObjectResult> migrateData(DataObject srcDataObject, DataStore srcDatastore, DataStore destDatastore, Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain) {
        AsyncCallFuture<DataObjectResult> future = new AsyncCallFuture<DataObjectResult>();
        DataObjectResult res = new DataObjectResult(srcDataObject);
        DataObject destDataObject = null;
        try {
            if (srcDataObject instanceof SnapshotInfo && snapshotChain.keySet().contains(srcDataObject)) {
                s_logger.debug("PEARL - snapshot instance with a chain of snaps: size"+ snapshotChain.get(srcDataObject).first().size());
                for (SnapshotInfo snapshotInfo : snapshotChain.get(srcDataObject).first()) {
                    destDataObject = destDatastore.create(snapshotInfo);
                    snapshotInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrationRequested);
                    destDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrationRequested);
                    // migrateJob(future, snapshotInfo, destDataObject, destDatastore);
                    s_logger.debug("PEARL - snap name: "+ snapshotInfo.getName());
                    MigrateDataContext<DataObjectResult> context = new MigrateDataContext<DataObjectResult>(null, future, snapshotInfo, destDataObject, destDatastore);
                    AsyncCallbackDispatcher<StorageServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
                    caller.setCallback(caller.getTarget().migrateDataCallBack(null, null)).setContext(context);
                    s_logger.debug(snapshotInfo.getDataStore().getTO().toString());
                    motionSrv.copyAsync(snapshotInfo, destDataObject, caller);
                }
            } else {
                s_logger.debug("PEARL - not a snapshot instance");
                destDataObject = destDatastore.create(srcDataObject);
                srcDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrationRequested);
                destDataObject.processEvent(ObjectInDataStoreStateMachine.Event.MigrationRequested);
                //migrateJob(future, srcDataObject, destDataObject, destDatastore);
                MigrateDataContext<DataObjectResult> context = new MigrateDataContext<DataObjectResult>(null, future, srcDataObject, destDataObject, destDatastore);
                AsyncCallbackDispatcher<StorageServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
                caller.setCallback(caller.getTarget().migrateDataCallBack(null, null)).setContext(context);
                s_logger.debug(srcDataObject.getDataStore().getTO().toString());
                motionSrv.copyAsync(srcDataObject, destDataObject, caller);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to copy Data", e);
            if (destDataObject != null) {
                destDataObject.getDataStore().delete(destDataObject);
                srcDataObject.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }

//    protected void migrateJob(AsyncCallFuture<DataObjectResult> future, DataObject srcDataObject, DataObject destDataObject, DataStore destDatastore) throws ExecutionException, InterruptedException {
//        s_logger.debug("PEARL - in migrateJob() ");
//        MigrateDataContext<DataObjectResult> context = new MigrateDataContext<DataObjectResult>(null, future, srcDataObject, destDataObject, destDatastore);
//        AsyncCallbackDispatcher<StorageManagerImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
//        caller.setCallback(caller.getTarget().migrateDataCallBack(null, null)).setContext(context);
//        s_logger.debug(srcDataObject.getDataStore().getTO().toString());
//        motionSrv.copyAsync(srcDataObject, destDataObject, caller);
//    }

    /**
     * Callback function to handle state change of source and destination data objects based on the success or failure of the migrate task
     */
    protected Void migrateDataCallBack(AsyncCallbackDispatcher<StorageServiceImpl, CopyCommandResult> callback, MigrateDataContext<DataObjectResult> context) throws ExecutionException, InterruptedException {
        s_logger.debug("PEARL - completed transfer - @ migrate callback");
        DataObject srcData = context.srcData;
        DataObject destData = context.destData;
        s_logger.debug("PEARL - src data = "+srcData.getUri());
        s_logger.debug("PEARL - dest data = "+ destData.getUri());
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<DataObjectResult> future = context.future;
        DataObjectResult res = new DataObjectResult(srcData);
        CopyCmdAnswer answer = (CopyCmdAnswer) result.getAnswer();
        try {
            if (!answer.getResult()) {
                s_logger.debug("PEARL - migration failed");
                res.setResult(result.getResult());
                srcData.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                destData.processEvent(ObjectInDataStoreStateMachine.Event.MigrationFailed);
                destData.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

                if (destData != null) {
                    destData.getDataStore().delete(destData);
                }

            } else {
                s_logger.debug("PEARL - migration succeeded");
                destData.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed, answer);
                s_logger.debug("PEARL - Deleting source data");
                srcData.getDataStore().delete(srcData);
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

}


