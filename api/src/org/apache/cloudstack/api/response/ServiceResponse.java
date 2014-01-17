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

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
public class ServiceResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the service name")
    private String name;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "the service provider name", responseObject = ProviderResponse.class)
    private List<ProviderResponse> providers;

    @SerializedName("capability")
    @Param(description = "the list of capabilities", responseObject = CapabilityResponse.class)
    private List<CapabilityResponse> capabilities;

    public void setName(String name) {
        this.name = name;
    }

    public void setCapabilities(List<CapabilityResponse> capabilities) {
        this.capabilities = capabilities;
    }

    public void setProviders(List<ProviderResponse> providers) {
        this.providers = providers;
    }
}
