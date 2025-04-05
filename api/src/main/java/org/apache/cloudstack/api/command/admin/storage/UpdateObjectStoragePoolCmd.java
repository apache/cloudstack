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

import org.apache.cloudstack.storage.object.ObjectStore;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = UpdateObjectStoragePoolCmd.APINAME, description = "Updates object storage pool", responseObject = ObjectStoreResponse.class, entityType = {ObjectStore.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0")
public class UpdateObjectStoragePoolCmd extends BaseCmd {
    public static final String APINAME = "updateObjectStoragePool";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ObjectStoreResponse.class, required = true, description = "Object Store ID")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name for the object store")
    private String name;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "the object store provider name")
    private String providerName;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, description = "the url for the object store")
    private String url;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.MAP,
               description = "the details for the object store. Example: details[0].key=accesskey&details[0].value=s389ddssaa&details[1].key=secretkey&details[1].value=8dshfsss")
    private Map details;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getProviderName() {
        return providerName;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = null;
        if (details != null && !details.isEmpty()) {
            detailsMap = new HashMap<>();
            for (Object prop : details.values()) {
                HashMap<String, String> detail = (HashMap<String, String>) prop;
                String key = detail.get(ApiConstants.KEY);
                String value = detail.get(ApiConstants.VALUE);
                detailsMap.put(key, value);
            }
        }
        return detailsMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ObjectStore result = _storageService.updateObjectStore(getId(), this);

        ObjectStoreResponse storeResponse;
        if (result != null) {
            storeResponse = _responseGenerator.createObjectStoreResponse(result);
            storeResponse.setResponseName(getCommandName());
            storeResponse.setObjectName("objectstore");
            setResponseObject(storeResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update object storage");
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
