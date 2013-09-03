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

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DedicateHostResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the dedicated resource")
    private String id;

    @SerializedName("hostid") @Param(description="the ID of the host")
    private String hostId;

    @SerializedName("hostname") @Param(description="the name of the host")
    private String hostName;

    @SerializedName("domainid") @Param(description="the domain ID of the host")
    private String domainId;

    @SerializedName("accountid") @Param(description="the Account ID of the host")
    private String accountId;

    @SerializedName("affinitygroupid")
    @Param(description = "the Dedication Affinity Group ID of the host")
    private String affinityGroupId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
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
