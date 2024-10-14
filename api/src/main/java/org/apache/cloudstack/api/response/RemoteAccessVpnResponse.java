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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.RemoteAccessVpn;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = RemoteAccessVpn.class)
@SuppressWarnings("unused")
public class RemoteAccessVpnResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "the public ip address of the vpn server")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "the public ip address of the vpn server")
    private String publicIp;

    @SerializedName("iprange")
    @Param(description = "the range of ips to allocate to the clients")
    private String ipRange;

    @SerializedName("presharedkey")
    @Param(description = "the ipsec preshared key", isSensitive = true)
    private String presharedKey;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the remote access vpn")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the vpn")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vpn")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the account of the remote access vpn")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the account of the remote access vpn")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the remote access vpn belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the rule")
    private String state;

    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the remote access vpn")
    private String id;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is vpn for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    public void setPresharedKey(String presharedKey) {
        this.presharedKey = presharedKey;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String name) {
        this.domainName = name;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }
    public void setState(String state) {
        this.state = state;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
