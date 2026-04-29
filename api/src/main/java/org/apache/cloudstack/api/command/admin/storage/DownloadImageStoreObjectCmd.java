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
package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.browser.StorageBrowser;

import javax.inject.Inject;
import java.nio.file.Path;

@APICommand(name = "downloadImageStoreObject", description = "Download object at a specified path on an image store.",
            responseObject = ExtractResponse.class, since = "4.19.0", requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class DownloadImageStoreObjectCmd extends BaseAsyncCmd {

    @Inject
    StorageBrowser storageBrowser;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ImageStoreResponse.class, required = true,
               description = "id of the image store")
    private Long storeId;

    @Parameter(name = ApiConstants.PATH, type = CommandType.STRING, description = "path to download on image store")
    private String path;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getStoreId() {
        return storeId;
    }

    public String getPath() {
        if (path == null) {
            path = "/";
        }
        // We prepend "/" to path and normalize to prevent path traversal attacks
        return Path.of(String.format("/%s", path)).normalize().toString().substring(1);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ExtractResponse response = storageBrowser.downloadImageStoreObject(this);
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }

    /**
     * For commands the API framework needs to know the owner of the object being acted upon. This method is
     * used to determine that information.
     *
     * @return the id of the account that owns the object being acted upon
     */
    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IMAGE_STORE_OBJECT_DOWNLOAD;
    }

    @Override
    public String getEventDescription() {
        return "Downloading object at path " + getPath() + " on image store " + getStoreId();
    }
}
