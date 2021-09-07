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
package org.apache.cloudstack.storage.datastore;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@Component
public class DataObjectManagerImpl implements DataObjectManager {
    private static final Logger s_logger = Logger.getLogger(DataObjectManagerImpl.class);
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    DataMotionService motionSrv;
    protected long waitingTime = 1800; // half an hour
    protected long waitingRetries = 10;

    protected DataObject waitingForCreated(DataObject dataObj, DataStore dataStore) {
        long retries = this.waitingRetries;
        DataObjectInStore obj = null;
        do {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                s_logger.debug("sleep interrupted", e);
                throw new CloudRuntimeException("sleep interrupted", e);
            }

            obj = objectInDataStoreMgr.findObject(dataObj, dataStore);
            if (obj == null) {
                s_logger.debug("can't find object in db, maybe it's cleaned up already, exit waiting");
                break;
            }
            if (obj.getState() == ObjectInDataStoreStateMachine.State.Ready) {
                break;
            }
            retries--;
        } while (retries > 0);

        if (obj == null || retries <= 0) {
            s_logger.debug("waiting too long for template downloading, marked it as failed");
            throw new CloudRuntimeException("waiting too long for template downloading, marked it as failed");
        }
        return objectInDataStoreMgr.get(dataObj, dataStore, null);
    }

    class CreateContext<T> extends AsyncRpcContext<T> {
        final DataObject objInStrore;

        public CreateContext(AsyncCompletionCallback<T> callback, DataObject objInStore) {
            super(callback);
            this.objInStrore = objInStore;
        }

    }

    @Override
    public void createAsync(DataObject data, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback, boolean noCopy) {
        DataObjectInStore obj = objectInDataStoreMgr.findObject(data, store);
        DataObject objInStore = null;
        boolean freshNewTemplate = false;
        if (obj == null) {
            try {
                objInStore = objectInDataStoreMgr.create(data, store);
                freshNewTemplate = true;
            } catch (Throwable e) {
                obj = objectInDataStoreMgr.findObject(data, store);
                if (obj == null) {
                    CreateCmdResult result = new CreateCmdResult(null, null);
                    result.setSuccess(false);
                    result.setResult(e.toString());
                    callback.complete(result);
                    return;
                }
            }
        }

        if (!freshNewTemplate && obj.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            try {
                objInStore = waitingForCreated(data, store);
            } catch (Exception e) {
                CreateCmdResult result = new CreateCmdResult(null, null);
                result.setSuccess(false);
                result.setResult(e.toString());
                callback.complete(result);
                return;
            }

            CreateCmdResult result = new CreateCmdResult(null, null);
            callback.complete(result);
            return;
        }

        try {
            ObjectInDataStoreStateMachine.Event event = null;
            if (noCopy) {
                event = ObjectInDataStoreStateMachine.Event.CreateOnlyRequested;
            } else {
                event = ObjectInDataStoreStateMachine.Event.CreateRequested;
            }
            objectInDataStoreMgr.update(objInStore, event);
        } catch (NoTransitionException e) {
            try {
                objectInDataStoreMgr.update(objInStore, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("state transation failed", e1);
            }
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(e.toString());
            callback.complete(result);
            return;
        } catch (ConcurrentOperationException e) {
            try {
                objectInDataStoreMgr.update(objInStore, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("state transation failed", e1);
            }
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(e.toString());
            callback.complete(result);
            return;
        }

        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, objInStore);
        AsyncCallbackDispatcher<DataObjectManagerImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createAsynCallback(null, null)).setContext(context);

        store.getDriver().createAsync(store, objInStore, caller);
        return;
    }

    protected Void createAsynCallback(AsyncCallbackDispatcher<DataObjectManagerImpl, CreateCmdResult> callback, CreateContext<CreateCmdResult> context) {
        CreateCmdResult result = callback.getResult();
        DataObject objInStrore = context.objInStrore;
        CreateCmdResult upResult = new CreateCmdResult(null, null);
        if (result.isFailed()) {
            upResult.setResult(result.getResult());
            context.getParentCallback().complete(upResult);
            return null;
        }

        try {
            objectInDataStoreMgr.update(objInStrore, ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            try {
                objectInDataStoreMgr.update(objInStrore, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to change state", e1);
            }

            upResult.setResult(e.toString());
            context.getParentCallback().complete(upResult);
            return null;
        } catch (ConcurrentOperationException e) {
            try {
                objectInDataStoreMgr.update(objInStrore, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to change state", e1);
            }

            upResult.setResult(e.toString());
            context.getParentCallback().complete(upResult);
            return null;
        }

        context.getParentCallback().complete(result);
        return null;
    }

    class CopyContext<T> extends AsyncRpcContext<T> {
        DataObject destObj;
        DataObject srcObj;

        public CopyContext(AsyncCompletionCallback<T> callback, DataObject srcObj, DataObject destObj) {
            super(callback);
            this.srcObj = srcObj;
            this.destObj = destObj;
        }
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CreateCmdResult> callback) {
        try {
            objectInDataStoreMgr.update(destData, ObjectInDataStoreStateMachine.Event.CopyingRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("failed to change state", e);
            try {
                objectInDataStoreMgr.update(destData, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to further change state to OperationFailed", e1);
            }
            CreateCmdResult res = new CreateCmdResult(null, null);
            res.setResult("Failed to change state: " + e.toString());
            callback.complete(res);
        } catch (ConcurrentOperationException e) {
            s_logger.debug("failed to change state", e);
            try {
                objectInDataStoreMgr.update(destData, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to further change state to OperationFailed", e1);
            }
            CreateCmdResult res = new CreateCmdResult(null, null);
            res.setResult("Failed to change state: " + e.toString());
            callback.complete(res);
        }

        CopyContext<CreateCmdResult> anotherCall = new CopyContext<CreateCmdResult>(callback, srcData, destData);
        AsyncCallbackDispatcher<DataObjectManagerImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyCallback(null, null)).setContext(anotherCall);

        motionSrv.copyAsync(srcData, destData, caller);
    }

    protected Void copyCallback(AsyncCallbackDispatcher<DataObjectManagerImpl, CopyCommandResult> callback, CopyContext<CreateCmdResult> context) {
        CopyCommandResult result = callback.getResult();
        DataObject destObj = context.destObj;

        if (result.isFailed()) {
            try {
                objectInDataStoreMgr.update(destObj, Event.OperationFailed);
            } catch (NoTransitionException e) {
                s_logger.debug("Failed to update copying state", e);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("Failed to update copying state", e);
            }
            CreateCmdResult res = new CreateCmdResult(null, null);
            res.setResult(result.getResult());
            context.getParentCallback().complete(res);
        }

        try {
            objectInDataStoreMgr.update(destObj, ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to update copying state: ", e);
            try {
                objectInDataStoreMgr.update(destObj, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to further change state to OperationFailed", e1);
            }
            CreateCmdResult res = new CreateCmdResult(null, null);
            res.setResult("Failed to update copying state: " + e.toString());
            context.getParentCallback().complete(res);
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Failed to update copying state: ", e);
            try {
                objectInDataStoreMgr.update(destObj, ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (Exception e1) {
                s_logger.debug("failed to further change state to OperationFailed", e1);
            }
            CreateCmdResult res = new CreateCmdResult(null, null);
            res.setResult("Failed to update copying state: " + e.toString());
            context.getParentCallback().complete(res);
        }
        CreateCmdResult res = new CreateCmdResult(result.getPath(), null);
        context.getParentCallback().complete(res);
        return null;
    }

    class DeleteContext<T> extends AsyncRpcContext<T> {
        private final DataObject obj;

        public DeleteContext(AsyncCompletionCallback<T> callback, DataObject obj) {
            super(callback);
            this.obj = obj;
        }

    }

    @Override
    public void deleteAsync(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        try {
            objectInDataStoreMgr.update(data, Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("destroy failed", e);
            CreateCmdResult res = new CreateCmdResult(null, null);
            callback.complete(res);
        } catch (ConcurrentOperationException e) {
            s_logger.debug("destroy failed", e);
            CreateCmdResult res = new CreateCmdResult(null, null);
            callback.complete(res);
        }

        DeleteContext<CommandResult> context = new DeleteContext<CommandResult>(callback, data);
        AsyncCallbackDispatcher<DataObjectManagerImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteAsynCallback(null, null)).setContext(context);

        data.getDataStore().getDriver().deleteAsync(data.getDataStore(), data, caller);
        return;
    }

    protected Void deleteAsynCallback(AsyncCallbackDispatcher<DataObjectManagerImpl, CommandResult> callback, DeleteContext<CommandResult> context) {
        DataObject destObj = context.obj;

        CommandResult res = callback.getResult();
        if (res.isFailed()) {
            try {
                objectInDataStoreMgr.update(destObj, Event.OperationFailed);
            } catch (NoTransitionException e) {
                s_logger.debug("delete failed", e);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("delete failed", e);
            }

        } else {
            try {
                objectInDataStoreMgr.update(destObj, Event.OperationSuccessed);
            } catch (NoTransitionException e) {
                s_logger.debug("delete failed", e);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("delete failed", e);
            }
        }

        context.getParentCallback().complete(res);
        return null;
    }

    @Override
    public DataObject createInternalStateOnly(DataObject data, DataStore store) {
        DataObjectInStore obj = objectInDataStoreMgr.findObject(data, store);
        DataObject objInStore = null;
        if (obj == null) {
            objInStore = objectInDataStoreMgr.create(data, store);
        }
        try {
            ObjectInDataStoreStateMachine.Event event = null;
            event = ObjectInDataStoreStateMachine.Event.CreateRequested;
            objectInDataStoreMgr.update(objInStore, event);

            objectInDataStoreMgr.update(objInStore, ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state", e);
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state", e);
        }

        return objInStore;
    }

    @Override
    public void update(DataObject data, String path, Long size) {
        throw new CloudRuntimeException("not implemented");
    }
}
