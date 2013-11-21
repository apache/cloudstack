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

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.DedicatedResources;
import com.cloud.serializer.Param;

@EntityReference(value = DedicatedResources.class)
public class DedicateZoneResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the dedicated resource")
    private String id;

    @SerializedName("zoneid")
    @Param(description = "the ID of the Zone")
    private String zoneId;

    @SerializedName("zonename")
    @Param(description = "the Name of the Zone")
    private String zoneName;

    @SerializedName("domainid")
    @Param(description = "the domain ID to which the Zone is dedicated")
    private String domainId;

    @SerializedName("accountid")
    @Param(description = "the Account Id to which the Zone is dedicated")
    private String accountId;

    @SerializedName("affinitygroupid")
    @Param(description = "the Dedication Affinity Group ID of the zone")
    private String affinityGroupId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAffinityGroupId() {
        return affinityGroupId;
    }

    public void setAffinityGroupId(String affinityGroupId) {
        this.affinityGroupId = affinityGroupId;
    }
}
