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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateVolumeAnswer;
import org.apache.cloudstack.storage.command.CreateVolumeCommand;
import org.apache.cloudstack.storage.command.CreateVolumeFromBaseImageCommand;
import org.apache.cloudstack.storage.command.DeleteVolumeCommand;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);
    protected PrimaryDataStore dataStore;
    public DefaultPrimaryDataStoreDriverImpl(PrimaryDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    public DefaultPrimaryDataStoreDriverImpl() {
        
    }
    
    @Override
    public void setDataStore(PrimaryDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    private class CreateVolumeContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject volume;
        /**
         * @param callback
         */
        public CreateVolumeContext(AsyncCompletionCallback<T> callback, VolumeObject volume) {
            super(callback);
            this.volume = volume;
        }
        
        public VolumeObject getVolume() {
            return this.volume;
        }
        
    }
    
    @Override
    public void createVolumeAsync(VolumeObject vol, AsyncCompletionCallback<CommandResult> callback) {
        List<EndPoint> endPoints = vol.getDataStore().getEndPoints();
        EndPoint ep = endPoints.get(0);
        VolumeInfo volInfo = vol;
        CreateVolumeCommand createCmd = new CreateVolumeCommand(this.dataStore.getVolumeTO(volInfo));
        
        CreateVolumeContext<CommandResult> context = new CreateVolumeContext<CommandResult>(callback, vol);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context)
            .setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));

        ep.sendMessageAsync(createCmd, caller);
    }
    
    public Void createVolumeAsyncCallback(AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> callback, CreateVolumeContext<CommandResult> context) {
        CommandResult result = new CommandResult();
        CreateVolumeAnswer volAnswer = (CreateVolumeAnswer) callback.getResult();
        if (volAnswer.getResult()) {
            VolumeObject volume = context.getVolume();
            volume.setPath(volAnswer.getVolumeUuid());
        } else {
            result.setResult(volAnswer.getDetails());
        }
        
        context.getParentCallback().complete(result);
        return null;
    }
  
    @Override
    public void deleteVolumeAsync(VolumeObject vo, AsyncCompletionCallback<CommandResult> callback) {
        DeleteVolumeCommand cmd = new DeleteVolumeCommand(this.dataStore.getVolumeTO(vo));
        List<EndPoint> endPoints = vo.getDataStore().getEndPoints();
        EndPoint ep = endPoints.get(0);
        AsyncRpcConext<CommandResult> context = new AsyncRpcConext<CommandResult>(callback);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteVolumeCallback(null, null))
            .setContext(context);
        ep.sendMessageAsync(cmd, caller);
    }
    
    public Void deleteVolumeCallback(AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> callback, AsyncRpcConext<CommandResult> context) {
        CommandResult result = new CommandResult();
        Answer answer = callback.getResult();
        if (!answer.getResult()) {
            result.setResult(answer.getDetails());
        }
        context.getParentCallback().complete(result);
        return null;
    }

    @Override
    public String grantAccess(VolumeObject vol, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAccess(VolumeObject vol, EndPoint ep) {
        // TODO Auto-generated method stub
        return true;
    }
    
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
    public void createVolumeFromBaseImageAsync(VolumeObject volume, String template, AsyncCompletionCallback<CommandResult> callback) {
        VolumeTO vol = this.dataStore.getVolumeTO(volume);
        CreateVolumeFromBaseImageCommand cmd = new CreateVolumeFromBaseImageCommand(vol, template);
        List<EndPoint> endPoints = this.dataStore.getEndPoints();
        EndPoint ep = endPoints.get(0);
        
        CreateVolumeFromBaseImageContext<CommandResult> context = new CreateVolumeFromBaseImageContext<CommandResult>(callback, volume);
        AsyncCallbackDispatcher<DefaultPrimaryDataStoreDriverImpl, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context)
            .setCallback(caller.getTarget().createVolumeFromBaseImageAsyncCallback(null, null));

        ep.sendMessageAsync(cmd, caller);
        
       
    }
    
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
    }

    @Override
    public long getCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean initialize(Map<String, String> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean grantAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean revokeAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

   
}
