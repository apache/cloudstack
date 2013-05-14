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
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.motion.DataMotionService;
import org.apache.cloudstack.storage.volume.VolumeServiceImpl.CreateBaseImageResult;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class TemplateInstallStrategyImpl implements TemplateInstallStrategy {
    private static final Logger s_logger = Logger
            .getLogger(TemplateInstallStrategyImpl.class);
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    DataMotionService motionSrv;
    @Inject
    ImageDataFactory imageFactory;
    protected long waitingTime = 1800; // half an hour
    protected long waitingRetries = 10;
/*
    protected TemplateInfo waitingForTemplateDownload(TemplateInfo template,
            PrimaryDataStore dataStore) {
        long retries = this.waitingRetries;
        ObjectInDataStoreVO obj = null;
        do {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                s_logger.debug("sleep interrupted", e);
                throw new CloudRuntimeException("sleep interrupted", e);
            }

            obj = objectInDataStoreMgr.findObject(template.getId(),
                    template.getType(), dataStore.getId(), dataStore.getRole());
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
            throw new CloudRuntimeException(
                    "waiting too long for template downloading, marked it as failed");
        }
        return imageFactory.getTemplate(template.getId(), dataStore);
    }

    class InstallContext<T> extends AsyncRpcConext<T> {
        final TemplateInfo destTemplate;
        final TemplateInfo srcTemplate;

        public InstallContext(AsyncCompletionCallback<T> callback,
                TemplateInfo destTemplate, TemplateInfo srcTemplate) {
            super(callback);
            this.destTemplate = destTemplate;
            this.srcTemplate = srcTemplate;
        }

    }

    @Override
    public Void installAsync(TemplateInfo template, PrimaryDataStore store,
            AsyncCompletionCallback<CreateBaseImageResult> callback) {
        ObjectInDataStoreVO obj = objectInDataStoreMgr.findObject(
                template.getId(), template.getType(), store.getId(),
                store.getRole());
        TemplateInfo templateOnPrimaryStoreObj = null;
        boolean freshNewTemplate = false;
        if (obj == null) {
            try {
                templateOnPrimaryStoreObj = objectInDataStoreMgr.create(
                        template, store);
                freshNewTemplate = true;
            } catch (Throwable e) {
                obj = objectInDataStoreMgr.findObject(template.getId(),
                        template.getType(), store.getId(), store.getRole());
                if (obj == null) {
                    CreateBaseImageResult result = new CreateBaseImageResult(
                            null);
                    result.setSuccess(false);
                    result.setResult(e.toString());
                    callback.complete(result);
                    return null;
                }
            }
        }

        if (!freshNewTemplate
                && obj.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            try {
                templateOnPrimaryStoreObj = waitingForTemplateDownload(
                        template, store);
            } catch (Exception e) {
                CreateBaseImageResult result = new CreateBaseImageResult(null);
                result.setSuccess(false);
                result.setResult(e.toString());
                callback.complete(result);
                return null;
            }

            CreateBaseImageResult result = new CreateBaseImageResult(
                    templateOnPrimaryStoreObj);
            callback.complete(result);
            return null;
        }

        try {
            objectInDataStoreMgr.update(templateOnPrimaryStoreObj,
                    ObjectInDataStoreStateMachine.Event.CreateRequested);
        } catch (NoTransitionException e) {
            try {
                objectInDataStoreMgr.update(templateOnPrimaryStoreObj,
                        ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("state transation failed", e1);
            }
            CreateBaseImageResult result = new CreateBaseImageResult(null);
            result.setSuccess(false);
            result.setResult(e.toString());
            callback.complete(result);
            return null;
        }

        InstallContext<CreateBaseImageResult> context = new InstallContext<CreateBaseImageResult>(
                callback, templateOnPrimaryStoreObj, template);
        AsyncCallbackDispatcher<TemplateInstallStrategyImpl, CreateCmdResult> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setCallback(
                caller.getTarget().installTemplateCallback(null, null))
                .setContext(context);

        store.getDriver().createAsync(templateOnPrimaryStoreObj, caller);
        return null;
    }

    class CopyTemplateContext<T> extends AsyncRpcConext<T> {
        TemplateInfo template;

        public CopyTemplateContext(AsyncCompletionCallback<T> callback,
                TemplateInfo template) {
            super(callback);
            this.template = template;
        }
    }

    protected Void installTemplateCallback(
            AsyncCallbackDispatcher<TemplateInstallStrategyImpl, CreateCmdResult> callback,
            InstallContext<CreateBaseImageResult> context) {
        CreateCmdResult result = callback.getResult();
        TemplateInfo templateOnPrimaryStoreObj = context.destTemplate;
        CreateBaseImageResult upResult = new CreateBaseImageResult(
                templateOnPrimaryStoreObj);
        if (result.isFailed()) {
            upResult.setResult(result.getResult());
            context.getParentCallback().complete(upResult);
            return null;
        }

        ObjectInDataStoreVO obj = objectInDataStoreMgr.findObject(
                templateOnPrimaryStoreObj.getId(), templateOnPrimaryStoreObj
                        .getType(), templateOnPrimaryStoreObj.getDataStore()
                        .getId(), templateOnPrimaryStoreObj.getDataStore()
                        .getRole());

        obj.setInstallPath(result.getPath());
        obj.setSize(result.getSize());
        try {
            objectInDataStoreMgr.update(obj,
                    ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            try {
                objectInDataStoreMgr.update(obj,
                        ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("failed to change state", e1);
            }

            upResult.setResult(e.toString());
            context.getParentCallback().complete(upResult);
            return null;
        }

        moveTemplate(context.srcTemplate, templateOnPrimaryStoreObj, obj,
                context.getParentCallback());
        return null;
    }

    protected void moveTemplate(TemplateInfo srcTemplate,
            TemplateInfo destTemplate, ObjectInDataStoreVO obj,
            AsyncCompletionCallback<CreateBaseImageResult> callback) {
        // move template into primary storage
        try {
            objectInDataStoreMgr.update(destTemplate,
                    ObjectInDataStoreStateMachine.Event.CopyingRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("failed to change state", e);
            try {
                objectInDataStoreMgr.update(destTemplate,
                        ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (NoTransitionException e1) {

            }
            CreateBaseImageResult res = new CreateBaseImageResult(destTemplate);
            res.setResult("Failed to change state: " + e.toString());
            callback.complete(res);
        }

        CopyTemplateContext<CreateBaseImageResult> anotherCall = new CopyTemplateContext<CreateBaseImageResult>(
                callback, destTemplate);
        AsyncCallbackDispatcher<TemplateInstallStrategyImpl, CopyCommandResult> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallback(null, null))
                .setContext(anotherCall);

        motionSrv.copyAsync(srcTemplate, destTemplate, caller);
    }

    protected Void copyTemplateCallback(
            AsyncCallbackDispatcher<TemplateInstallStrategyImpl, CopyCommandResult> callback,
            CopyTemplateContext<CreateBaseImageResult> context) {
        CopyCommandResult result = callback.getResult();
        TemplateInfo templateOnPrimaryStoreObj = context.template;
        if (result.isFailed()) {
            CreateBaseImageResult res = new CreateBaseImageResult(
                    templateOnPrimaryStoreObj);
            res.setResult(result.getResult());
            context.getParentCallback().complete(res);
        }
        DataObjectInStore obj = objectInDataStoreMgr.findObject(
                templateOnPrimaryStoreObj, templateOnPrimaryStoreObj.getDataStore());


        CreateBaseImageResult res = new CreateBaseImageResult(
                templateOnPrimaryStoreObj);
        try {
            objectInDataStoreMgr.update(obj,
                    ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to update copying state: ", e);
            try {
                objectInDataStoreMgr.update(templateOnPrimaryStoreObj,
                        ObjectInDataStoreStateMachine.Event.OperationFailed);
            } catch (NoTransitionException e1) {
            }

            res.setResult("Failed to update copying state: " + e.toString());
            context.getParentCallback().complete(res);
        }
        context.getParentCallback().complete(res);
        return null;
    }*/
    @Override
    public Void installAsync(TemplateInfo template, PrimaryDataStore store,
            AsyncCompletionCallback<CreateBaseImageResult> callback) {
        // TODO Auto-generated method stub
        return null;
    }

}
