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
package org.apache.cloudstack.storage.image;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageService;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateEvent;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.fsm.NoTransitionException;

@Component
public class ImageServiceImpl implements ImageService {
    private static final Logger s_logger = Logger.getLogger(ImageServiceImpl.class);
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    DataObjectManager dataObjectMgr;
    
    class CreateTemplateContext<T> extends AsyncRpcConext<T> {
        final TemplateInfo srcTemplate;
        final DataStore store;
        final AsyncCallFuture<CommandResult> future;
        final DataObject templateOnStore;

        public CreateTemplateContext(AsyncCompletionCallback<T> callback, TemplateInfo srcTemplate,
                AsyncCallFuture<CommandResult> future,
                DataStore store,
                DataObject templateOnStore
             ) {
            super(callback);
            this.srcTemplate = srcTemplate;
            this.future = future;
            this.store = store;
            this.templateOnStore = templateOnStore;
        }
    }
    
    @Override
    public AsyncCallFuture<CommandResult> createTemplateAsync(
            TemplateInfo template, DataStore store) {
        TemplateObject to = (TemplateObject) template;
        AsyncCallFuture<CommandResult> future = new AsyncCallFuture<CommandResult>();
        try {
            to.stateTransit(TemplateEvent.CreateRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to transit state:", e);
            CommandResult result = new CommandResult();
            result.setResult(e.toString());
            future.complete(result);
            return future;
        }
        
        DataObject templateOnStore = store.create(template);
        templateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);
        
        CreateTemplateContext<CommandResult> context = new CreateTemplateContext<CommandResult>(null, 
                template,
                future,
                store,
                templateOnStore
               );
        AsyncCallbackDispatcher<ImageServiceImpl, CreateCmdResult> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createTemplateCallback(null, null))
        .setContext(context);
        store.getDriver().createAsync(templateOnStore, caller);
        return future;
    }
    
    protected Void createTemplateCallback(AsyncCallbackDispatcher<ImageServiceImpl, CreateCmdResult> callback, 
            CreateTemplateContext<CreateCmdResult> context) {
        TemplateObject template = (TemplateObject)context.srcTemplate;
        AsyncCallFuture<CommandResult> future = context.future;
        CommandResult result = new CommandResult();
        DataObject templateOnStore = context.templateOnStore;
        CreateCmdResult callbackResult = callback.getResult();
        if (callbackResult.isFailed()) {
            try {
                templateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                template.stateTransit(TemplateEvent.OperationFailed);
            } catch (NoTransitionException e) {
               s_logger.debug("Failed to update template state", e);
            }
            result.setResult(callbackResult.getResult());
            future.complete(result);
            return null;
        }
        
        try {
            templateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
            template.stateTransit(TemplateEvent.OperationSucceeded);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to transit state", e);
            result.setResult(e.toString());
            future.complete(result);
            return null;
        }
        
        future.complete(result);
        return null;
    }

    @Override
    public AsyncCallFuture<CommandResult> deleteTemplateAsync(
            TemplateInfo template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncCallFuture<CommandResult> createTemplateFromSnapshotAsync(
            SnapshotInfo snapshot, TemplateInfo template, DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncCallFuture<CommandResult> createTemplateFromVolumeAsync(
            VolumeInfo volume, TemplateInfo template, DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }
}
