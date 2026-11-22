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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.mcp.McpToken;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = McpToken.class)
public class McpTokenResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the MCP token.")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the MCP token.")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the MCP token.")
    private String description;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "ID of the user that owns the MCP token.")
    private String userId;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "Username of the owner of the MCP token.")
    private String username;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Expiration date of the MCP token.")
    private Date endDate;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation date of the MCP token.")
    private Date created;

    @SerializedName(ApiConstants.TOKEN)
    @Param(description = "The MCP token. Only returned when the token is created.", isSensitive = true)
    private String token;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
