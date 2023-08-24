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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.Parameter;

import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.serializer.Param;
import com.cloud.utils.Pair;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = AutoScaleVmProfile.class)
public class AutoScaleVmProfileResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the autoscale vm profile ID")
    private String id;

    /* Parameters related to deploy virtual machine */
    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the availability zone to be used while deploying a virtual machine")
    private String zoneId;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "the service offering to be used while deploying a virtual machine")
    private String serviceOfferingId;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "the template to be used while deploying a virtual machine")
    private String templateId;

    @SerializedName(ApiConstants.OTHER_DEPLOY_PARAMS)
    @Param(description = "parameters other than zoneId/serviceOfferringId/templateId to be used while deploying a virtual machine")
    private Map<String, String> otherDeployParams;

    /* Parameters related to destroying a virtual machine */
    @SerializedName(ApiConstants.AUTOSCALE_EXPUNGE_VM_GRACE_PERIOD)
    @Param(description = "the time allowed for existing connections to get closed before a vm is destroyed")
    private Integer expungeVmGracePeriod;

    /* Parameters related to a running virtual machine - monitoring aspects */
    @SerializedName(ApiConstants.COUNTERPARAM_LIST)
    @Parameter(name = ApiConstants.COUNTERPARAM_LIST,
               type = CommandType.MAP,
               description = "counterparam list. Example: counterparam[0].name=snmpcommunity&counterparam[0].value=public&counterparam[1].name=snmpport&counterparam[1].value=161")
    private Map<String, String> counterParams;

    @SerializedName(ApiConstants.USER_DATA)
    @Param(description = "Base 64 encoded VM user data")
    private String userData;

    @SerializedName(ApiConstants.AUTOSCALE_USER_ID)
    @Param(description = "the ID of the user used to launch and destroy the VMs")
    private String autoscaleUserId;

    @Parameter(name = ApiConstants.CS_URL,
               type = CommandType.STRING,
               description = "the API URL including port of the CloudStack Management Server example: http://server.cloud.com:8080/client/api?")
    // leaving cloud.com reference above as it serves only as an example
    private String csUrl;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account owning the instance group")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id vm profile")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vm profile")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the vm profile")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the vm profile")
    private String domainName;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is profile for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    public AutoScaleVmProfileResponse() {
        // Empty constructor
    }

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setOtherDeployParams(List<Pair<String, String>> otherDeployParams) {
        this.otherDeployParams = new HashMap<>();
        for (Pair<String, String> paramKV : otherDeployParams) {
            String key = paramKV.first();
            String value = paramKV.second();
            this.otherDeployParams.put(key, value);
        }
    }

    public void setCounterParams(List<Pair<String, String>> counterParams) {
        this.counterParams = new HashMap<>();
        for (Pair<String, String> paramKV : counterParams) {
            String key = paramKV.first();
            String value = paramKV.second();
            this.counterParams.put(key, value);
        }
    }

    public void setUserData(String userData) {
        this.userData = userData;
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
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setAutoscaleUserId(String autoscaleUserId) {
        this.autoscaleUserId = autoscaleUserId;
    }

    public void setExpungeVmGracePeriod(Integer expungeVmGracePeriod) {
        this.expungeVmGracePeriod = expungeVmGracePeriod;
    }

    public void setCsUrl(String csUrl) {
        this.csUrl = csUrl;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
