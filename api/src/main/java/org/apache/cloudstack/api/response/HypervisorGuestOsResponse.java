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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class HypervisorGuestOsResponse extends BaseResponse {
    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "standard display name for the Guest OS")
    private String osStdName;

    @SerializedName(ApiConstants.OS_NAME_FOR_HYPERVISOR)
    @Param(description = "hypervisor specific name for the Guest OS")
    private String osNameForHypervisor;

    public String getOsStdName() {
        return osStdName;
    }

    public void setOsStdName(String osStdName) {
        this.osStdName = osStdName;
    }

    public String getOsNameForHypervisor() {
        return osNameForHypervisor;
    }

    public void setOsNameForHypervisor(String osNameForHypervisor) {
        this.osNameForHypervisor = osNameForHypervisor;
    }
}
