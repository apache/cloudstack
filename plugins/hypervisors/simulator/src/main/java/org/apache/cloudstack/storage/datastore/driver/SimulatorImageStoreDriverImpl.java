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

import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.image.NfsImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;

public class SimulatorImageStoreDriverImpl extends NfsImageStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(SimulatorImageStoreDriverImpl.class);

    @Inject
    TemplateDataStoreDao _templateStoreDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    EndPointSelector _epSelector;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl nfsStore = (ImageStoreImpl)store;
        NfsTO nfsTO = new NfsTO();
        nfsTO.setRole(store.getRole());
        nfsTO.setUrl(nfsStore.getUri());
        nfsTO.setNfsVersion(getNfsVersion(nfsStore.getId()));
        return nfsTO;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        if (data.getType() == DataObjectType.TEMPLATE) {
            createTemplate(data, callback);
        } else if (data.getType() == DataObjectType.VOLUME) {
            createVolume(data, callback);
        }
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        callback.complete(new CommandResult());
    }

    protected void createTemplate(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<SimulatorImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context);
        caller.setCallback(caller.getTarget().createTemplateAsyncCallback(null, null));
        String path = UUID.randomUUID().toString();
        Long size = new Long(5 * 1024L * 1024L);
        DownloadAnswer answer = new DownloadAnswer(null, 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, path, path, size, size, null);
        caller.complete(answer);
        return;
    }

    protected void createVolume(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<SimulatorImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context);
        caller.setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));
        String path = UUID.randomUUID().toString();
        Long size = new Long(5 * 1024L * 1024L);
        DownloadAnswer answer = new DownloadAnswer(null, 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, path, path, size, size, null);
        caller.complete(answer);
        return;
    }

    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, Storage.ImageFormat format, DataObject dataObject) {
        EndPoint ep = _epSelector.select(store);
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            return null;
        }
        // Create Symlink at ssvm
        String path = installPath;
        String uuid = UUID.randomUUID().toString() + "." + format.getFileExtension();
        // Construct actual URL locally now that the symlink exists at SSVM
        return generateCopyUrl(ep.getPublicAddr(), uuid);
    }

    private String generateCopyUrl(String ipAddress, String uuid) {
        String hostname = ipAddress;
        String scheme = "http";
        return scheme + "://" + hostname + "/userdata/" + uuid;
    }
}
