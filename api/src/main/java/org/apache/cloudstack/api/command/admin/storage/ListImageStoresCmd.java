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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listImageStores", description = "Lists image stores.", responseObject = ImageStoreResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListImageStoresCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListImageStoresCmd.class.getName());

    private static final String s_name = "listimagestoresresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the image store")
    private String storeName;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "the image store protocol")
    private String protocol;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "the image store provider")
    private String provider;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID for the image store")
    private Long zoneId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ImageStoreResponse.class, description = "the ID of the storage pool")
    private Long id;

    @Parameter(name = ApiConstants.READ_ONLY, type = CommandType.BOOLEAN, entityType = ImageStoreResponse.class, description = "read-only status of the image store", since = "4.15.0")
    private Boolean readonly;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getProtocol() {
        return protocol;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        ListResponse<ImageStoreResponse> response = _queryService.searchForImageStores(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
