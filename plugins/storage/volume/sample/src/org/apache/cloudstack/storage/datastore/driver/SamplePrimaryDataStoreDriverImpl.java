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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.datastore.DataObjectManager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SamplePrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(SamplePrimaryDataStoreDriverImpl.class);
    @Inject
    EndPointSelector selector;
    @Inject
    StoragePoolHostDao storeHostDao;
    @Inject
    DataObjectManager dataObjMgr;

    public SamplePrimaryDataStoreDriverImpl() {

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
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null;
    }

    private class CreateVolumeContext<T> extends AsyncRpcContext<T> {
        private final DataObject volume;
        public CreateVolumeContext(AsyncCompletionCallback<T> callback, DataObject volume) {
            super(callback);
            this.volume = volume;
        }
    }

    public Void createAsyncCallback(AsyncCallbackDispatcher<SamplePrimaryDataStoreDriverImpl, Answer> callback,
            CreateVolumeContext<CreateCmdResult> context) {
        /*
         * CreateCmdResult result = null; CreateObjectAnswer volAnswer =
         * (CreateObjectAnswer) callback.getResult(); if (volAnswer.getResult())
         * { result = new CreateCmdResult(volAnswer.getPath(), volAnswer); }
         * else { result = new CreateCmdResult("", null);
         * result.setResult(volAnswer.getDetails()); }
         * 
         * context.getParentCallback().complete(result);
         */
        return null;
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject vo, AsyncCompletionCallback<CommandResult> callback) {
        /*
         * DeleteCommand cmd = new DeleteCommand(vo.getUri());
         * 
         * EndPoint ep = selector.select(vo); AsyncRpcContext<CommandResult>
         * context = new AsyncRpcContext<CommandResult>(callback);
         * AsyncCallbackDispatcher<SamplePrimaryDataStoreDriverImpl, Answer>
         * caller = AsyncCallbackDispatcher.create(this);
         * caller.setCallback(caller.getTarget().deleteCallback(null, null))
         * .setContext(context); ep.sendMessageAsync(cmd, caller);
         */
    }

    public Void deleteCallback(AsyncCallbackDispatcher<SamplePrimaryDataStoreDriverImpl, Answer> callback,
            AsyncRpcContext<CommandResult> context) {
        CommandResult result = new CommandResult();
        Answer answer = callback.getResult();
        if (!answer.getResult()) {
            result.setResult(answer.getDetails());
        }
        context.getParentCallback().complete(result);
        return null;
    }

    /*
     * private class CreateVolumeFromBaseImageContext<T> extends
     * AsyncRpcContext<T> { private final VolumeObject volume;
     * 
     * public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T>
     * callback, VolumeObject volume) { super(callback); this.volume = volume; }
     * 
     * public VolumeObject getVolume() { return this.volume; }
     * 
     * }
     * 
     * @Override public void createVolumeFromBaseImageAsync(VolumeObject volume,
     * TemplateInfo template, AsyncCompletionCallback<CommandResult> callback) {
     * VolumeTO vol = this.dataStore.getVolumeTO(volume); List<EndPoint>
     * endPoints = this.dataStore.getEndPoints(); EndPoint ep =
     * endPoints.get(0); String templateUri =
     * template.getDataStore().grantAccess(template, ep);
     * CreateVolumeFromBaseImageCommand cmd = new
     * CreateVolumeFromBaseImageCommand(vol, templateUri);
     * 
     * CreateVolumeFromBaseImageContext<CommandResult> context = new
     * CreateVolumeFromBaseImageContext<CommandResult>(callback, volume);
     * AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller
     * = AsyncCallbackDispatcher.create(this); caller.setContext(context)
     * .setCallback
     * (caller.getTarget().createVolumeFromBaseImageAsyncCallback(null, null));
     * 
     * ep.sendMessageAsync(cmd, caller); }
     */
    /*
     * public Object
     * createVolumeFromBaseImageAsyncCallback(AsyncCallbackDispatcher
     * <DefaultPrimaryDataStoreDriverImpl, Answer> callback,
     * CreateVolumeFromBaseImageContext<CommandResult> context) {
     * CreateVolumeAnswer answer = (CreateVolumeAnswer)callback.getResult();
     * CommandResult result = new CommandResult(); if (answer == null ||
     * answer.getDetails() != null) { result.setSuccess(false); if (answer !=
     * null) { result.setResult(answer.getDetails()); } } else {
     * result.setSuccess(true); VolumeObject volume = context.getVolume();
     * volume.setPath(answer.getVolumeUuid()); }
     * AsyncCompletionCallback<CommandResult> parentCall =
     * context.getParentCallback(); parentCall.complete(result); return null; }
     */

    @Override
    public void createAsync(DataStore dataStore, DataObject vol, AsyncCompletionCallback<CreateCmdResult> callback) {
        EndPoint ep = selector.select(vol);
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        CreateObjectCommand createCmd = new CreateObjectCommand(null);

        CreateVolumeContext<CreateCmdResult> context = new CreateVolumeContext<CreateCmdResult>(callback, vol);
        AsyncCallbackDispatcher<SamplePrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context).setCallback(caller.getTarget().createAsyncCallback(null, null));

        ep.sendMessageAsync(createCmd, caller);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
    }

}
