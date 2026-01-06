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

package org.apache.cloudstack.mom.webhook.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.mom.webhook.Webhook;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {Webhook.class})
public class WebhookResponse extends BaseResponse implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Webhook")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the Webhook")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the Webhook")
    private String description;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the Webhook")
    private String state;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The ID of the domain in which the Webhook exists")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The name of the domain in which the Webhook exists")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the Webhook belongs")
    private String domainPath;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The account associated with the Webhook")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the Kubernetes cluster")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the Kubernetes cluster")
    private String projectName;

    @SerializedName(ApiConstants.PAYLOAD_URL)
    @Param(description = "The payload URL end point for the Webhook")
    private String payloadUrl;

    @SerializedName(ApiConstants.SECRET_KEY)
    @Param(description = "The secret key for the Webhook")
    private String secretKey;

    @SerializedName(ApiConstants.SSL_VERIFICATION)
    @Param(description = "Whether SSL verification is enabled for the Webhook")
    private boolean sslVerification;

    @SerializedName(ApiConstants.SCOPE)
    @Param(description = "The scope of the Webhook")
    private String scope;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date when this Webhook was created")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public void setPayloadUrl(String payloadUrl) {
        this.payloadUrl = payloadUrl;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setSslVerification(boolean sslVerification) {
        this.sslVerification = sslVerification;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
