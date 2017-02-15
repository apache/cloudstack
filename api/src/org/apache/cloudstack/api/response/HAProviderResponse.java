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

import java.util.List;

@EntityReference(value = HAConfig.class)
public final class HAProviderResponse extends BaseResponse {
    @SerializedName(ApiConstants.HA_PROVIDER)
    @Param(description = "the HA provider")
    private String provider;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the HA provider resource type detail")
    private List<String> supportedResourceTypes;

    public HAProviderResponse() {
        super("haprovider");
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getSupportedResourceTypes() {
        return supportedResourceTypes;
    }

    public void setSupportedResourceTypes(List<String> supportedResourceTypes) {
        this.supportedResourceTypes = supportedResourceTypes;
    }
}
