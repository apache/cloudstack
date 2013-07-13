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
package org.apache.cloudstack.storage.datastore.driver;

import java.util.Map;
import java.util.Timer;
import javax.inject.Inject;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.storage.download.DownloadListener;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.storage.upload.UploadListener;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.storage.Storage.ImageFormat;

public class SwiftImageStoreDriverImpl extends BaseImageStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(SwiftImageStoreDriverImpl.class);

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    StorageCacheManager cacheManager;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl imgStore = (ImageStoreImpl) store;
        Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new SwiftTO(imgStore.getId(), imgStore.getUri(), details.get(ApiConstants.ACCOUNT),
                details.get(ApiConstants.USERNAME), details.get(ApiConstants.KEY));
    }

    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format) {
        throw new UnsupportedServiceException("Extract entity url is not yet supported for Swift image store provider");
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        VirtualMachineTemplate tmpl = _templateDao.findById(data.getId());
        DataStore cacheStore = cacheManager.getCacheStorage(dataStore.getScope());
        DownloadCommand dcmd = new DownloadCommand((TemplateObjectTO)(data.getTO()), maxTemplateSizeInBytes);
        dcmd.setCacheStore(cacheStore.getTO());
        dcmd.setProxy(getHttpProxy());

        EndPoint ep = _epSelector.select(data);
        if (ep == null) {
            s_logger.warn("There is no secondary storage VM for downloading template to image store " + dataStore.getName());
            return;
        }

        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<SwiftImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setContext(context);

        if (data.getType() == DataObjectType.TEMPLATE) {
            caller.setCallback(caller.getTarget().createTemplateAsyncCallback(null, null));
        } else if (data.getType() == DataObjectType.VOLUME) {
            caller.setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));
        }
        ep.sendMessageAsync(dcmd, caller);


    }

}
