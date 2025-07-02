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

package org.apache.cloudstack.logsws.api.response;


import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.logsws.LogsWebSession;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {LogsWebSession.class})
public class LogsWebSessionResponse extends BaseResponse implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the logs web session")
    private String id;

    @SerializedName(ApiConstants.FILTERS)
    @Param(description = "The filters for the logs web session")
    private List<String> filters;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The ID of the domain of the logs web session creator")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The name of the domain of the logs web session creator")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "The path of the domain of the logs web session creator")
    private String domainPath;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The account which created the logs web session")
    private String accountName;

    @SerializedName(ApiConstants.CREATOR_ADDRESS)
    @Param(description = "The address of creator for this logs web session")
    private String creatorAddress;

    @SerializedName(ApiConstants.CONNECTED)
    @Param(description = "The number of clients connected for this logs web session")
    private Integer connected;

    @SerializedName(ApiConstants.CLIENT_ADDRESS)
    @Param(description = "The address of the last connected client for this logs web session")
    private String clientAddress;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date when this logs web session was created")
    private Date created;

    @SerializedName(ApiConstants.WEBSOCKET)
    @Param(description = "The logs web session websocket options")
    private Set<LogsWebSessionWebSocketResponse> websocketResponses;

    public void setId(String id) {
        this.id = id;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
    }

    @Override
    public void setProjectName(String projectName) {
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public void setConnected(Integer connected) {
        this.connected = connected;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setWebsocketResponse(Set<LogsWebSessionWebSocketResponse> websocketResponse) {
        this.websocketResponses = websocketResponse;
    }
}
