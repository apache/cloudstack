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

import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Upload;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.image.NfsImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.configuration.Config;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudStackImageStoreDriverImpl extends NfsImageStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(CloudStackImageStoreDriverImpl.class);

    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostDao _hostDao;
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
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format, DataObject dataObject) {
        // find an endpoint to send command
        EndPoint ep = _epSelector.select(store);
        // Create Symlink at ssvm
        String path = installPath;
        String uuid = UUID.randomUUID().toString() + "." + format.getFileExtension();
        CreateEntityDownloadURLCommand cmd = new CreateEntityDownloadURLCommand(((ImageStoreEntity)store).getMountPoint(),
                                                                path, uuid, dataObject == null ? null: dataObject.getTO());
        Answer ans = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            ans = new Answer(cmd, false, errMsg);
        } else {
            ans = ep.sendMessage(cmd);
        }
        if (ans == null || !ans.getResult()) {
            String errorString = "Unable to create a link for entity at " + installPath + " on ssvm," + ans.getDetails();
            s_logger.error(errorString);
            throw new CloudRuntimeException(errorString);
        }
        // Construct actual URL locally now that the symlink exists at SSVM
        return generateCopyUrl(ep.getPublicAddr(), uuid);
    }

    private String generateCopyUrl(String ipAddress, String uuid) {

        String hostname = ipAddress;
        String scheme = "http";
        boolean _sslCopy = false;
        String sslCfg = _configDao.getValue(Config.SecStorageEncryptCopy.toString());
        String _ssvmUrlDomain = _configDao.getValue("secstorage.ssl.cert.domain");
        if (sslCfg != null) {
            _sslCopy = Boolean.parseBoolean(sslCfg);
        }
        if(_sslCopy && (_ssvmUrlDomain == null || _ssvmUrlDomain.isEmpty())){
            s_logger.warn("Empty secondary storage url domain, ignoring SSL");
            _sslCopy = false;
        }
        if (_sslCopy) {
            if(_ssvmUrlDomain.startsWith("*")) {
                hostname = ipAddress.replace(".", "-");
                hostname = hostname + _ssvmUrlDomain.substring(1);
            } else {
                hostname = _ssvmUrlDomain;
            }
            scheme = "https";
        }
        return scheme + "://" + hostname + "/userdata/" + uuid;
    }

    @Override
    public void deleteEntityExtractUrl(DataStore store, String installPath, String downloadUrl, Upload.Type entityType) {
        // find an endpoint to send command based on the ssvm on which the url was created.
        EndPoint ep = _epSelector.select(store, downloadUrl);

        // Delete Symlink at ssvm. In case of volume also delete the volume.
        DeleteEntityDownloadURLCommand cmd = new DeleteEntityDownloadURLCommand(installPath, entityType, downloadUrl, ((ImageStoreEntity) store).getMountPoint());

        Answer ans = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            ans = new Answer(cmd, false, errMsg);
        } else {
            ans = ep.sendMessage(cmd);
        }
        if (ans == null || !ans.getResult()) {
            String errorString = "Unable to delete the url " + downloadUrl + " for path " + installPath + " on ssvm, " + ans.getDetails();
            s_logger.error(errorString);
            throw new CloudRuntimeException(errorString);
        }

    }

}
