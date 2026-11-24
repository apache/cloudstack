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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DnsZoneNetworkMapResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the mapping")
    private String id;

    @SerializedName(ApiConstants.DNS_ZONE_ID)
    @Param(description = "The ID of the DNS zone")
    private String dnsZoneId;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "The ID of the Network")
    private String networkId;

    @SerializedName("subdomain")
    @Param(description = "The sub domain name of the auto-registered DNS record")
    private String subDomain;

    public DnsZoneNetworkMapResponse() {
        super();
        setObjectName("dnszonenetwork");
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setDnsZoneId(String dnsZoneId) {
        this.dnsZoneId = dnsZoneId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }
}
