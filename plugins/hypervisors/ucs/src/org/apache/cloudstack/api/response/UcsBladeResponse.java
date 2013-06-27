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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.ucs.database.UcsBladeVO;
import com.google.gson.annotations.SerializedName;
@EntityReference(value=UcsBladeVO.class)
public class UcsBladeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ucs blade id")
    private String id;
    @SerializedName(ApiConstants.UCS_MANAGER_ID)
    @Param(description = "ucs manager id")
    private String ucsManagerId;
    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "cloudstack host id this blade associates to")
    private String hostId;
    @SerializedName(ApiConstants.UCS_BLADE_DN)
    @Param(description = "ucs blade dn")
    private String dn;
    @SerializedName(ApiConstants.UCS_PROFILE_DN)
    @Param(description = "associated ucs profile dn")
    private String associatedProfileDn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(String ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getAssociatedProfileDn() {
        return associatedProfileDn;
    }

    public void setAssociatedProfileDn(String associatedProfileDn) {
        this.associatedProfileDn = associatedProfileDn;
    }

}
