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
package com.cloud.stack.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CloudStackSecurityGroup {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.ACCOUNT)
    private String accountName;
    @SerializedName(ApiConstants.DESCRIPTION)
    private String description;
    @SerializedName(ApiConstants.DOMAIN)
    private String domainName;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.JOB_ID)
    private String jobId;
    @SerializedName(ApiConstants.JOB_STATUS)
    private Integer jobStatus;
    @SerializedName(ApiConstants.NAME)
    private String name;
    @SerializedName(ApiConstants.INGRESS_RULE)
    private List<CloudStackIngressRule> ingressRules;
    @SerializedName(ApiConstants.TAGS)
    private List<CloudStackKeyValue> tags;

    public CloudStackSecurityGroup() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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

    public String getJobId() {
        return jobId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public List<CloudStackIngressRule> getIngressRules() {
        return ingressRules;
    }

    public List<CloudStackKeyValue> getTags() {
        return tags;
    }

}
