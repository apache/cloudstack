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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.storage.ImageStore;

@APICommand(name = UpdateImageStoreCmd.APINAME, description = "Updates image store read-only status", responseObject = ImageStoreResponse.class, entityType = {ImageStore.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.15.0")
public class UpdateImageStoreCmd extends BaseCmd {
    private static final Logger LOG = Logger.getLogger(UpdateImageStoreCmd.class.getName());
    public static final String APINAME = "updateImageStore";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ImageStoreResponse.class, required = true, description = "Image Store UUID")
    private Long id;

    @Parameter(name = ApiConstants.READ_ONLY, type = CommandType.BOOLEAN, required = true, description = "If set to true, it designates the corresponding image store to read-only, " +
            "hence not considering them during storage migration")
    private Boolean readonly;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ImageStore result = _storageService.updateImageStoreStatus(getId(), getReadonly());
        ImageStoreResponse storeResponse = null;
        if (result != null) {
            storeResponse = _responseGenerator.createImageStoreResponse(result);
            storeResponse.setResponseName(getCommandName()+"response");
            storeResponse.setObjectName("imagestore");
            setResponseObject(storeResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Image store status");
        }

    }

    @Override
    public String getCommandName() {
        return APINAME;
    }


    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
