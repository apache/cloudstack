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
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DnsZone.class)
public class DnsZoneResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the DNS zone")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the DNS zone")
    private String name;

    @SerializedName("dnsserverid")
    @Param(description = "ID of the DNS server this zone belongs to")
    private String dnsServerId;

    @SerializedName("dnsservername")
    @Param(description = "the name of the DNS server hosting this zone")
    private String dnsServerName;

    @SerializedName("dnsserveraccount")
    @Param(description = "the account name of the DNS server owner")
    private String dnsServerAccountName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the DNS zone")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain associated with the DNS zone")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the DNS server")
    private String domainId;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "ID of the network this zone is associated with")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "Name of the network this zone is associated with")
    private String networkName;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The type of the zone (Public/Private)")
    private DnsZone.ZoneType type;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the zone (Active/Inactive)")
    private DnsZone.State state;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description for the DNS zone")
    private String description;

    public DnsZoneResponse() {
        super();
        setObjectName("dnszone");
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDnsServerId(String dnsServerId) {
        this.dnsServerId = dnsServerId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setType(DnsZone.ZoneType type) {
        this.type = type;
    }

    public void setState(DnsZone.State state) {
        this.state = state;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDnsServerName(String dnsServerName) {
        this.dnsServerName = dnsServerName;
    }

    public void setDnsServerAccountName(String dnsServerAccountName) {
        this.dnsServerAccountName = dnsServerAccountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

}
