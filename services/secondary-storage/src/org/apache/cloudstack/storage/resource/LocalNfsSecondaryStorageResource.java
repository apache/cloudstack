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
package org.apache.cloudstack.storage.resource;

import static com.cloud.utils.StringUtils.join;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.configuration.Config;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.apache.cloudstack.storage.template.DownloadManagerImpl.ZfsPathParser;

import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Component
public class LocalNfsSecondaryStorageResource extends NfsSecondaryStorageResource {

    private static final Logger s_logger = Logger.getLogger(LocalNfsSecondaryStorageResource.class);

    public LocalNfsSecondaryStorageResource() {
        this._dlMgr = new DownloadManagerImpl();
        ((DownloadManagerImpl) _dlMgr).setThreadPool(Executors.newFixedThreadPool(10));
        _storage = new JavaStorageLayer();
        this._inSystemVM = false;
    }

    public void setParentPath(String path) {
        this._parent = path;
    }

    @Override
    public Answer executeRequest(Command cmd) {
         return super.executeRequest(cmd);
    }

    @Override
    synchronized public String getRootDir(String secUrl) {
        try {
            URI uri = new URI(secUrl);
            String dir = mountUri(uri);
            return _parent + "/" + dir;
        } catch (Exception e) {
            String msg = "GetRootDir for " + secUrl + " failed due to " + e.toString();
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    protected void mount(String localRootPath, String remoteDevice, URI uri) {
        ensureLocalRootPathExists(localRootPath, uri);
        
        if (mountExists(localRootPath, uri)) {
            return;
        }

        attemptMount(localRootPath, remoteDevice, uri);

        // Change permissions for the mountpoint - seems to bypass authentication
        Script script = new Script(true, "chmod", _timeout, s_logger);
        script.add("777", localRootPath);
        String result = script.execute();
        if (result != null) {
            String errMsg = "Unable to set permissions for " + localRootPath + " due to " + result;
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        s_logger.debug("Successfully set 777 permission for " + localRootPath);

        // XXX: Adding the check for creation of snapshots dir here. Might have
        // to move it somewhere more logical later.
        checkForSnapshotsDir(localRootPath);
        checkForVolumesDir(localRootPath);
    }

}
