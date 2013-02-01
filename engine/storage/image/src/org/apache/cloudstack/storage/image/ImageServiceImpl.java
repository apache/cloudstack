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
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.fsm.NoTransitionException;

@Component
public class ImageServiceImpl implements ImageService {
    private static final Logger s_logger = Logger.getLogger(ImageServiceImpl.class);
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    
    class CreateTemplateContext<T> extends AsyncRpcConext<T> {
        final TemplateInfo srcTemplate;
        final TemplateInfo templateOnStore;
        final AsyncCallFuture<CommandResult> future;
        final ObjectInDataStoreVO obj;
        public CreateTemplateContext(AsyncCompletionCallback<T> callback, TemplateInfo srcTemplate,
                TemplateInfo templateOnStore,
                AsyncCallFuture<CommandResult> future,
                ObjectInDataStoreVO obj) {
            super(callback);
            this.srcTemplate = srcTemplate;
            this.templateOnStore = templateOnStore;
            this.future = future;
            this.obj = obj;
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
        
        ObjectInDataStoreVO obj = objectInDataStoreMgr.findObject(template.getId(), template.getType(), store.getId(), store.getRole());
        TemplateInfo templateOnStore = null;
        if (obj == null) {
            templateOnStore = (TemplateInfo)objectInDataStoreMgr.create(template, store);
            obj = objectInDataStoreMgr.findObject(template.getId(), template.getType(), store.getId(), store.getRole());
        } else {
            CommandResult result = new CommandResult();
            result.setResult("duplicate template on the storage");
            future.complete(result);
            return future;
        }
        
        try {
            objectInDataStoreMgr.update(obj, Event.CreateOnlyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("failed to transit", e);
            CommandResult result = new CommandResult();
            result.setResult(e.toString());
            future.complete(result);
            return future;
        }
        CreateTemplateContext<CommandResult> context = new CreateTemplateContext<CommandResult>(null, 
                template, templateOnStore,
                future,
                obj);
        AsyncCallbackDispatcher<ImageServiceImpl, CreateCmdResult> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createTemplateCallback(null, null))
        .setContext(context);
        store.getDriver().createAsync(templateOnStore, caller);
        return future;
    }
    
    protected Void createTemplateCallback(AsyncCallbackDispatcher<ImageServiceImpl, CreateCmdResult> callback, 
            CreateTemplateContext<CreateCmdResult> context) {
        
        TemplateInfo templateOnStore = context.templateOnStore;
        TemplateObject template = (TemplateObject)context.srcTemplate;
        AsyncCallFuture<CommandResult> future = context.future;
        CommandResult result = new CommandResult();
        
        CreateCmdResult callbackResult = callback.getResult();
        if (callbackResult.isFailed()) {
            try {
                objectInDataStoreMgr.update(templateOnStore, Event.OperationFailed);
            } catch (NoTransitionException e) {
                s_logger.debug("failed to transit state", e);
            }
            result.setResult(callbackResult.getResult());
            future.complete(result);
            return null;
        }
        ObjectInDataStoreVO obj = objectInDataStoreMgr.findObject(templateOnStore.getId(), templateOnStore.getType(), templateOnStore.getDataStore().getId(), templateOnStore.getDataStore().getRole());
        obj.setInstallPath(callbackResult.getPath());
        
        if (callbackResult.getSize() != null) {
            obj.setSize(callbackResult.getSize());
        }
        
        try {
            objectInDataStoreMgr.update(obj, Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to transit state", e);
            result.setResult(e.toString());
            future.complete(result);
            return null;
        }
        
        template.setImageStoreId(templateOnStore.getDataStore().getId());
        template.setSize(callbackResult.getSize());
        try {
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
}
