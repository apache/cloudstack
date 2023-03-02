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
package org.apache.cloudstack.api.command.admin.storage;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.storage.ImageStore;
import com.cloud.user.Account;

@APICommand(name = "createSecondaryStagingStore", description = "create secondary staging store.", responseObject = ImageStoreResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateSecondaryStagingStoreCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSecondaryStagingStoreCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, length = 2048, description = "the URL for the staging store")
    private String url;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID for the staging store")
    private Long zoneId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "the details for the staging store")
    private Map<String, ? extends Map<String, String>> details;

    @Parameter(name = ApiConstants.SCOPE, type = CommandType.STRING, required = false, description = "the scope of the staging store: zone only for now")
    private String scope;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, required = false, description = "the staging store provider name")
    private String providerName;

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
        Map<String, String> detailsMap = null;
        if (details != null && !details.isEmpty()) {
            detailsMap = new HashMap<String, String>();
            for (Map<String, String> detail : details.values()) {
                detailsMap.put(detail.get("key"), detail.get("value"));
            }
        }
        return detailsMap;
    }

    public String getScope() {
        return this.scope;
    }

    public String getProviderName() {
        return this.providerName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        try {
            ImageStore result = _storageService.createSecondaryStagingStore(this);
            ImageStoreResponse storeResponse = null;
            if (result != null) {
                storeResponse = _responseGenerator.createImageStoreResponse(result);
                storeResponse.setResponseName(getCommandName());
                storeResponse.setObjectName("secondarystorage");
                this.setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add secondary storage");
            }
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
