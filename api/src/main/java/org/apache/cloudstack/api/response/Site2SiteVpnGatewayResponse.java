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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Site2SiteVpnGateway.class)
@SuppressWarnings("unused")
public class Site2SiteVpnGatewayResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the vpn gateway ID")
    private String id;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "the public IP address")
    private String ip;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "the vpc id of this gateway")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "the vpc name of this gateway", since = "4.13.2")
    private String vpcName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the owner")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the owner")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "the domain path of the owner", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the date and time the host was removed")
    private Date removed;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is vpn gateway for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    public void setId(String id) {
        this.id = id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domain = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }
    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
