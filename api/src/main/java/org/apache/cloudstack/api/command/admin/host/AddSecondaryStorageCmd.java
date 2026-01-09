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
package org.apache.cloudstack.api.command.admin.host;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.storage.ImageStore;
import com.cloud.user.Account;
import org.apache.commons.collections.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@APICommand(name = "addSecondaryStorage", description = "Adds secondary storage.", responseObject = ImageStoreResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddSecondaryStorageCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "The URL for the secondary storage")
    protected String url;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "The Zone ID for the secondary storage")
    protected Long zoneId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].copytemplatesfromothersecondarystorages=true")
    protected Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = new HashMap<>();
        if (MapUtils.isNotEmpty(details)) {
            Collection<?> props = details.values();
            for (Object prop : props) {
                HashMap<String, String> detail = (HashMap<String, String>) prop;
                for (Map.Entry<String, String> entry: detail.entrySet()) {
                    detailsMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return detailsMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        try{
            ImageStore result = _storageService.discoverImageStore(null, getUrl(), "NFS", getZoneId(), getDetails());
            ImageStoreResponse storeResponse = null;
            if (result != null ) {
                    storeResponse = _responseGenerator.createImageStoreResponse(result);
                    storeResponse.setResponseName(getCommandName());
                    storeResponse.setObjectName("secondarystorage");
                    setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add secondary storage");
            }
        } catch (DiscoveryException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
