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
import org.apache.cloudstack.dns.DnsServer;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DnsServer.class)
public class DnsServerResponse extends BaseResponse  {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the DNS server")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the DNS server")
    private String name;

    @SerializedName(ApiConstants.URL)
    @Param(description = "the URL of the DNS server API")
    private String url;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "the provider type of the DNS server")
    private String provider;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the DNS server")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the DNS server")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the DNS server")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the DNS server")
    private String domainName;

    public DnsServerResponse() {
        super();
        setObjectName("dnsserver");
    }
}
