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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.storage.ObjectStore;
import com.cloud.user.Account;

@APICommand(name = "addSecondaryStorage", description="Adds secondary storage.", responseObject=HostResponse.class)
public class AddSecondaryStorageCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSecondaryStorageCmd.class.getName());
    private static final String s_name = "addsecondarystorageresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL for the secondary storage")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class,
            description="the Zone ID for the secondary storage")
    private Long zoneId;

    @Parameter(name=ApiConstants.REGION_ID, type=CommandType.UUID, entityType=RegionResponse.class,
            description="the Region ID for the secondary storage")
    private Long regionId;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.MAP, description="the details for the secondary storage data store")
    private Map details;

    @Parameter(name=ApiConstants.SCOPE, type=CommandType.STRING,
            required=false, description="the scope of the secondary storage data store: zone or region or global")
    private String scope;

    @Parameter(name=ApiConstants.PROVIDER, type=CommandType.STRING,
            required=false, description="the secondary storage store provider name")
    private String imageProviderName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public Map getDetails() {
        return details;
    }

    public String getScope() {
        return this.scope;
     }

    public String getImageProviderName() {
        return this.imageProviderName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        try{
            ObjectStore result = _resourceService.discoverObjectStore(this);
            ObjectStoreResponse storeResponse = null;
            if (result != null ) {
                    storeResponse = _responseGenerator.createObjectStoreResponse(result);
                    storeResponse.setResponseName(getCommandName());
                    storeResponse.setObjectName("secondarystorage");
                    this.setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add secondary storage");
            }
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
