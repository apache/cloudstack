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

import java.net.URISyntaxException;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.DecodedDataObject;
import com.cloud.utils.storage.encoding.Decoder;


public class DefaultPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);
    @Inject
    EndPointSelector selector;
    @Inject
    StoragePoolHostDao storeHostDao;
    @Inject
    DataObjectManager dataObjMgr;
    public DefaultPrimaryDataStoreDriverImpl() {
        
    }
    
    private class CreateVolumeContext<T> extends AsyncRpcConext<T> {
        private final DataObject volume;
        /**
         * @param callback
         */
        public CreateVolumeContext(AsyncCompletionCallback<T> callback, DataObject volume) {
            super(callback);
            this.volume = volume;
        }
        
        public DataObject getVolume() {
            return this.volume;
        }
        
    }
    
    public Void createAsyncCallback(AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> callback, CreateVolumeContext<CreateCmdResult> context) {
        CreateCmdResult result = null;
        CreateObjectAnswer volAnswer = (CreateObjectAnswer) callback.getResult();
        if (volAnswer.getResult()) {
            result = new CreateCmdResult(volAnswer.getPath(), volAnswer.getSize());
        } else {
            result = new CreateCmdResult("", null);
            result.setResult(volAnswer.getDetails());
        }
        
        context.getParentCallback().complete(result);
        return null;
    }
  
    @Override
    public void deleteAsync(DataObject vo, AsyncCompletionCallback<CommandResult> callback) {
        DeleteCommand cmd = new DeleteCommand(vo.getUri());
    
        EndPoint ep = selector.select(vo);
        AsyncRpcConext<CommandResult> context = new AsyncRpcConext<CommandResult>(callback);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteCallback(null, null))
            .setContext(context);
        ep.sendMessageAsync(cmd, caller);
    }
    
    public Void deleteCallback(AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> callback, AsyncRpcConext<CommandResult> context) {
        CommandResult result = new CommandResult();
        Answer answer = callback.getResult();
        if (!answer.getResult()) {
            result.setResult(answer.getDetails());
        }
        context.getParentCallback().complete(result);
        return null;
    }
    /*
    private class CreateVolumeFromBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject volume;
      
        public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T> callback, VolumeObject volume) {
            super(callback);
            this.volume = volume;
        }
        
        public VolumeObject getVolume() {
            return this.volume;
        }
        
    }

    @Override
    public void createVolumeFromBaseImageAsync(VolumeObject volume, TemplateInfo template, AsyncCompletionCallback<CommandResult> callback) {
        VolumeTO vol = this.dataStore.getVolumeTO(volume);
        List<EndPoint> endPoints = this.dataStore.getEndPoints();
        EndPoint ep = endPoints.get(0);
        String templateUri = template.getDataStore().grantAccess(template, ep);
        CreateVolumeFromBaseImageCommand cmd = new CreateVolumeFromBaseImageCommand(vol, templateUri);
        
        CreateVolumeFromBaseImageContext<CommandResult> context = new CreateVolumeFromBaseImageContext<CommandResult>(callback, volume);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context)
            .setCallback(caller.getTarget().createVolumeFromBaseImageAsyncCallback(null, null));

        ep.sendMessageAsync(cmd, caller);
    }*/
    /*
    public Object createVolumeFromBaseImageAsyncCallback(AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> callback, CreateVolumeFromBaseImageContext<CommandResult> context) {
        CreateVolumeAnswer answer = (CreateVolumeAnswer)callback.getResult();
        CommandResult result = new CommandResult();
        if (answer == null || answer.getDetails() != null) {
            result.setSucess(false);
            if (answer != null) {
                result.setResult(answer.getDetails());
            }
        } else {
            result.setSucess(true);
            VolumeObject volume = context.getVolume();
            volume.setPath(answer.getVolumeUuid());
        }
        AsyncCompletionCallback<CommandResult> parentCall = context.getParentCallback();
        parentCall.complete(result);
        return null;
    }*/

    @Override
    public void createAsync(DataObject vol,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        EndPoint ep = selector.select(vol);
        CreateObjectCommand createCmd = new CreateObjectCommand(vol.getUri());
        
        CreateVolumeContext<CreateCmdResult> context = new CreateVolumeContext<CreateCmdResult>(callback, vol);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context)
            .setCallback(caller.getTarget().createAsyncCallback(null, null));

        ep.sendMessageAsync(createCmd, caller);
    }

    @Override
    public String grantAccess(DataObject object, EndPoint ep) {
        //StoragePoolHostVO poolHost = storeHostDao.findByPoolHost(object.getDataStore().getId(), ep.getId());
        
        String uri = object.getUri();
        try {
            DecodedDataObject obj = Decoder.decode(uri);
            if (obj.getPath() == null) {
                //create an obj
                EndPoint newEp = selector.select(object);
                CreateObjectCommand createCmd = new CreateObjectCommand(uri);
                CreateObjectAnswer answer = (CreateObjectAnswer)ep.sendMessage(createCmd);
                if (answer.getResult()) {
                    dataObjMgr.update(object, answer.getPath(), answer.getSize());
                } else {
                    s_logger.debug("failed to create object" + answer.getDetails());
                    throw new CloudRuntimeException("failed to create object" + answer.getDetails());
                }
            }
            
            return object.getUri();
        } catch (URISyntaxException e) {
           throw new CloudRuntimeException("uri parsed error", e);
        }
    }

    @Override
    public boolean revokeAccess(DataObject vol, EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub
        
    }
   
}
