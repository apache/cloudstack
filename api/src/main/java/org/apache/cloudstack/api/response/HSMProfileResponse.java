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
import org.apache.cloudstack.kms.HSMProfile;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = HSMProfile.class)
public class HSMProfileResponse extends BaseResponse implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the HSM profile")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the HSM profile")
    private String name;

    @SerializedName(ApiConstants.PROTOCOL)
    @Param(description = "the protocol of the HSM profile")
    private String protocol;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account ID of the HSM profile owner")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name of the HSM profile owner")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the HSM profile owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the HSM profile owner")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "the domain path of the HSM profile owner")
    private String domainPath;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project ID of the HSM profile owner")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the HSM profile owner")
    private String projectName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID where the HSM profile is available")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the zone name where the HSM profile is available")
    private String zoneName;

    @SerializedName("vendor")
    @Param(description = "the vendor name of the HSM profile")
    private String vendorName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the HSM profile")
    private String state;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "whether the HSM profile is enabled")
    private Boolean enabled;

    @SerializedName("system")
    @Param(description = "whether this is a system HSM profile available to all users globally")
    private Boolean system;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the HSM profile was created")
    private Date created;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "HSM configuration details (sensitive values are encrypted)")
    private Map<String, String> details;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
