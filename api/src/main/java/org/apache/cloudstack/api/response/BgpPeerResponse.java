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

import java.util.Date;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.network.BgpPeer;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = BgpPeer.class)
public class BgpPeerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of the bgp peer")
    private String id;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "IPv4 address of bgp peer")
    private String ip4Address;

    @SerializedName(ApiConstants.IP6_ADDRESS)
    @Param(description = "IPv6 address of bgp peer")
    private String ip6Address;

    @SerializedName(ApiConstants.AS_NUMBER)
    @Param(description = "AS number of bgp peer")
    private Long asNumber;

    @SerializedName(ApiConstants.PASSWORD)
    @Param(description = "password of bgp peer")
    private String password;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "id of zone to which the bgp peer belongs to." )
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "name of zone to which the bgp peer belongs to." )
    private String zoneName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "date when this bgp peer was created." )
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the bgp peer")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the bgp peer")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the bgp peer")
    private String domainName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the bgp peer")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the bgp peer")
    private String projectName;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "additional key/value details of the bgp peer")
    private Map details;

    public void setId(String id) {
        this.id = id;
    }

    public void setIp4Address(String ip4Address) {
        this.ip4Address = ip4Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public void setAsNumber(Long asNumber) {
        this.asNumber = asNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public Long getAsNumber() {
        return asNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public Date getCreated() {
        return created;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Map getDetails() {
        return details;
    }
}
