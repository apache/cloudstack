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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.ha.HAConfig;

@EntityReference(value = HAConfig.class)
public final class HostHAResponse extends BaseResponse {
    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the ID of the host")
    private String id;

    @SerializedName(ApiConstants.HA_ENABLE)
    @Param(description = "if host HA is enabled for the host")
    private Boolean enabled;

    @SerializedName(ApiConstants.HA_STATE)
    @Param(description = "the HA state of the host")
    private HAConfig.HAState haState;

    @SerializedName(ApiConstants.HA_PROVIDER)
    @Param(description = "the host HA provider")
    private String provider;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "operation status")
    private Boolean status;

    public HostHAResponse() {
        super("hostha");
    }

    public HostHAResponse(final HAConfig config) {
        this();
        if (config == null) {
            this.enabled = false;
            this.haState = HAConfig.HAState.Disabled;
            return;
        }
        setProvider(config.getHaProvider());
        setEnabled(config.isEnabled());
        setHaState(config.getState());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public HAConfig.HAState getHaState() {
        return haState;
    }

    public void setHaState(HAConfig.HAState haState) {
        this.haState = haState;
        if (haState == null) {
            this.haState = HAConfig.HAState.Disabled;
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
