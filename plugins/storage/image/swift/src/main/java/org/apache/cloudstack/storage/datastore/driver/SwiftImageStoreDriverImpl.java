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

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.cloud.configuration.Config;
import com.cloud.utils.SwiftUtil;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.to.TemplateObjectTO;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;

public class SwiftImageStoreDriverImpl extends BaseImageStoreDriverImpl {

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    StorageCacheManager cacheManager;
    @Inject
    ConfigurationDao _configDao;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl imgStore = (ImageStoreImpl)store;
        Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new SwiftTO(imgStore.getId(), imgStore.getUri(), details.get(ApiConstants.ACCOUNT), details.get(ApiConstants.USERNAME), details.get(ApiConstants.KEY), details.get(ApiConstants.STORAGE_POLICY));
    }

    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format, DataObject dataObject) {

        SwiftTO swiftTO = (SwiftTO)store.getTO();
        String tempKey = UUID.randomUUID().toString();
        boolean result = SwiftUtil.setTempKey(swiftTO, tempKey);

        if (!result) {
            String errMsg = "Unable to set Temp-Key: " + tempKey;
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        String containerName = SwiftUtil.getContainerName(dataObject.getType().toString(), dataObject.getId());
        String objectName = installPath.split("\\/")[1];
        // Get extract url expiration interval set in global configuration (in seconds)
        int urlExpirationInterval = Integer.parseInt(_configDao.getValue(Config.ExtractURLExpirationInterval.toString()));

        URL swiftUrl = SwiftUtil.generateTempUrl(swiftTO, containerName, objectName, tempKey, urlExpirationInterval);
        if (swiftUrl != null) {
            logger.debug("Swift temp-url: " + swiftUrl.toString());
            return swiftUrl.toString();
        }

        throw new CloudRuntimeException("Unable to create extraction URL");
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
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<SwiftImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context);

        if (data.getType() == DataObjectType.TEMPLATE) {
            caller.setCallback(caller.getTarget().createTemplateAsyncCallback(null, null));
        } else if (data.getType() == DataObjectType.VOLUME) {
            caller.setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));
        }
        ep.sendMessageAsync(dcmd, caller);

    }

}
