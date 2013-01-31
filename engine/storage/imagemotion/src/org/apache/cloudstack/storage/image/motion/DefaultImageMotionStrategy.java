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
package org.apache.cloudstack.storage.image.motion;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.command.CopyCmd;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;

import com.cloud.agent.api.Answer;

//At least one of datastore is coming from image store or image cache store

public class DefaultImageMotionStrategy implements ImageMotionStrategy {
    @Inject
    EndPointSelector selector;
    private class CreateTemplateContext<T> extends AsyncRpcConext<T> {
        private final TemplateOnPrimaryDataStoreInfo template;
        public CreateTemplateContext(AsyncCompletionCallback<T> callback, TemplateOnPrimaryDataStoreInfo template) {
            super(callback);
            this.template = template;
        }
        
        public TemplateOnPrimaryDataStoreInfo getTemplate() {
            return this.template;
        }
        
    }
/*
    @Override
    public void copyTemplateAsync(String destUri, String srcUri, EndPoint ep, AsyncCompletionCallback<CommandResult> callback) {

        CopyCmd copyCommand = new CopyCmd(destUri, srcUri);
        CreateTemplateContext<CommandResult> context = new CreateTemplateContext<CommandResult>(callback, null);
        AsyncCallbackDispatcher<DefaultImageMotionStrategy, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null))
            .setContext(context);
             
        ep.sendMessageAsync(copyCommand, caller);
    }
    
    public Object copyTemplateCallBack(AsyncCallbackDispatcher<DefaultImageMotionStrategy, Answer> callback, CreateTemplateContext<CommandResult> context) {
        AsyncCompletionCallback<CommandResult> parentCall = context.getParentCallback();
        CopyTemplateToPrimaryStorageAnswer answer = (CopyTemplateToPrimaryStorageAnswer)callback.getResult();
        CommandResult result = new CommandResult();
       
        if (!answer.getResult()) {
            result.setSucess(answer.getResult());
            result.setResult(answer.getDetails());
        } else {
            TemplateOnPrimaryDataStoreInfo templateStore = context.getTemplate();
            templateStore.setPath(answer.getPath());
            result.setSucess(true);
        }

        parentCall.complete(result);
        return null;
    }*/

    @Override
    public boolean canHandle(DataObject srcData, DataObject destData) {
        /*
        DataStore destStore = destData.getDataStore();
        DataStore srcStore = srcData.getDataStore();
        if (destStore.getRole() == DataStoreRole.Image || destStore.getRole() == DataStoreRole.ImageCache 
                || srcStore.getRole() == DataStoreRole.Image 
                || srcStore.getRole() == DataStoreRole.ImageCache) {
            return true;
        }*/
        return false;
    }

    @Override
    public Void copyAsync(DataObject srcData, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        DataStore destStore = destData.getDataStore();
        DataStore srcStore = srcData.getDataStore();
        EndPoint ep = selector.select(srcData, destData);
        CopyCommandResult result = new CopyCommandResult("");
        if (ep == null) {
            result.setResult("can't find end point");
            callback.complete(result);
            return null;
        }
        
        String srcUri = srcStore.getDriver().grantAccess(srcData, ep);
        String destUri = destStore.getDriver().grantAccess(destData, ep);
        CopyCmd cmd = new CopyCmd(srcUri, destUri);
        
        CreateTemplateContext<CopyCommandResult> context = new CreateTemplateContext<CopyCommandResult>(callback, null);
        AsyncCallbackDispatcher<DefaultImageMotionStrategy, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyAsyncCallback(null, null))
            .setContext(context);
             
        ep.sendMessageAsync(cmd, caller);
        return null;
    }
    
    protected Void copyAsyncCallback(AsyncCallbackDispatcher<DefaultImageMotionStrategy, Answer> callback, CreateTemplateContext<CopyCommandResult> context) {
        AsyncCompletionCallback<CopyCommandResult> parentCall = context.getParentCallback();
        Answer answer = (Answer)callback.getResult();
        if (!answer.getResult()) {
            CopyCommandResult result = new CopyCommandResult("");
            result.setResult(answer.getDetails());
            parentCall.complete(result);
        } else {
            CopyCmdAnswer ans = (CopyCmdAnswer)answer;
            CopyCommandResult result = new CopyCommandResult(ans.getPath());
            parentCall.complete(result);
        }
        return null;
        
    }

}
