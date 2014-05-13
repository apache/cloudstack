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
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.GuestOSHypervisor;

@EntityReference(value = GuestOSHypervisor.class)
public class GuestOsMappingResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the Guest OS mapping")
    private String id;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "the hypervisor")
    private String hypervisor;

    @SerializedName(ApiConstants.HYPERVISOR_VERSION)
    @Param(description = "version of the hypervisor for mapping")
    private String hypervisorVersion;

    @SerializedName(ApiConstants.OS_TYPE_ID)
    @Param(description = "the ID of the Guest OS type")
    private String osTypeId;

    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "standard display name for the Guest OS")
    private String osStdName;

    @SerializedName(ApiConstants.OS_NAME_FOR_HYPERVISOR)
    @Param(description = "hypervisor specific name for the Guest OS")
    private String osNameForHypervisor;

    @SerializedName(ApiConstants.IS_USER_DEFINED)
    @Param(description = "is the mapping user defined")
    private String isUserDefined;

    public String getIsUserDefined() {
        return isUserDefined;
    }

    public void setIsUserDefined(String isUserDefined) {
        this.isUserDefined = isUserDefined;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getOsTypeId() {
        return osTypeId;
    }

    public void setOsTypeId(String osTypeId) {
        this.osTypeId = osTypeId;
    }

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
