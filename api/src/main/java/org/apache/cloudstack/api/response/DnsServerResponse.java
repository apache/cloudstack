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
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.dns.DnsServer;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DnsServer.class)
public class DnsServerResponse extends BaseResponse  {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the DNS server")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the DNS server")
    private String name;

    @SerializedName(ApiConstants.URL)
    @Param(description = "URL of the DNS server API")
    private String url;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "The port of the DNS server")
    private Integer port;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "The provider type of the DNS server")
    private String provider;

    @SerializedName(ApiConstants.IS_PUBLIC)
    @Param(description = "Is the DNS server publicly available")
    private Boolean isPublic;

    @SerializedName(ApiConstants.PUBLIC_DOMAIN_SUFFIX)
    @Param(description = "The public domain suffix for the DNS server")
    private String publicDomainSuffix;

    @SerializedName(ApiConstants.NAME_SERVERS)
    @Param(description = "Name servers entries associated to DNS server")
    private List<String> nameServers;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the DNS server")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the DNS server")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain associated with the DNS server")
    private String domainName;

    public DnsServerResponse() {
        super();

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setPublic(Boolean value) {
        isPublic = value;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setPublicDomainSuffix(String publicDomainSuffix) {
        this.publicDomainSuffix = publicDomainSuffix;
    }

    public void setNameServers(List<String> nameServers) {
        this.nameServers = nameServers;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
