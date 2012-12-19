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

import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyTemplateToPrimaryStorageCmd;
import org.apache.cloudstack.storage.command.CopyTemplateToPrimaryStorageAnswer;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.springframework.stereotype.Component;

@Component
public class DefaultImageMotionStrategy implements ImageMotionStrategy {

    @Override
    public boolean canHandle(TemplateOnPrimaryDataStoreInfo templateStore) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public EndPoint getEndPoint(TemplateOnPrimaryDataStoreInfo templateStore) {
        PrimaryDataStore pdi = templateStore.getPrimaryDataStore();
        return pdi.getEndPoints().get(0);
    }

    @Override
    public boolean copyTemplate(TemplateOnPrimaryDataStoreInfo templateStore, EndPoint ep) {
        ImageOnPrimayDataStoreTO imageTo = new ImageOnPrimayDataStoreTO(templateStore);
        CopyTemplateToPrimaryStorageCmd copyCommand = new CopyTemplateToPrimaryStorageCmd(imageTo);
        ep.sendMessage(copyCommand);
        return true;
    }
    
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

    @Override
    public void copyTemplateAsync(TemplateOnPrimaryDataStoreInfo templateStore, EndPoint ep, AsyncCompletionCallback<CommandResult> callback) {
        ImageOnPrimayDataStoreTO imageTo = new ImageOnPrimayDataStoreTO(templateStore);
        CopyTemplateToPrimaryStorageCmd copyCommand = new CopyTemplateToPrimaryStorageCmd(imageTo);
        CreateTemplateContext<CommandResult> context = new CreateTemplateContext<CommandResult>(callback, templateStore);
        AsyncCallbackDispatcher<DefaultImageMotionStrategy> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null))
            .setContext(context);
             
        ep.sendMessageAsync(copyCommand, caller);
    }
    
    public Object copyTemplateCallBack(AsyncCallbackDispatcher<DefaultImageMotionStrategy> callback, CreateTemplateContext<CommandResult> context) {
        AsyncCompletionCallback<CommandResult> parentCall = context.getParentCallback();
        CopyTemplateToPrimaryStorageAnswer answer = callback.getResult();
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
    }

}
