//Licensed to the Apache Software Foundation (ASF) under one or more

//contributor license agreements.  See the NOTICE file distributed with
//this work for additional information regarding copyright ownership.
//The ASF licenses this file to You under the Apache License, Version 2.0
//(the "License"); you may not use this file except in compliance with
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.serializer.Param;

public class NetscalerControlCenterResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "id")
    private String id;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "username")
    private String username;

    @SerializedName(ApiConstants.UUID)
    @Param(description = "uuid")
    private String uuid;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "ncc_ip")
    private String nccip;

    @SerializedName(ApiConstants.NUM_RETRIES)
    @Param(description = "num_retries")
    private String numretries;

    public NetscalerControlCenterResponse() {
    }

    public NetscalerControlCenterResponse(NetScalerControlCenterVO controlcenter) {
        this.id = controlcenter.getUuid();
        this.username = controlcenter.getUsername();
        this.uuid = controlcenter.getUuid();
        this.username = controlcenter.getUsername();
        this.nccip = controlcenter.getNccip();
        this.numretries = String.valueOf(controlcenter.getNumRetries());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNccip() {
        return nccip;
    }

    public void setNccip(String nccip) {
        this.nccip = nccip;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNumRetries() {
        return numretries;
    }

    public void setNumRetries(String numRetries) {
        this.numretries = numRetries;
    }
}
