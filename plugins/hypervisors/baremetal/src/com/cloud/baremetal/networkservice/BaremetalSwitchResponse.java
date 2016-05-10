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
//
package com.cloud.baremetal.networkservice;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.baremetal.database.BaremetalSwitchVO;
import com.cloud.serializer.Param;

/**
 * @author fridvin
 */
@EntityReference(value = BaremetalSwitchVO.class)
public class BaremetalSwitchResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of baremetal switch")
    private String id;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the IP address of the baremetal switch")
    private String ip;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "the username for the baremetal switch")
    private String username;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "type of baremetal switch")
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
