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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class HypervisorGuestOsNamesResponse extends BaseResponse {
    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "the hypervisor")
    private String hypervisor;

    @SerializedName(ApiConstants.HYPERVISOR_VERSION)
    @Param(description = "version of the hypervisor for guest os names")
    private String hypervisorVersion;

    @SerializedName(ApiConstants.GUEST_OSES)
    @Param(description = "the guest OSes of the hypervisor", responseObject = HypervisorGuestOsResponse.class)
    private List<HypervisorGuestOsResponse> guestOSes;

    @SerializedName(ApiConstants.GUEST_OSES_COUNT)
    @Param(description = "the count of guest OSes of the hypervisor")
    private Integer guestOSesCount;

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public List<HypervisorGuestOsResponse> getGuestOSes() {
        return guestOSes;
    }

    public void setGuestOSes(List<HypervisorGuestOsResponse> guestOSes) {
        this.guestOSes = guestOSes;
    }

    public Integer getGuestOSesCount() {
        return guestOSesCount;
    }

    public void setGuestOSesCount(Integer guestOSesCount) {
        this.guestOSesCount = guestOSesCount;
    }
}
